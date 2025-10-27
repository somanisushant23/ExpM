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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).entryDao()

    // Prepare date helpers
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val cal = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonth = cal.get(Calendar.MONTH)

    // Expose entries filtered to current month as LiveData so the UI can observe reactively
    val entriesForCurrentMonth: LiveData<List<Entry>> = dao.getAllFlow().map { list ->
        list.filter { entry ->
            try {
                val d = sdf.parse(entry.date) ?: return@filter false
                val c = Calendar.getInstance()
                c.time = d
                val y = c.get(Calendar.YEAR)
                val m = c.get(Calendar.MONTH)
                y == currentYear && m == currentMonth
            } catch (e: Exception) {
                false
            }
        }
    }.asLiveData()
}

