package com.example.expm.worker

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import android.util.Log
import com.example.expm.data.AppDatabase
import com.example.expm.data.Entry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Worker that imports Entry objects from the app's exported JSON file into the local Room database.
 *
 * Behavior:
 * - Looks for Documents/ExpM/entries_export.json on external storage.
 * - Parses the file into a List<Entry> using Gson.
 * - Inserts each entry into the database. To avoid primary-key collisions with local rows,
 *   we reset the id to 0 so Room will auto-generate a new id.
 * - Inserting entries will update the app's Flow/LiveData and therefore the RecyclerView.
 */
class JsonImportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "JsonImportWorker"
        private const val INPUT_FILE_NAME = "entries_export.json"
        private const val APP_FOLDER = "ExpM"
    }

    override suspend fun doWork(): WorkResult {
        Log.i(TAG, "JsonImportWorker started")

        return try {
            // Ensure external storage is at least readable
            val state = Environment.getExternalStorageState()
            if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                Log.e(TAG, "External storage not available: $state")
                return WorkResult.failure()
            }

            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(publicDocs, APP_FOLDER)
            val file = File(appDir, INPUT_FILE_NAME)

            if (!file.exists()) {
                Log.e(TAG, "Import file does not exist: ${file.absolutePath}")
                return WorkResult.failure()
            }

            val json = file.readText()
            if (json.isBlank()) {
                Log.e(TAG, "Import file is empty: ${file.absolutePath}")
                return WorkResult.failure()
            }

            val gson = Gson()
            val type = object : TypeToken<List<Entry>>() {}.type
            val entries: List<Entry> = try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to parse JSON: ${ex.message}", ex)
                return WorkResult.failure()
            }

            if (entries.isEmpty()) {
                Log.i(TAG, "No entries found in import file")
                return WorkResult.success()
            }

            val db = AppDatabase.getInstance(applicationContext)

            // Insert entries into DB. Reset id to 0 so Room assigns new ids and we avoid collisions.
            var inserted = 0
            entries.forEach { entry ->
                try {
                    val toInsert = entry.copy(id = 0)
                    db.entryDao().insert(toInsert)
                    inserted++
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to insert entry: ${t.message}", t)
                    // continue inserting other entries
                }
            }

            Log.i(TAG, "Imported $inserted/${entries.size} entries from ${file.absolutePath}")

            WorkResult.success()
        } catch (t: Throwable) {
            Log.e(TAG, "JsonImportWorker failed: ${t.message}", t)
            WorkResult.failure()
        }
    }
}

