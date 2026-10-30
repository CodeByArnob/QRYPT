package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScanRecordEntity::class,
        ProductCacheEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PrismScanDatabase : RoomDatabase() {
    abstract fun scanRecordDao(): ScanRecordDao
    abstract fun productCacheDao(): ProductCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_cache` (
                        `barcode` TEXT NOT NULL,
                        `formatName` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `quantity` TEXT NOT NULL,
                        `categories` TEXT NOT NULL,
                        `ingredientsText` TEXT NOT NULL,
                        `allergens` TEXT NOT NULL,
                        `packaging` TEXT NOT NULL,
                        `origins` TEXT NOT NULL,
                        `nutriscoreGrade` TEXT,
                        `novaGroup` INTEGER,
                        `energyKcal100g` REAL,
                        `fat100g` REAL,
                        `saturatedFat100g` REAL,
                        `carbohydrates100g` REAL,
                        `sugars100g` REAL,
                        `proteins100g` REAL,
                        `salt100g` REAL,
                        `fiber100g` REAL,
                        `sourceDatabase` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `attributionLicense` TEXT NOT NULL,
                        `fetchedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`barcode`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: PrismScanDatabase? = null

        fun getInstance(context: Context): PrismScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrismScanDatabase::class.java,
                    "prism_scan_vault.db"
                )
                    .addMigrations(MIGRATION_1_2)
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
