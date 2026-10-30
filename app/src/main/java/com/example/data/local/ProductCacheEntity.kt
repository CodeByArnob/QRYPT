package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ProductInfo
import com.example.domain.model.ProductNutritionInfo

@Entity(tableName = "product_cache")
data class ProductCacheEntity(
    @PrimaryKey val barcode: String,
    val formatName: String,
    val productName: String,
    val brand: String,
    val imageUrl: String?,
    val quantity: String,
    val categories: String,
    val ingredientsText: String,
    val allergens: String,
    val packaging: String,
    val origins: String,
    val nutriscoreGrade: String?,
    val novaGroup: Int?,
    val energyKcal100g: Double?,
    val fat100g: Double?,
    val saturatedFat100g: Double?,
    val carbohydrates100g: Double?,
    val sugars100g: Double?,
    val proteins100g: Double?,
    val salt100g: Double?,
    val fiber100g: Double?,
    val sourceDatabase: String,
    val sourceUrl: String,
    val attributionLicense: String,
    val fetchedAtEpochMs: Long
) {
    fun toDomainModel(isFromCache: Boolean = true): ProductInfo {
        return ProductInfo(
            barcode = barcode,
            formatName = formatName,
            productName = productName,
            brand = brand,
            imageUrl = imageUrl,
            quantity = quantity,
            categories = categories,
            ingredientsText = ingredientsText,
            allergens = allergens,
            packaging = packaging,
            origins = origins,
            nutrition = ProductNutritionInfo(
                nutriscoreGrade = nutriscoreGrade,
                novaGroup = novaGroup,
                energyKcal100g = energyKcal100g,
                fat100g = fat100g,
                saturatedFat100g = saturatedFat100g,
                carbohydrates100g = carbohydrates100g,
                sugars100g = sugars100g,
                proteins100g = proteins100g,
                salt100g = salt100g,
                fiber100g = fiber100g
            ),
            sourceDatabase = sourceDatabase,
            sourceUrl = sourceUrl,
            attributionLicense = attributionLicense,
            fetchedAtEpochMs = fetchedAtEpochMs,
            isFromCache = isFromCache
        )
    }

    companion object {
        fun fromDomainModel(info: ProductInfo): ProductCacheEntity {
            return ProductCacheEntity(
                barcode = info.barcode,
                formatName = info.formatName,
                productName = info.productName,
                brand = info.brand,
                imageUrl = info.imageUrl,
                quantity = info.quantity,
                categories = info.categories,
                ingredientsText = info.ingredientsText,
                allergens = info.allergens,
                packaging = info.packaging,
                origins = info.origins,
                nutriscoreGrade = info.nutrition.nutriscoreGrade,
                novaGroup = info.nutrition.novaGroup,
                energyKcal100g = info.nutrition.energyKcal100g,
                fat100g = info.nutrition.fat100g,
                saturatedFat100g = info.nutrition.saturatedFat100g,
                carbohydrates100g = info.nutrition.carbohydrates100g,
                sugars100g = info.nutrition.sugars100g,
                proteins100g = info.nutrition.proteins100g,
                salt100g = info.nutrition.salt100g,
                fiber100g = info.nutrition.fiber100g,
                sourceDatabase = info.sourceDatabase,
                sourceUrl = info.sourceUrl,
                attributionLicense = info.attributionLicense,
                fetchedAtEpochMs = info.fetchedAtEpochMs
            )
        }
    }
}
