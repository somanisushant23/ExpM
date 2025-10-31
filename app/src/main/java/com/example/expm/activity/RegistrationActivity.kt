package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.expm.R
import com.example.expm.viewmodel.RegistrationViewModel

class RegistrationActivity : AppCompatActivity() {

    private lateinit var viewModel: RegistrationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        val loginLink: TextView = findViewById(R.id.tvLoginLink)
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}