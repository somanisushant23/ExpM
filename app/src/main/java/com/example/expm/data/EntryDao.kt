package com.example.expm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry): Int

    @Delete
    suspend fun delete(entry: Entry): Int

    @Query("SELECT * FROM entries ORDER BY date DESC")
    suspend fun getAll(): List<Entry>

    // Reactive stream of entries; Room will emit updates when the table changes
    @Query("SELECT * FROM entries ORDER BY date DESC")
    fun getAllFlow(): Flow<List<Entry>>

    // Reactive single entry by id
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<Entry?>

    @Query("SELECT * FROM entries WHERE isPersisted = 0 ORDER BY date DESC")
    suspend fun getUnsyncEntries(): List<Entry>
}
