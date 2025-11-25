package com.example.expm.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expm.R
import com.example.expm.data.AppDatabase
import com.example.expm.data.Utility
import com.example.expm.network.utils.TokenManager
import com.example.expm.utils.BiometricHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize biometric helper and token manager
        biometricHelper = BiometricHelper(this)
        setContentView(R.layout.activity_splash)

        // Initialize default date range if not present
        initializeDateRangeIfNeeded()

        // Check authentication status and navigate accordingly
        Handler(Looper.getMainLooper()).postDelayed({
            val tokenManager = TokenManager.getInstance(this)

            if (tokenManager.isLoggedIn()) {
                // User is logged in, check if biometric authentication is needed
                if (shouldShowBiometricPrompt(tokenManager)) {
                    showBiometricOrPinAuthentication(tokenManager)
                } else {
                    // No biometric needed, go directly to MainActivity
                    navigateToMainActivity()
                }
            } else {
                // User is not logged in, go to LoginActivity
                navigateToLoginActivity()
            }
        }, 1500)
    }

    /**
     * Initialize default date range if start_date or end_date is missing
     * Sets start_date to 30 days back and end_date to today
     */
    private fun initializeDateRangeIfNeeded() {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getInstance(applicationContext)

                withContext(Dispatchers.IO) {
                    val startDateUtility = database.utilityDao().getByKey("start_date")
                    val endDateUtility = database.utilityDao().getByKey("end_date")

                    val currentTime = System.currentTimeMillis()

                    // Calculate default dates
                    val calendar = Calendar.getInstance()
                    val endDate = calendar.timeInMillis // Today

                    calendar.add(Calendar.DAY_OF_MONTH, -30) // 30 days back
                    val startDate = calendar.timeInMillis

                    // Insert start_date if missing
                    if (startDateUtility == null) {
                        database.utilityDao().insert(
                            Utility(
                                data_key = "start_date",
                                data_value = startDate.toString(),
                                created_on = currentTime,
                                updated_on = currentTime,
                                isActive = true
                            )
                        )
                    }

                    // Insert end_date if missing
                    if (endDateUtility == null) {
                        database.utilityDao().insert(
                            Utility(
                                data_key = "end_date",
                                data_value = endDate.toString(),
                                created_on = currentTime,
                                updated_on = currentTime,
                                isActive = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Log error but don't block app launch
                e.printStackTrace()
            }
        }
    }

    /**
     * Check if biometric prompt should be shown
     */
    private fun shouldShowBiometricPrompt(tokenManager: TokenManager): Boolean {
        return biometricHelper.isBiometricOrDeviceCredentialAvailable() &&
                biometricHelper.isBiometricEnabled() //&&
                //!tokenManager.isBiometricAuthenticatedForSession()
    }

    /**
     * Show biometric or PIN authentication prompt
     */
    private fun showBiometricOrPinAuthentication(tokenManager: TokenManager) {
        biometricHelper.showBiometricOrDeviceCredentialPrompt(
            onSuccess = {
                // Authentication successful
                tokenManager.setBiometricAuthenticatedForSession(true)
                navigateToMainActivity()
            },
            onError = { _ ->
                // Authentication error (e.g., user cancelled)
                finish()
            },
            onFailed = {
                // Authentication failed but user can try again
                // Keep the splash screen and let them retry
            }
        )
    }

    /**
     * Navigate to MainActivity
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Navigate to LoginActivity
     */
    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


}
