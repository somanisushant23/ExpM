package com.example.expm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(investment: Investment): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(investment: Investment): Int

    @Delete
    suspend fun delete(investment: Investment): Int

    @Query("SELECT * FROM investments ORDER BY created_on DESC")
    suspend fun getAll(): List<Investment>

    // Reactive stream of investments; Room will emit updates when the table changes
    @Query("SELECT * FROM investments WHERE isDeleted = 0 ORDER BY created_on DESC")
    fun getAllFlow(): Flow<List<Investment>>

    // Reactive single Investment by id
    @Query("SELECT * FROM investments WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<Investment?>

    @Query("SELECT * FROM investments WHERE isPersisted = 0 ORDER BY created_on DESC")
    suspend fun getUnsyncInvestments(): List<Investment>
}