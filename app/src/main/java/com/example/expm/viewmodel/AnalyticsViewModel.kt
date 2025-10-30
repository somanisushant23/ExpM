package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.expm.data.AppDatabase
import com.example.expm.data.Entry
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategoryTotal(
    val category: String,
    val total: Double
)

data class MonthlyData(
    val month: String,
    val totalExpense: Double,
    val totalIncome: Double
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).entryDao()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val cal = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonth = cal.get(Calendar.MONTH)

    // Get all entries for current month
    val entriesForCurrentMonth: LiveData<List<Entry>> = dao.getAllFlow().map { list ->
        list.filter { entry ->
            try {
                val d = sdf.parse(entry.date) ?: return@filter false
                val c = Calendar.getInstance()
                c.time = d
                val y = c.get(Calendar.YEAR)
                val m = c.get(Calendar.MONTH)
                y == currentYear && m == currentMonth
            } catch (_: Exception) {
                false
            }
        }
    }.asLiveData()

    // Get expense breakdown by category
    val expenseByCategoryForCurrentMonth: LiveData<List<CategoryTotal>> = dao.getAllFlow().map { list ->
        list.filter { entry ->
            try {
                val d = sdf.parse(entry.date) ?: return@filter false
                val c = Calendar.getInstance()
                c.time = d
                val y = c.get(Calendar.YEAR)
                val m = c.get(Calendar.MONTH)
                entry.type == "Expense" && y == currentYear && m == currentMonth
            } catch (_: Exception) {
                false
            }
        }.groupBy { it.category }
            .map { CategoryTotal(it.key, it.value.sumOf { entry -> entry.amount }) }
            .sortedByDescending { it.total }
    }.asLiveData()


    // Get monthly trends for the last 6 months
    val monthlyTrends: LiveData<List<MonthlyData>> = dao.getAllFlow().map { list ->
        val monthlyMap = mutableMapOf<String, Pair<Double, Double>>() // month -> (expense, income)
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        // Get last 6 months
        val months = mutableListOf<String>()
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            months.add(monthFormat.format(c.time))
            monthlyMap[monthFormat.format(c.time)] = Pair(0.0, 0.0)
        }

        // Group entries by month
        list.forEach { entry ->
            try {
                val d = sdf.parse(entry.date) ?: return@forEach
                val c = Calendar.getInstance()
                c.time = d
                val monthKey = monthFormat.format(c.time)

                if (monthKey in monthlyMap) {
                    val current = monthlyMap[monthKey]!!
                    if (entry.type == "Expense") {
                        monthlyMap[monthKey] = Pair(current.first + entry.amount, current.second)
                    } else {
                        monthlyMap[monthKey] = Pair(current.first, current.second + entry.amount)
                    }
                }
            } catch (_: Exception) {
                // Skip invalid dates
            }
        }

        months.map { month ->
            val data = monthlyMap[month]!!
            MonthlyData(month, data.first, data.second)
        }
    }.asLiveData()
}

