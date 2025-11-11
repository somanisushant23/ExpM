package com.example.expm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.example.expm.data.AppDatabase
import com.example.expm.data.Entry
import com.example.expm.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).entryDao()
    private val utilityDao = AppDatabase.getInstance(application).utilityDao()

    // Prepare date helpers
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val cal = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonth = cal.get(Calendar.MONTH)

    // Filter type: "All", "Expense", or "Income"
    private val _filterType = MutableLiveData<String>("All")

    // Sort order: "date_desc", "date_asc", "amount_desc", "amount_asc"
    private val _sortOrder = MutableLiveData<String>("date_desc")

    // Search query
    private val _searchQuery = MutableLiveData<String>("")

    // Start and end timestamps from Utility DB
    private val startTimestampLiveData: LiveData<Long?> = utilityDao.getByKeyFlow("start_date")
        .asLiveData()
        .map { it?.data_value?.toLongOrNull() }

    private val endTimestampLiveData: LiveData<Long?> = utilityDao.getByKeyFlow("end_date")
        .asLiveData()
        .map { it?.data_value?.toLongOrNull() }

    // Expose formatted dates for UI
    val startDate: LiveData<String?> = startTimestampLiveData.map { timestamp ->
        timestamp?.let { AppUtils.formatTimestampToDate(it) }
    }

    val endDate: LiveData<String?> = endTimestampLiveData.map { timestamp ->
        timestamp?.let { AppUtils.formatTimestampToDate(it) }
    }

    private fun <T, R> LiveData<T>.map(transform: (T) -> R): LiveData<R> {
        val result = MediatorLiveData<R>()
        result.addSource(this) { value ->
            result.value = transform(value)
        }
        return result
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // All entries from database
    private val allEntriesLiveData: LiveData<List<Entry>> = dao.getAllFlow().asLiveData()

    // Expose entries filtered and sorted
    val entriesForCurrentMonth: LiveData<List<Entry>> = MediatorLiveData<List<Entry>>().apply {
        var currentEntries: List<Entry>? = null
        var currentFilterType: String = "All"
        var currentSortOrder: String = "date_desc"
        var currentSearchQuery: String = ""
        var currentStartTimestamp: Long? = null
        var currentEndTimestamp: Long? = null

        fun update() {
            val entries = currentEntries ?: return

            // Filter by timestamp range and type
            val filtered = entries.filter { entry ->
                // First filter by timestamp range
                val isInRange = if (currentStartTimestamp != null && currentEndTimestamp != null) {
                    entry.created_on >= currentStartTimestamp!! && entry.created_on <= currentEndTimestamp!!
                } else {
                    // If timestamps are not set, fall back to current month
                    try {
                        AppUtils.isTimestampInCurrentMonth(entry.created_on)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error occurred: ", e)
                        false
                    }
                }

                // Then filter by type
                val matchesType = when (currentFilterType) {
                    "All" -> true
                    "Expense" -> entry.type.equals("Expense", true)
                    "Income" -> entry.type.equals("Income", true)
                    else -> true
                }

                // Then filter by search query (title, amount, or notes)
                val matchesSearch = if (currentSearchQuery.isEmpty()) {
                    true
                } else {
                    val query = currentSearchQuery.lowercase()
                    entry.title.lowercase().contains(query) ||
                    entry.amount.toString().contains(query) ||
                    entry.notes.lowercase().contains(query)
                }

                isInRange && matchesType && matchesSearch
            }

            // Sort the filtered list
            val sorted = when (currentSortOrder) {
                "date_desc" -> filtered.sortedByDescending { it.created_on.toString() }
                "date_asc" -> filtered.sortedBy { it.created_on.toString() }
                "amount_desc" -> filtered.sortedByDescending { it.amount }
                "amount_asc" -> filtered.sortedBy { it.amount }
                else -> filtered.sortedByDescending { it.created_on.toString() }
            }

            value = sorted
        }

        addSource(allEntriesLiveData) { entries ->
            currentEntries = entries
            update()
        }

        addSource(_filterType) { filterType ->
            currentFilterType = filterType
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

        addSource(startTimestampLiveData) { startTimestamp ->
            currentStartTimestamp = startTimestamp
            update()
        }

        addSource(endTimestampLiveData) { endTimestamp ->
            currentEndTimestamp = endTimestamp
            update()
        }
    }
}
