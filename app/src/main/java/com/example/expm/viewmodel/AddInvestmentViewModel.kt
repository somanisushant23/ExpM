package com.example.expm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.expm.network.Resource
import com.example.expm.network.RetrofitClient
import com.example.expm.network.models.InvestmentRequest
import com.example.expm.network.models.InvestmentResponse
import com.example.expm.network.models.InvestmentType
import com.example.expm.network.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddInvestmentViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService()
    private val tokenManager = TokenManager.getInstance(application)

    // LiveData for investment post state
    private val _postInvestmentState = MutableLiveData<Resource<List<InvestmentResponse>>?>()
    val postInvestmentState: LiveData<Resource<List<InvestmentResponse>>?> = _postInvestmentState

    /**
     * Post investment to server
     */
    fun postInvestment(
        amount: Int,
        expectedReturnRate: Float,
        investmentType: InvestmentType,
        creationDate: String,
        maturityDate: String,
        transactionDate: String,
        description: String? = null,
        clientId: String? = null
    ) {
        viewModelScope.launch {
            _postInvestmentState.value = Resource.Loading()

            try {
                val token = tokenManager.getAuthHeader()
                val email = tokenManager.getUserEmail()

                if (token.isNullOrEmpty() || email.isNullOrEmpty()) {
                    _postInvestmentState.value = Resource.Error("Authentication required")
                    return@launch
                }

                val investmentRequest = InvestmentRequest(
                    amount = amount,
                    investmentType = investmentType,
                    expectedReturnRate = expectedReturnRate,
                    creationDate = creationDate,
                    maturityDate = maturityDate,
                    transactionType = "Investment",
                    transactionDate = transactionDate,
                    description = description,
                    createdOn = System.currentTimeMillis(),
                    updatedOn = System.currentTimeMillis(),
                    clientId = clientId
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.postInvestments(
                        token = token,
                        email = email,
                        investments = listOf(investmentRequest)
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    _postInvestmentState.value = Resource.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to post investment"
                    _postInvestmentState.value = Resource.Error(errorMsg)
                    Log.e("AddInvestmentVM", "Error: $errorMsg")
                }
            } catch (e: Exception) {
                _postInvestmentState.value = Resource.Error(e.message ?: "Unknown error occurred")
                Log.e("AddInvestmentVM", "Exception: ${e.message}", e)
            }
        }
    }

    /**
     * Clear the post investment state
     */
    fun clearState() {
        _postInvestmentState.value = null
    }
}

