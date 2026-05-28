package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Gorev::class, MemoryLog::class, ChatSession::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class TheiaDatabase : RoomDatabase() {
    abstract fun theiaDao(): TheiaDao

    companion object {
        @Volatile
        private var INSTANCE: TheiaDatabase? = null

        fun getDatabase(context: Context): TheiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheiaDatabase::class.java,
                    "theia_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
