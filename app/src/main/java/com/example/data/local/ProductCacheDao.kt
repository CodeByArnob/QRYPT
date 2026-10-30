package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductCacheDao {

    @Query("SELECT * FROM product_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun getCachedProduct(barcode: String): ProductCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedProduct(entity: ProductCacheEntity)

    @Query("DELETE FROM product_cache WHERE barcode = :barcode")
    suspend fun deleteCachedProduct(barcode: String)

    @Query("DELETE FROM product_cache")
    suspend fun clearAllCachedProducts()
}
