package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PrismScanDatabase : RoomDatabase() {
    abstract fun scanRecordDao(): ScanRecordDao

    companion object {
        @Volatile
        private var INSTANCE: PrismScanDatabase? = null

        fun getInstance(context: Context): PrismScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrismScanDatabase::class.java,
                    "prism_scan_vault.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): PrismScanDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PrismScanDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
