package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.example.expm.data.AppDatabase
import com.example.expm.data.Investment
import kotlinx.coroutines.flow.map

data class InvestmentSummary(
    val totalInvested: Int,
    val totalReturns: Int,
    val netGain: Int
)

class InvestmentsViewModel(application: Application) : AndroidViewModel(application) {
    private val investmentDao = AppDatabase.getInstance(application).investmentDao()

    // Sort order: "date_desc", "date_asc", "amount_desc", "amount_asc"
    private val _sortOrder = MutableLiveData<String>("date_desc")

    // Search query
    private val _searchQuery = MutableLiveData<String>("")

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // All investment entries from database
    private val allInvestmentsLiveData: LiveData<List<Investment>> = investmentDao.getAllFlow().asLiveData()

    // Get all investment entries with search and sort applied
    val investmentEntries: LiveData<List<Investment>> = MediatorLiveData<List<Investment>>().apply {
        var currentInvestments: List<Investment>? = null
        var currentSortOrder = "date_desc"
        var currentSearchQuery = ""

        fun update() {
            val investments = currentInvestments ?: return

            // Filter by search query (title, type, notes, or amount)
            val filtered = if (currentSearchQuery.isEmpty()) {
                investments
            } else {
                val query = currentSearchQuery.lowercase()
                investments.filter { investment ->
                    investment.title.lowercase().contains(query) ||
                    investment.type.lowercase().contains(query) ||
                    investment.notes.lowercase().contains(query) ||
                    investment.amount.toString().contains(query)
                }
            }

            // Sort the filtered list
            val sorted = when (currentSortOrder) {
                "date_desc" -> filtered.sortedByDescending { it.principalDateTimestamp }
                "date_asc" -> filtered.sortedBy { it.principalDateTimestamp }
                "amount_desc" -> filtered.sortedByDescending { it.amount }
                "amount_asc" -> filtered.sortedBy { it.amount }
                else -> filtered.sortedByDescending { it.principalDateTimestamp }
            }

            value = sorted
        }

        addSource(allInvestmentsLiveData) { investments ->
            currentInvestments = investments
            update()
        }

        addSource(_sortOrder) { sortOrder ->
            currentSortOrder = sortOrder
            update()
        }

        addSource(_searchQuery) { searchQuery ->
            currentSearchQuery = searchQuery
            update()
        }
    }

    // Get investment summary
    val investmentSummary: LiveData<InvestmentSummary> = investmentDao.getAllFlow().map { list ->
        val totalInvested = list.sumOf { it.amount }
        // In a real app, you would calculate returns based on current market values
        // For now, we'll just show the invested amount
        val totalReturns = 0
        val netGain = totalInvested + totalReturns

        InvestmentSummary(totalInvested, totalReturns, netGain)
    }.asLiveData()
}



