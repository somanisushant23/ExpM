package com.example.expm.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.expm.R
import com.example.expm.network.utils.TokenManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check authentication status and navigate accordingly
        Handler(Looper.getMainLooper()).postDelayed({
            val tokenManager = TokenManager.getInstance(this)

            val intent = if (tokenManager.isLoggedIn()) {
                // User is logged in, go to MainActivity
                Intent(this, MainActivity::class.java)
            } else {
                // User is not logged in, go to LoginActivity
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, 1500)
    }
}
