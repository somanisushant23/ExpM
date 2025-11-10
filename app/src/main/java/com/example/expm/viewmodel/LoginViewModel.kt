package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.expm.network.Resource
import com.example.expm.network.RetrofitClient
import com.example.expm.network.models.LoginResponse
import com.example.expm.network.repository.ApiRepository
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize API service and repository
    private val apiService = RetrofitClient.getApiService()
    private val repository = ApiRepository(apiService)

    // LiveData for login state
    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            val result = repository.login(email, password)
            _loginState.value = result
        }
    }
}
