package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.example.expm.data.AppDatabase
import com.example.expm.data.Entry
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).entryDao()

    // Prepare date helpers
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val cal = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonth = cal.get(Calendar.MONTH)

    // Filter type: "All", "Expense", or "Income"
    private val _filterType = MutableLiveData<String>("All")

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    // Expose entries filtered to current month as LiveData so the UI can observe reactively
    val entriesForCurrentMonth: LiveData<List<Entry>> = _filterType.switchMap { filterType ->
        dao.getAllFlow().map { list ->
            list.filter { entry ->
                // First filter by current month
                val isCurrentMonth = try {
                    val d = sdf.parse(entry.date) ?: return@filter false
                    val c = Calendar.getInstance()
                    c.time = d
                    val y = c.get(Calendar.YEAR)
                    val m = c.get(Calendar.MONTH)
                    y == currentYear && m == currentMonth
                } catch (_: Exception) {
                    false
                }

                // Then filter by type
                if (!isCurrentMonth) {
                    false
                } else {
                    when (filterType) {
                        "All" -> true
                        "Expense" -> entry.type == "Expense"
                        "Income" -> entry.type == "Income"
                        else -> true
                    }
                }
            }
        }.asLiveData()
    }
}

