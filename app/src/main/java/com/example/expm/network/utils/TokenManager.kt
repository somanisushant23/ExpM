package com.example.expm.network.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to manage authentication tokens
 * Uses SharedPreferences to store and retrieve tokens
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "ExpMAuthPrefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_BIOMETRIC_AUTHENTICATED = "biometric_authenticated_session"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Save authentication token
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Get saved authentication token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    /**
     * Save user information
     */
    fun saveUserInfo(name: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    /**
     * Get user name
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Clear all saved data (logout)
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    /**
     * Get authorization header value
     */
    fun getAuthHeader(): String? {
        val token = getToken()
        return if (token != null) "$token" else null
    }

    /**
     * Mark that biometric authentication was successful in this session
     */
    fun setBiometricAuthenticatedForSession(authenticated: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_AUTHENTICATED, authenticated).apply()
    }

    /**
     * Check if biometric authentication was already done in this session
     */
    fun isBiometricAuthenticatedForSession(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_AUTHENTICATED, false)
    }

    /**
     * Clear biometric authentication session (call when app goes to background)
     */
    fun clearBiometricSession() {
        prefs.edit().putBoolean(KEY_BIOMETRIC_AUTHENTICATED, false).apply()
    }
}

