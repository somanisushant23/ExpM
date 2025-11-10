package com.example.expm.network.models

import com.google.gson.annotations.SerializedName

// ==================== Auth Models ====================

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val name: String,
    val email: String,
    val token: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val message: String
)

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null
)

// ==================== Entry Models ====================

data class EntryRequest(
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    @SerializedName("created_on")
    val createdOn: Long,
    @SerializedName("updated_on")
    val updatedOn: Long,
    val notes: String
)

data class EntryResponse(
    val success: Boolean,
    val message: String? = null,
    val data: EntryData? = null
)

data class EntryData(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    @SerializedName("created_on")
    val createdOn: Long,
    @SerializedName("updated_on")
    val updatedOn: Long,
    val notes: String,
    @SerializedName("is_persisted")
    val isPersisted: Boolean = true
)

data class EntriesResponse(
    val success: Boolean,
    val message: String? = null,
    val data: EntriesData? = null
)

data class EntriesData(
    val entries: List<EntryData>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class SyncResponse(
    val success: Boolean,
    val message: String? = null,
    val data: SyncData? = null
)

data class SyncData(
    val synced: Int,
    val failed: Int,
    val entries: List<EntryData>
)

// ==================== Category Models ====================

data class CategoriesResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CategoriesData? = null
)

data class CategoriesData(
    val categories: List<String>
)

// ==================== Analytics Models ====================

data class AnalyticsSummaryResponse(
    val success: Boolean,
    val message: String? = null,
    val data: AnalyticsSummaryData? = null
)

data class AnalyticsSummaryData(
    @SerializedName("total_income")
    val totalIncome: Double,
    @SerializedName("total_expense")
    val totalExpense: Double,
    val balance: Double,
    @SerializedName("category_breakdown")
    val categoryBreakdown: Map<String, Double>,
    @SerializedName("start_date")
    val startDate: Long? = null,
    @SerializedName("end_date")
    val endDate: Long? = null
)

// ==================== Transaction Models ====================

data class TransactionRequest(
    val title: String,
    val amount: Int,
    val category: String,
    val transactionType: String,
    val transactionDate: String,
    val description: String? = null
)

data class TransactionResponse(
    val id: Long,
    val title: String,
    val description: String? = null,
    val category: String,
    val transactionType: String,
    val amount: Int,
    val transactionDate: String,
    val createdOn: Long,
    val updatedOn: Long
)

// ==================== Generic Models ====================

data class GenericResponse(
    val message: String
)

