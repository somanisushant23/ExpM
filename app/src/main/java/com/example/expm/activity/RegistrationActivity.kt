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
import com.example.expm.viewmodel.RegistrationViewModel

class RegistrationActivity : AppCompatActivity() {

    private lateinit var viewModel: RegistrationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Register"

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RegistrationViewModel::class.java]

        // Get references to UI elements
        val etName: EditText = findViewById(R.id.etName)
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnRegister: Button = findViewById(R.id.btnRegister)
        val loginLink: TextView = findViewById(R.id.tvLoginLink)

        // Set click listener for register button
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Basic validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(name, email, password)
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Observe registrationState LiveData to handle registration response
        observeRegistrationState(btnRegister)
    }

    private fun observeRegistrationState(btnRegister: Button) {
        viewModel.registrationState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnRegister.isEnabled = false
                    btnRegister.text = "Registering..."
                }
                is Resource.Success -> {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"

                    Toast.makeText(this, resource.data?.message ?: "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Navigate to Login screen
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"
                    Toast.makeText(this, "Registration failed: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}