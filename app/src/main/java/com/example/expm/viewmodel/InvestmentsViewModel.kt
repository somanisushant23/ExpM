package com.example.expm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.expm.network.Resource
import com.example.expm.network.RetrofitClient
import com.example.expm.network.models.InvestmentResponse
import com.example.expm.network.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvestmentsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService()
    private val tokenManager = TokenManager.getInstance(application)

    // LiveData for get investments state
    private val _investmentsState = MutableLiveData<Resource<List<InvestmentResponse>>?>()
    val investmentsState: LiveData<Resource<List<InvestmentResponse>>?> = _investmentsState

    /**
     * Fetch investments from server
     */
    fun getInvestments() {
        viewModelScope.launch {
            _investmentsState.value = Resource.Loading()

            try {
                val token = tokenManager.getAuthHeader()
                val email = tokenManager.getUserEmail()

                if (token.isNullOrEmpty() || email.isNullOrEmpty()) {
                    _investmentsState.value = Resource.Error("Authentication required")
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    apiService.getInvestments(
                        token = token,
                        email = email
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _investmentsState.value = Resource.Success(body)
                    } else {
                        _investmentsState.value = Resource.Error("No data received")
                    }
                } else {
                    _investmentsState.value = Resource.Error(
                        response.errorBody()?.string() ?: "Failed to fetch investments"
                    )
                }
            } catch (e: Exception) {
                Log.e("InvestmentsViewModel", "Error fetching investments", e)
                _investmentsState.value = Resource.Error(
                    e.message ?: "An error occurred while fetching investments"
                )
            }
        }
    }
}



