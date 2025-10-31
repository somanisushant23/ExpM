package com.example.expm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(utility: Utility): Long

    @Update
    suspend fun update(utility: Utility): Int

    @Delete
    suspend fun delete(utility: Utility): Int

    @Query("SELECT * FROM utilities ORDER BY updated_on DESC")
    suspend fun getAll(): List<Utility>

    // Reactive single utility by key
    @Query("SELECT * FROM utilities WHERE data_key = :dataKey AND isActive = 1 LIMIT 1")
    fun getByKeyFlow(dataKey: String): Flow<Utility?>

}

