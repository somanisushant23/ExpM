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

    // LiveData for investment delete state
    private val _deleteInvestmentState = MutableLiveData<Resource<Unit>?>()
    val deleteInvestmentState: LiveData<Resource<Unit>?> = _deleteInvestmentState

    // LiveData for investment update state
    private val _updateInvestmentState = MutableLiveData<Resource<InvestmentResponse>?>()
    val updateInvestmentState: LiveData<Resource<InvestmentResponse>?> = _updateInvestmentState

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

    /**
     * Delete investment from server
     */
    fun deleteInvestment(remoteId: Long) {
        viewModelScope.launch {
            _deleteInvestmentState.value = Resource.Loading()

            try {
                val token = tokenManager.getAuthHeader()
                val email = tokenManager.getUserEmail()

                if (token.isNullOrEmpty() || email.isNullOrEmpty()) {
                    _deleteInvestmentState.value = Resource.Error("Authentication required")
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    apiService.deleteInvestment(
                        token = token,
                        email = email,
                        remoteId = remoteId
                    )
                }

                if (response.isSuccessful) {
                    _deleteInvestmentState.value = Resource.Success(Unit)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to delete investment"
                    _deleteInvestmentState.value = Resource.Error(errorMsg)
                    Log.e("AddInvestmentVM", "Delete Error: $errorMsg")
                }
            } catch (e: Exception) {
                _deleteInvestmentState.value = Resource.Error(e.message ?: "Unknown error occurred")
                Log.e("AddInvestmentVM", "Delete Exception: ${e.message}", e)
            }
        }
    }

    /**
     * Update investment on server
     */
    fun updateInvestment(
        remoteId: Long,
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
            _updateInvestmentState.value = Resource.Loading()

            try {
                val token = tokenManager.getAuthHeader()
                val email = tokenManager.getUserEmail()

                if (token.isNullOrEmpty() || email.isNullOrEmpty()) {
                    _updateInvestmentState.value = Resource.Error("Authentication required")
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
                    apiService.updateInvestment(
                        token = token,
                        email = email,
                        investments = investmentRequest
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    _updateInvestmentState.value = Resource.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to update investment"
                    _updateInvestmentState.value = Resource.Error(errorMsg)
                    Log.e("AddInvestmentVM", "Update Error: $errorMsg")
                }
            } catch (e: Exception) {
                _updateInvestmentState.value = Resource.Error(e.message ?: "Unknown error occurred")
                Log.e("AddInvestmentVM", "Update Exception: ${e.message}", e)
            }
        }
    }
}

