package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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

    // Get all investment entries
    val investmentEntries: LiveData<List<Investment>> = investmentDao.getAllFlow().asLiveData()

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



