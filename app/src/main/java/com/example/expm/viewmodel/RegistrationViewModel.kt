package com.example.expm.viewmodel

 import android.app.Application
import androidx.lifecycle.AndroidViewModel

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    var name: String = ""
    var email: String = ""
    var password: String = ""

    // TODO: Add validation and business logic for registration
}
