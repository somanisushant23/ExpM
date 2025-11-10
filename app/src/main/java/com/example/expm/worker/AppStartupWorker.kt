package com.example.expm.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import android.util.Log
import com.example.expm.data.AppDatabase
import com.example.expm.data.Utility
import com.example.expm.network.RetrofitClient
import com.example.expm.network.models.TransactionRequest
import com.example.expm.network.utils.TokenManager
import java.text.SimpleDateFormat
import java.util.*

class AppStartupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AppStartupWorker"
    }

    private lateinit var tokenManager: TokenManager
    private lateinit var database: AppDatabase
    private lateinit var apiService: com.example.expm.network.ApiService

    override suspend fun doWork(): WorkResult {
        Log.i(TAG, "AppStartupWorker running at app launch")

        try {
            // Initialize components
            if (!initializeComponents()) {
                return WorkResult.success()
            }

            // Perform sync operations
            syncDeletedEntries()
            syncUpdatedEntries()
            syncNewEntries()
            fetchNewTransactions()
            return WorkResult.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in AppStartupWorker", e)
            return WorkResult.retry()
        }
    }

    /**
     * Initialize required components and validate authentication
     * @return true if initialization successful, false otherwise
     */
    private fun initializeComponents(): Boolean {
        tokenManager = TokenManager.getInstance(applicationContext)

        if (!tokenManager.isLoggedIn()) {
            Log.w(TAG, "User not logged in, skipping sync")
            return false
        }

        val token = tokenManager.getAuthHeader()
        val email = tokenManager.getUserEmail()

        if (token == null || email == null) {
            Log.w(TAG, "Token or email not available, skipping sync")
            return false
        }

        database = AppDatabase.getInstance(applicationContext)
        apiService = RetrofitClient.getApiService()

        return true
    }

    /**
     * Fetch new transactions from the server
     * Fetches transactions updated after the stored updatedTime (or 0 if not exists)
     * Saves transactions with remoteId and updates the updatedTime utility
     */
    private suspend fun fetchNewTransactions() {
        val token = tokenManager.getAuthHeader() ?: return
        val email = tokenManager.getUserEmail() ?: return
        val utilityDao = database.utilityDao()

        // Get updatedTime from utility, default to 0 if not exists
        val updatedTimeUtility = utilityDao.getByKey("updatedTime")
        val updatedTime = updatedTimeUtility?.data_value?.toLongOrNull() ?: 0L

        Log.i(TAG, "Fetching transactions updated after: $updatedTime")

        try {
            val response = apiService.getNewTransactions(token, email, updatedTime)

            if (response.isSuccessful && response.body() != null) {
                val transactions = response.body()!!
                Log.i(TAG, "Successfully fetched ${transactions.size} transactions from server")

                if (transactions.isNotEmpty()) {
                    val entryDao = database.entryDao()

                    // Save each transaction as an Entry with remoteId
                    transactions.forEach { transaction ->
                        val entry = com.example.expm.data.Entry(
                            id = 0, // Auto-generate local ID
                            title = transaction.title,
                            amount = transaction.amount.toDouble(),
                            type = transaction.transactionType.lowercase(Locale.getDefault()),
                            category = transaction.category,
                            created_on = transaction.createdOn,
                            updated_on = transaction.updatedOn,
                            notes = transaction.description ?: "",
                            isPersisted = true,
                            isDeleted = false,
                            isUpdated = false,
                            remoteId = transaction.id
                        )
                        entryDao.insert(entry)
                    }

                    Log.i(TAG, "Saved ${transactions.size} transactions to local database")

                    // Get the maximum updatedOn timestamp from the fetched transactions
                    val maxUpdatedTime = transactions.maxOfOrNull { it.updatedOn } ?: System.currentTimeMillis()

                    // Update or create updatedTime utility
                    if (updatedTimeUtility != null) {
                        val updatedUtility = updatedTimeUtility.copy(
                            data_value = maxUpdatedTime.toString(),
                            updated_on = System.currentTimeMillis()
                        )
                        utilityDao.update(updatedUtility)
                        Log.i(TAG, "Updated updatedTime utility to: $maxUpdatedTime")
                    } else {
                        val newUtility = Utility(
                            id = 0,
                            data_key = "updatedTime",
                            data_value = maxUpdatedTime.toString(),
                            created_on = System.currentTimeMillis(),
                            updated_on = System.currentTimeMillis(),
                            isActive = true
                        )
                        utilityDao.insert(newUtility)
                        Log.i(TAG, "Created updatedTime utility with value: $maxUpdatedTime")
                    }
                } else {
                    Log.i(TAG, "No new transactions to fetch")
                }
            } else {
                Log.e(TAG, "Failed to fetch transactions: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching new transactions", e)
        }
    }

    /**
     * Sync deleted entries with the server
     * Deletes entries from server that are marked as deleted and have a remoteId
     */
    private suspend fun syncDeletedEntries() {
        val token = tokenManager.getAuthHeader() ?: return
        val email = tokenManager.getUserEmail() ?: return
        val entryDao = database.entryDao()
        val entries = entryDao.getAll()
        val deletedEntries = entries.filter { it.isDeleted && it.remoteId != 0L }

        if (deletedEntries.isEmpty()) {
            Log.i(TAG, "No deleted entries to sync")
            return
        }

        Log.i(TAG, "Found ${deletedEntries.size} deleted entries to sync with server")

        deletedEntries.forEach { entry ->
            try {
                val deleteResponse = apiService.deleteTransaction(token, email, entry.remoteId)
                if (deleteResponse.isSuccessful) {
                    Log.i(TAG, "Successfully deleted transaction with remoteId: ${entry.remoteId}")
                    entryDao.delete(entry)
                } else {
                    Log.e(TAG, "Failed to delete transaction ${entry.remoteId}: ${deleteResponse.code()} - ${deleteResponse.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting transaction ${entry.remoteId}", e)
            }
        }
    }

    /**
     * Sync updated entries with the server
     * Updates entries on server that are marked as updated and have a remoteId
     */
    private suspend fun syncUpdatedEntries() {
        val token = tokenManager.getAuthHeader() ?: return
        val email = tokenManager.getUserEmail() ?: return
        val entryDao = database.entryDao()
        val entries = entryDao.getAll()
        val updatedEntries = entries.filter { it.isUpdated && it.remoteId != 0L }

        if (updatedEntries.isEmpty()) {
            Log.i(TAG, "No updated entries to sync")
            return
        }

        Log.i(TAG, "Found ${updatedEntries.size} updated entries to sync with server")

        updatedEntries.forEach { entry ->
            try {
                val transactionRequest = TransactionRequest(
                    title = entry.title,
                    amount = entry.amount.toInt(),
                    category = entry.category,
                    transactionType = entry.type.uppercase(Locale.getDefault()),
                    transactionDate = formatDate(entry.created_on),
                    description = if (entry.notes.isBlank()) null else entry.notes
                )

                val updateResponse = apiService.updateTransaction(token, email, entry.remoteId, transactionRequest)
                if (updateResponse.isSuccessful) {
                    Log.i(TAG, "Successfully updated transaction with remoteId: ${entry.remoteId}")
                    // Mark entry as no longer updated
                    entryDao.update(entry.copy(isUpdated = false))
                } else {
                    Log.e(TAG, "Failed to update transaction ${entry.remoteId}: ${updateResponse.code()} - ${updateResponse.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating transaction ${entry.remoteId}", e)
            }
        }
    }

    /**
     * Sync new entries with the server
     * Creates entries on server that are not yet persisted
     */
    private suspend fun syncNewEntries() {
        val token = tokenManager.getAuthHeader() ?: return
        val email = tokenManager.getUserEmail() ?: return
        val entryDao = database.entryDao()
        val entries = entryDao.getAll()
        val nonPersistedEntries = entries.filter { !it.isPersisted && !it.isDeleted }

        if (nonPersistedEntries.isEmpty()) {
            Log.i(TAG, "No new entries to sync")
            return
        }

        Log.i(TAG, "Found ${nonPersistedEntries.size} entries to sync")

        val transactionRequests = nonPersistedEntries.map { entry ->
            TransactionRequest(
                title = entry.title,
                amount = entry.amount.toInt(),
                category = entry.category,
                transactionType = entry.type.uppercase(Locale.getDefault()),
                transactionDate = formatDate(entry.created_on),
                description = if (entry.notes.isBlank()) null else entry.notes
            )
        }

        try {
            val response = apiService.postTransactions(token, email, transactionRequests)

            if (response.isSuccessful && response.body() != null) {
                val transactionResponses = response.body()!!
                Log.i(TAG, "Successfully posted ${transactionResponses.size} transactions")

                nonPersistedEntries.forEachIndexed { index, entry ->
                    val remoteId = transactionResponses[index].id
                    entryDao.update(entry.copy(isPersisted = true, remoteId = remoteId))
                }

                Log.i(TAG, "Marked ${nonPersistedEntries.size} entries as persisted with remote IDs")
            } else {
                Log.e(TAG, "Failed to post transactions: ${response.code()} - ${response.message()}")
                throw Exception("Failed to post transactions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing new entries", e)
            throw e
        }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
