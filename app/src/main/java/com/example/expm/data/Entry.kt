package com.example.expm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "entries",
    indices = [
        Index(value = ["clientId"], unique = true)
    ])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val transactionDateTimestamp: Long,
    val created_on: Long = System.currentTimeMillis(),
    val updated_on: Long = System.currentTimeMillis(),
    val notes: String,
    // Mark whether an entry has been persisted/exported (0 = false, 1 = true)
    val isPersisted: Boolean = false,
    val isDeleted: Boolean = false,
    val isUpdated: Boolean = false,
    val remoteId: Long,
    val clientId: String = UUID.randomUUID().toString()
)
