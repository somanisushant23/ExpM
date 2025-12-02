package com.example.expm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.expm.data.AppDatabase
import com.example.expm.data.Investment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddInvestmentViewModel(application: Application) : AndroidViewModel(application) {
    private val _insertResult = MutableLiveData<Long?>(null)
    val insertResult: LiveData<Long?> = _insertResult

    private val db by lazy { AppDatabase.getInstance(application) }

    fun insertInvestment(investment: Investment) {
        // reset previous result
        _insertResult.value = null
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                try {
                    db.investmentDao().insert(investment)
                } catch (t: Throwable) {
                    Log.e("AddInvestmentViewModel", "Error inserting investment", t)
                    -1L
                }
            }
            _insertResult.value = id
        }
    }

    fun updateInvestment(investment: Investment) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    db.investmentDao().update(investment)
                } catch (t: Throwable) {
                    Log.e("AddInvestmentViewModel", "Error updating investment", t)
                }
            }
        }
    }

    fun clearResults() {
        _insertResult.value = null
    }
}

