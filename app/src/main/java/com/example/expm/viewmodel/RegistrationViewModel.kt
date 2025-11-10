package com.example.expm.viewmodel

 import android.app.Application
import androidx.lifecycle.AndroidViewModel
 import androidx.lifecycle.viewModelScope
 import com.example.expm.network.Resource
 import com.example.expm.network.RetrofitClient
 import com.example.expm.network.repository.ApiRepository
 import androidx.lifecycle.LiveData
 import androidx.lifecycle.MutableLiveData
 import com.example.expm.network.models.RegisterResponse
 import kotlinx.coroutines.launch

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize API service and repository
    private val apiService = RetrofitClient.getApiService()
    private val repository = ApiRepository(apiService)

    private val _registrationState = MutableLiveData<Resource<RegisterResponse>>()
    val registrationState: LiveData<Resource<RegisterResponse>> = _registrationState

    /**
     * Register new user
     */
    fun register(name: String, email: String, password: String) {
         viewModelScope.launch {
             _registrationState.value = Resource.Loading()
             val result = repository.register(name, email, password)
             _registrationState.value = result
         }
     }
}
