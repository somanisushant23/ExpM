package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.example.expm.R
import com.example.expm.network.Resource
import com.example.expm.network.utils.TokenManager
import com.example.expm.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Login"

        // Initialize ViewModel and TokenManager
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        tokenManager = TokenManager.getInstance(this)

        // Get references to UI elements
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val registerLink: TextView = findViewById(R.id.tvRegisterLink)

        // Set click listener for login button
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        // Observe loginState LiveData to handle login response
        observeLoginState(btnLogin)
    }

    private fun observeLoginState(btnLogin: Button) {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnLogin.isEnabled = false
                    btnLogin.text = "Logging in..."
                }
                is Resource.Success -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    val loginData = resource.data
                    if (loginData != null) {
                        // Save token and user info
                        tokenManager.saveToken(loginData.token)
                        tokenManager.saveUserInfo(
                            loginData.name,
                            loginData.email
                        )

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: Invalid response", Toast.LENGTH_SHORT).show()
                    }
                }
                is Resource.Error -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(this, "Login failed: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}