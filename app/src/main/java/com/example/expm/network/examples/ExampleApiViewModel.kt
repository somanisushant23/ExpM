package com.example.expm.network.examples

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expm.network.Resource
import com.example.expm.network.RetrofitClient
import com.example.expm.network.models.*
import com.example.expm.network.repository.ApiRepository
import kotlinx.coroutines.launch

/**
 * Example ViewModel demonstrating how to use the API integration
 *
 * This is a sample implementation showing best practices for:
 * - Making API calls
 * - Handling loading, success, and error states
 * - Using LiveData for UI observation
 * - Managing authentication
 */
class ExampleApiViewModel : ViewModel() {

    // Initialize API service and repository
    private val apiService = RetrofitClient.getApiService()
    private val repository = ApiRepository(apiService)

    // LiveData for login state
    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> = _loginState

    // LiveData for entries
    private val _entriesState = MutableLiveData<Resource<EntriesResponse>>()
    val entriesState: LiveData<Resource<EntriesResponse>> = _entriesState

    // LiveData for create entry
    private val _createEntryState = MutableLiveData<Resource<EntryResponse>>()
    val createEntryState: LiveData<Resource<EntryResponse>> = _createEntryState

    // LiveData for sync
    private val _syncState = MutableLiveData<Resource<SyncResponse>>()
    val syncState: LiveData<Resource<SyncResponse>> = _syncState

    // LiveData for analytics
    private val _analyticsState = MutableLiveData<Resource<AnalyticsSummaryResponse>>()
    val analyticsState: LiveData<Resource<AnalyticsSummaryResponse>> = _analyticsState

    /**
     * Get all entries from server
     */
    fun getEntries(token: String, page: Int = 1, limit: Int = 50) {
        viewModelScope.launch {
            _entriesState.value = Resource.Loading()
            val result = repository.getEntries(token, page, limit)
            _entriesState.value = result
        }
    }

    /**
     * Create a new entry on server
     */
    fun createEntry(token: String, entry: EntryRequest) {
        viewModelScope.launch {
            _createEntryState.value = Resource.Loading()
            val result = repository.createEntry(token, entry)
            _createEntryState.value = result
        }
    }

    /**
     * Update an existing entry
     */
    fun updateEntry(token: String, entryId: Long, entry: EntryRequest) {
        viewModelScope.launch {
            _createEntryState.value = Resource.Loading()
            val result = repository.updateEntry(token, entryId, entry)
            _createEntryState.value = result
        }
    }

    /**
     * Delete an entry
     */
    fun deleteEntry(token: String, entryId: Long) {
        viewModelScope.launch {
            val result = repository.deleteEntry(token, entryId)
            // You might want to create a separate LiveData for delete operations
            // For now, we'll trigger a refresh of entries on success
            if (result is Resource.Success) {
                getEntries(token)
            }
        }
    }

    /**
     * Sync local entries with server
     */
    fun syncEntries(token: String, entries: List<EntryRequest>) {
        viewModelScope.launch {
            _syncState.value = Resource.Loading()
            val result = repository.syncEntries(token, entries)
            _syncState.value = result
        }
    }

    /**
     * Get analytics summary
     */
    fun getAnalyticsSummary(token: String, startDate: Long? = null, endDate: Long? = null) {
        viewModelScope.launch {
            _analyticsState.value = Resource.Loading()
            val result = repository.getAnalyticsSummary(token, startDate, endDate)
            _analyticsState.value = result
        }
    }

    /**
     * Get categories
     */
    fun getCategories(token: String) {
        viewModelScope.launch {
            val result = repository.getCategories(token)
            // Handle result as needed
        }
    }
}

