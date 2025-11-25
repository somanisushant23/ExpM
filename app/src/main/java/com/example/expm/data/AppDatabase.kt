package com.example.expm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(entities = [Entry::class, Utility::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun utilityDao(): UtilityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 6 to 7: Add clientId column to entries table
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val currentTime = System.currentTimeMillis()

                // SQLite doesn't support adding NOT NULL columns directly
                // We need to recreate the table with the new schema

                // 1. Create new table with clientId column
                database.execSQL("""
                    CREATE TABLE entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        created_on INTEGER NOT NULL,
                        updated_on INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        isPersisted INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        isUpdated INTEGER NOT NULL,
                        remoteId INTEGER NOT NULL,
                        clientId TEXT NOT NULL
                    )
                """.trimIndent())

                // 2. Copy data from old table to new table, generating UUIDs and updating timestamps
                database.execSQL("""
                    INSERT INTO entries_new (id, title, amount, type, category, created_on, updated_on, notes, isPersisted, isDeleted, isUpdated, remoteId, clientId)
                    SELECT id, title, amount, type, category, created_on, $currentTime, notes, isPersisted, isDeleted, 1, remoteId, ''
                    FROM entries
                """.trimIndent())

                // 3. Update each row with a unique UUID for clientId
                database.query("SELECT id FROM entries_new").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val uuid = UUID.randomUUID().toString()
                        database.execSQL("UPDATE entries_new SET clientId = ? WHERE id = ?", arrayOf<Any>(uuid, id))
                    }
                }

                // 4. Drop old table
                database.execSQL("DROP TABLE entries")

                // 5. Rename new table to original name
                database.execSQL("ALTER TABLE entries_new RENAME TO entries")
            }
        }

        // Migration from version 9 to 10: Add unique indices on clientId and remoteId
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create unique index on clientId
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_entries_clientId ON entries(clientId)")

                // Create unique index on remoteId
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_entries_remoteId ON entries(remoteId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expm_db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
