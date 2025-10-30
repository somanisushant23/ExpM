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

class AppStartupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AppStartupWorker"
        private const val OUTPUT_FILE_NAME = "entries_export.json"
        private const val APP_FOLDER = "ExpM"
    }

    override suspend fun doWork(): WorkResult {
        Log.i(TAG, "AppStartupWorker running at app launch")

        return try {
            // Ensure external storage is available for write
            val state = Environment.getExternalStorageState()
            if (state != Environment.MEDIA_MOUNTED) {
                Log.e(TAG, "External storage not mounted: $state")
                return WorkResult.failure()
            }

            // Obtain unsynced entries from the Room database
            val db = AppDatabase.getInstance(applicationContext)
            val entries = db.entryDao().getUnsyncEntries()

            // Serialize to JSON using Gson
            val gson = Gson()
            val type = object : TypeToken<List<Entry>>() {}.type
            val json = gson.toJson(entries, type)

            // Write to public external Documents directory under an app folder (may require runtime permission)
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(publicDocs, APP_FOLDER)
            if (!appDir.exists()) {
                val created = appDir.mkdirs()
                if (!created) {
                    Log.e(TAG, "Failed to create external app directory: ${appDir.absolutePath}")
                    return WorkResult.failure()
                }
            }

            val file = File(appDir, OUTPUT_FILE_NAME)
            file.writeText(json)

            Log.i(TAG, "Wrote ${entries.size} entries to ${file.absolutePath}")

            // Mark entries as persisted after successful export
            try {
                entries.forEach { entry ->
                    val persisted = entry.copy(isPersisted = true)
                    db.entryDao().update(persisted)
                }
                Log.i(TAG, "Marked ${entries.size} entries as persisted")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to mark entries as persisted: ${t.message}", t)
                // Not failing the whole worker because export succeeded; but return failure
                return WorkResult.failure()
            }

            WorkResult.success()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to export entries: ${t.message}", t)
            WorkResult.failure()
        }
    }
}
