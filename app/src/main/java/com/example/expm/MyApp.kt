package com.example.expm

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.expm.worker.AppStartupWorker
import java.util.concurrent.TimeUnit

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Force light mode regardless of system theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Schedule a unique one-time work to run at app startup.
        try {
            // Create constraints requiring WiFi connection
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<AppStartupWorker>()
                .setConstraints(constraints)
                // Optional: configure a backoff in case of retry
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "app_startup_work",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Log.i("MyApp", "Enqueued app_startup_work with WiFi constraint")
        } catch (t: Throwable) {
            // Protect against WorkManager not being initialized or other errors at cold start
            Log.w("MyApp", "Failed to enqueue startup work: ${t.message}")
        }
    }
}

