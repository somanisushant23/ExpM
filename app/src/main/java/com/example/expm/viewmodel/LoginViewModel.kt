package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var email: String = ""
    var password: String = ""

    // TODO: Add validation and business logic for login
}
