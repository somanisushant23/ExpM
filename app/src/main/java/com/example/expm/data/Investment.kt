package com.example.expm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "investments",
    indices = [
        Index(value = ["clientId"], unique = true)
    ])
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Int,
    val type: String,
    val returnRate: Float,
    val principalDateTimestamp: Long,
    val maturityDateTimestamp: Long,
    val notes: String,
    val created_on: Long = System.currentTimeMillis(),
    val updated_on: Long = System.currentTimeMillis(),
    // Mark whether an entry has been persisted/exported (0 = false, 1 = true)
    val isPersisted: Boolean = false,
    val isDeleted: Boolean = false,
    val isUpdated: Boolean = false,
    val remoteId: Long,
    val clientId: String = UUID.randomUUID().toString()
)