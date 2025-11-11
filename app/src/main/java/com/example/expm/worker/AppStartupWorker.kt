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
            return WorkResult.failure()
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
                    val allLocalEntries = entryDao.getAll()
                    var insertedCount = 0
                    var updatedCount = 0
                    var skippedCount = 0

                    // Save each transaction as an Entry with remoteId, handling conflicts
                    transactions.forEach { transaction ->
                        // Check if an entry with the same createdOn timestamp exists locally
                        val existingEntry = allLocalEntries.find { it.created_on == transaction.createdOn }

                        if (existingEntry != null) {
                            // Entry with same createdOn exists - compare updatedOn timestamps
                            when {
                                transaction.updatedOn > existingEntry.updated_on -> {
                                    // Server has more recent data, update local entry
                                    val updatedEntry = existingEntry.copy(
                                        title = transaction.title,
                                        amount = transaction.amount.toDouble(),
                                        type = transaction.transactionType.lowercase(Locale.getDefault()),
                                        category = transaction.category,
                                        updated_on = transaction.updatedOn,
                                        notes = transaction.description ?: "",
                                        isPersisted = true,
                                        isUpdated = false,
                                        remoteId = transaction.id
                                    )
                                    entryDao.update(updatedEntry)
                                    updatedCount++
                                    Log.d(TAG, "Updated local entry (createdOn: ${transaction.createdOn}) with server data (server updatedOn: ${transaction.updatedOn} > local: ${existingEntry.updated_on})")
                                }
                                transaction.updatedOn < existingEntry.updated_on -> {
                                    // Local data is more recent, keep it and mark as updated to sync back
                                    val needsRemoteIdUpdate = existingEntry.remoteId != transaction.id
                                    if (!existingEntry.isUpdated || needsRemoteIdUpdate) {
                                        entryDao.update(existingEntry.copy(
                                            isUpdated = true,
                                            remoteId = transaction.id // Ensure remoteId is set
                                        ))
                                        Log.d(TAG, "Local entry (createdOn: ${transaction.createdOn}) is more recent (local updatedOn: ${existingEntry.updated_on} > server: ${transaction.updatedOn}), marking for sync")
                                    }
                                    skippedCount++
                                }
                                else -> {
                                    // Same timestamp, but ensure remoteId is set
                                    if (existingEntry.remoteId != transaction.id) {
                                        entryDao.update(existingEntry.copy(
                                            remoteId = transaction.id,
                                            isPersisted = true
                                        ))
                                        Log.d(TAG, "Updated remoteId for entry (createdOn: ${transaction.createdOn})")
                                    }
                                    skippedCount++
                                    Log.d(TAG, "Entry (createdOn: ${transaction.createdOn}) has identical timestamp, skipping")
                                }
                            }
                        } else {
                            // New entry from server, insert it
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
                            insertedCount++
                        }
                    }

                    Log.i(TAG, "Sync complete - Inserted: $insertedCount, Updated: $updatedCount, Skipped: $skippedCount")

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
                    description = if (entry.notes.isBlank()) null else entry.notes,
                    createdOn = entry.created_on
                )

                val updateResponse = apiService.updateTransaction(token, email, entry.remoteId, transactionRequest)
                if (updateResponse.isSuccessful) {
                    // Update local entry with current timestamp and mark as no longer updated
                    val currentTime = System.currentTimeMillis()
                    val syncedEntry = entry.copy(
                        updated_on = currentTime,
                        isUpdated = false
                    )
                    entryDao.update(syncedEntry)
                    Log.i(TAG, "Successfully updated transaction with remoteId: ${entry.remoteId}, local updatedOn: $currentTime")
                } else if (updateResponse.code() == 409) {
                    // Conflict detected - server has a more recent version
                    Log.w(TAG, "Conflict detected for transaction ${entry.remoteId}. Server has more recent data. Will be resolved in next fetch.")
                    // Mark entry to be refreshed from server on next sync
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
                description = if (entry.notes.isBlank()) null else entry.notes,
                createdOn = entry.created_on
            )
        }

        try {
            val response = apiService.postTransactions(token, email, transactionRequests)

            if (response.isSuccessful && response.body() != null) {
                val transactionResponses = response.body()!!
                Log.i(TAG, "Successfully posted ${transactionResponses.size} transactions")

                nonPersistedEntries.forEachIndexed { index, entry ->
                    val serverTransaction = transactionResponses[index]
                    // Update local entry with server data including timestamps
                    val syncedEntry = entry.copy(
                        isPersisted = true,
                        remoteId = serverTransaction.id,
                        created_on = serverTransaction.createdOn,
                        updated_on = serverTransaction.updatedOn
                    )
                    entryDao.update(syncedEntry)
                    Log.d(TAG, "Entry ${entry.id} synced with remoteId: ${serverTransaction.id}, createdOn: ${serverTransaction.createdOn}, updatedOn: ${serverTransaction.updatedOn}")
                }

                Log.i(TAG, "Marked ${nonPersistedEntries.size} entries as persisted with remote IDs and server timestamps")
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
