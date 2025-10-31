package com.example.expm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utilities")
data class Utility(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data_key: String,
    val data_value: String,
    val created_on: String,
    val updated_on: String,
    val isActive: Boolean = true
)

