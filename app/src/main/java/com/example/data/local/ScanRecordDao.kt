package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC, id DESC")
    fun observeAllRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC, id DESC")
    suspend fun getAllRecordsSnapshot(): List<ScanRecordEntity>

    @Query("SELECT * FROM scan_records WHERE rawValue = :rawValue ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun findLatestByRawValue(rawValue: String): ScanRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<ScanRecordEntity>)

    @Query("UPDATE scan_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE scan_records SET customNotes = :notes WHERE id = :id")
    suspend fun updateCustomNotes(id: Long, notes: String)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_records WHERE isFavorite = 0")
    suspend fun deleteNonFavorites()

    @Query("DELETE FROM scan_records")
    suspend fun clearAll()
}
