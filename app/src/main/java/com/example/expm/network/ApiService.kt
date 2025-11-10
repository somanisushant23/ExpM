package com.example.expm.network

import com.example.expm.network.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service interface for Expense Manager API calls
 */
interface ApiService {

    // Authentication endpoints
    @POST("users/signin")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("users/signup")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<GenericResponse>

    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserProfile>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfile>

    // Entry endpoints
    @GET("entries")
    suspend fun getEntries(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<EntriesResponse>

    @GET("entries/{id}")
    suspend fun getEntry(
        @Header("Authorization") token: String,
        @Path("id") entryId: Long
    ): Response<EntryResponse>

    @POST("entries")
    suspend fun createEntry(
        @Header("Authorization") token: String,
        @Body entry: EntryRequest
    ): Response<EntryResponse>

    @PUT("entries/{id}")
    suspend fun updateEntry(
        @Header("Authorization") token: String,
        @Path("id") entryId: Long,
        @Body entry: EntryRequest
    ): Response<EntryResponse>

    @DELETE("entries/{id}")
    suspend fun deleteEntry(
        @Header("Authorization") token: String,
        @Path("id") entryId: Long
    ): Response<GenericResponse>

    @POST("entries/sync")
    suspend fun syncEntries(
        @Header("Authorization") token: String,
        @Body entries: List<EntryRequest>
    ): Response<SyncResponse>

    // Transaction endpoints
    @POST("transactions")
    suspend fun postTransactions(
        @Header("Authorization") token: String,
        @Header("email") email: String,
        @Body transactions: List<TransactionRequest>
    ): Response<List<TransactionResponse>>

    @GET("transactions/new-transactions")
    suspend fun getNewTransactions(
        @Header("Authorization") token: String,
        @Header("email") email: String,
        @Query("updatedTime") updatedTime: Long
    ): Response<List<TransactionResponse>>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Header("Authorization") token: String,
        @Header("email") email: String,
        @Path("id") remoteId: Long
    ): Response<GenericResponse>

    @PATCH("transactions/{id}")
    suspend fun updateTransaction(
        @Header("Authorization") token: String,
        @Header("email") email: String,
        @Path("id") remoteId: Long,
        @Body transaction: TransactionRequest
    ): Response<GenericResponse>

    // Category endpoints
    @GET("categories")
    suspend fun getCategories(
        @Header("Authorization") token: String
    ): Response<CategoriesResponse>

    // Analytics endpoints
    @GET("analytics/summary")
    suspend fun getAnalyticsSummary(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: Long? = null,
        @Query("end_date") endDate: Long? = null
    ): Response<AnalyticsSummaryResponse>
}

