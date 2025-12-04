package com.example.expm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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

    // LiveData for search query
    private val _searchQuery = MutableLiveData<String>("")

    // Combined LiveData for filtered investments
    val investmentsState: LiveData<Resource<List<InvestmentResponse>>?> = MediatorLiveData<Resource<List<InvestmentResponse>>?>().apply {
        addSource(_investmentsState) { resource ->
            value = filterInvestments(resource, _searchQuery.value ?: "")
        }
        addSource(_searchQuery) { query ->
            value = filterInvestments(_investmentsState.value, query)
        }
    }

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

    /**
     * Set search query for filtering investments
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Filter investments based on search query
     */
    private fun filterInvestments(
        resource: Resource<List<InvestmentResponse>>?,
        query: String
    ): Resource<List<InvestmentResponse>>? {
        if (resource !is Resource.Success || query.isBlank()) {
            return resource
        }

        val filteredList = resource.data?.filter { investment ->
            val searchText = query.lowercase()
            investment.investmentType.name.lowercase().contains(searchText) ||
                    investment.description?.lowercase()?.contains(searchText) == true ||
                    investment.amount.contains(searchText) ||
                    investment.expectedReturnRate.toString().contains(searchText) ||
                    investment.creationDate.contains(searchText) ||
                    investment.maturityDate.contains(searchText)
        }

        return Resource.Success(filteredList ?: emptyList())
    }
}
