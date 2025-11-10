package com.example.expm.network.repository

import com.example.expm.network.ApiService
import com.example.expm.network.Resource
import com.example.expm.network.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository to handle API calls
 */
class ApiRepository(private val apiService: ApiService) {

    // ==================== Helper Functions ====================

    /**
     * Generic function to handle API responses
     */
    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    response.body()?.let {
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response body")
                } else {
                    Resource.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // ==================== Authentication ====================

    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        return safeApiCall {
            apiService.login(LoginRequest(email, password))
        }
    }

    suspend fun register(name: String, email: String, password: String): Resource<RegisterResponse> {
        return safeApiCall {
            apiService.register(RegisterRequest(name, email, password))
        }
    }

    suspend fun logout(token: String): Resource<GenericResponse> {
        return safeApiCall {
            apiService.logout("$token")
        }
    }

    suspend fun getProfile(token: String): Resource<UserProfile> {
        return safeApiCall {
            apiService.getProfile("$token")
        }
    }

    suspend fun updateProfile(
        token: String,
        name: String? = null,
        email: String? = null,
        password: String? = null
    ): Resource<UserProfile> {
        return safeApiCall {
            apiService.updateProfile(
                "$token",
                UpdateProfileRequest(name, email, password)
            )
        }
    }

    // ==================== Entries ====================

    suspend fun getEntries(
        token: String,
        page: Int = 1,
        limit: Int = 50
    ): Resource<EntriesResponse> {
        return safeApiCall {
            apiService.getEntries("$token", page, limit)
        }
    }

    suspend fun getEntry(token: String, entryId: Long): Resource<EntryResponse> {
        return safeApiCall {
            apiService.getEntry("$token", entryId)
        }
    }

    suspend fun createEntry(token: String, entry: EntryRequest): Resource<EntryResponse> {
        return safeApiCall {
            apiService.createEntry("$token", entry)
        }
    }

    suspend fun updateEntry(
        token: String,
        entryId: Long,
        entry: EntryRequest
    ): Resource<EntryResponse> {
        return safeApiCall {
            apiService.updateEntry("$token", entryId, entry)
        }
    }

    suspend fun deleteEntry(token: String, entryId: Long): Resource<GenericResponse> {
        return safeApiCall {
            apiService.deleteEntry("$token", entryId)
        }
    }

    suspend fun syncEntries(token: String, entries: List<EntryRequest>): Resource<SyncResponse> {
        return safeApiCall {
            apiService.syncEntries("$token", entries)
        }
    }

    // ==================== Categories ====================

    suspend fun getCategories(token: String): Resource<CategoriesResponse> {
        return safeApiCall {
            apiService.getCategories("$token")
        }
    }

    // ==================== Analytics ====================

    suspend fun getAnalyticsSummary(
        token: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): Resource<AnalyticsSummaryResponse> {
        return safeApiCall {
            apiService.getAnalyticsSummary("$token", startDate, endDate)
        }
    }
}

