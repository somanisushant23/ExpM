package com.example.expm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.expm.data.AppDatabase
import com.example.expm.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val _insertResult = MutableLiveData<Long?>(null)
    val insertResult: LiveData<Long?> = _insertResult

    // operation result for update/delete: true on success, false on failure, null when idle
    private val _operationResult = MutableLiveData<Boolean?>(null)
    val operationResult: LiveData<Boolean?> = _operationResult

    private val db by lazy { AppDatabase.getInstance(application) }

    fun insertEntry(entry: Entry) {
        // reset previous result
        _insertResult.value = null
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                try {
                    db.entryDao().insert(entry)
                } catch (t: Throwable) {
                    -1L
                }
            }
            _insertResult.value = id
        }
    }

    fun getEntry(id: Long): LiveData<Entry?> {
        return db.entryDao().getByIdFlow(id).asLiveData()
    }

    fun updateEntry(entry: Entry) {
        _operationResult.value = null
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    db.entryDao().update(entry.copy(isUpdated = true))
                } catch (_: Throwable) {
                    -1
                }
            }
            _operationResult.value = res > 0
        }
    }

    fun deleteEntry(entry: Entry) {
        _operationResult.value = null
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    db.entryDao().update(entry.copy(isDeleted = true))
                } catch (_: Throwable) {
                    -1
                }
            }
            _operationResult.value = res > 0
        }
    }

    fun clearResults() {
        _insertResult.value = null
        _operationResult.value = null
    }
}
