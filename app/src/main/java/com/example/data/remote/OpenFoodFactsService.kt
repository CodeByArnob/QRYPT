package com.example.data.remote

import com.example.domain.model.ProductInfo
import com.example.domain.model.ProductNutritionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface RemoteLookupResult {
    data class Found(val product: ProductInfo) : RemoteLookupResult
    data class NotFound(val barcode: String) : RemoteLookupResult
    data class NetworkError(val message: String, val isTimeout: Boolean = false) : RemoteLookupResult
}

interface ProductLookupRemoteDataSource {
    suspend fun fetchProductByBarcode(barcode: String, formatName: String): RemoteLookupResult
}

class OpenFoodFactsService(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val baseUrl: String = DEFAULT_BASE_URL
) : ProductLookupRemoteDataSource {

    override suspend fun fetchProductByBarcode(
        barcode: String,
        formatName: String
    ): RemoteLookupResult = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim().filter { it.isDigit() || it.uppercaseChar() == 'X' }
        if (cleanBarcode.isEmpty()) {
            return@withContext RemoteLookupResult.NotFound(barcode)
        }

        val endpoint = "${baseUrl.trimEnd('/')}/api/v3/product/$cleanBarcode.json?fields=$REQUESTED_FIELDS"
        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.code == 404) {
                    return@withContext RemoteLookupResult.NotFound(cleanBarcode)
                }
                if (!response.isSuccessful) {
                    return@withContext RemoteLookupResult.NetworkError(
                        message = "Open Food Facts server returned HTTP ${response.code}.",
                        isTimeout = response.code == 408 || response.code == 504
                    )
                }
                if (bodyString.isBlank()) {
                    return@withContext RemoteLookupResult.NetworkError(
                        message = "Received an empty response from Open Food Facts."
                    )
                }
                parseOpenFoodFactsResponse(bodyString, cleanBarcode, formatName)
            }
        } catch (_: SocketTimeoutException) {
            RemoteLookupResult.NetworkError(
                message = "Product lookup timed out while contacting Open Food Facts. Please check your connection and try again.",
                isTimeout = true
            )
        } catch (e: IOException) {
            RemoteLookupResult.NetworkError(
                message = "Unable to reach Open Food Facts (${e.localizedMessage ?: "network error"}).",
                isTimeout = false
            )
        } catch (e: Exception) {
            RemoteLookupResult.NetworkError(
                message = "Unexpected error while parsing product data: ${e.localizedMessage ?: "unknown error"}."
            )
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://world.openfoodfacts.org"
        const val USER_AGENT = "QRYPT/1.1.0 (https://github.com/CodeByArnob/QRYPT)"

        private const val REQUESTED_FIELDS =
            "code,product_name,product_name_en,generic_name,brands,image_front_url,image_url,image_small_url," +
                "quantity,categories,ingredients_text,ingredients_text_en,nutriscore_grade,nova_group," +
                "nutriments,allergens,origins,packaging,url"

        fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        fun parseOpenFoodFactsResponse(
            jsonString: String,
            barcode: String,
            formatName: String
        ): RemoteLookupResult {
            return try {
                val root = JSONObject(jsonString)

                // Check v3 result object if present
                val resultObj = root.optJSONObject("result")
                val resultId = resultObj?.optString("id").orEmpty()
                if (resultId.equals("product_not_found", ignoreCase = true)) {
                    return RemoteLookupResult.NotFound(barcode)
                }

                // Check status field (v3 string "status": "failure" or v2 int "status": 0)
                val statusRaw = root.opt("status")
                if (statusRaw is String && statusRaw.equals("failure", ignoreCase = true)) {
                    return RemoteLookupResult.NotFound(barcode)
                }
                if (statusRaw is Number && statusRaw.toInt() == 0) {
                    return RemoteLookupResult.NotFound(barcode)
                }

                val productObj = root.optJSONObject("product")
                    ?: return RemoteLookupResult.NotFound(barcode)

                val rawName = firstNonBlank(
                    productObj.optString("product_name"),
                    productObj.optString("product_name_en"),
                    productObj.optString("generic_name")
                )
                val brand = cleanTagString(productObj.optString("brands"))
                val quantity = productObj.optString("quantity").trim()
                val categories = cleanTagString(productObj.optString("categories"))
                val ingredients = firstNonBlank(
                    productObj.optString("ingredients_text"),
                    productObj.optString("ingredients_text_en")
                )
                val allergens = cleanTagString(productObj.optString("allergens"))
                val origins = cleanTagString(productObj.optString("origins"))
                val packaging = cleanTagString(productObj.optString("packaging"))

                if (rawName.isBlank() && brand.isBlank() && categories.isBlank()) {
                    return RemoteLookupResult.NotFound(barcode)
                }

                val resolvedName = when {
                    rawName.isNotBlank() -> rawName
                    brand.isNotBlank() -> "$brand Product"
                    else -> "Product $barcode"
                }

                val imageUrl = firstNonBlank(
                    productObj.optString("image_front_url"),
                    productObj.optString("image_url"),
                    productObj.optString("image_small_url")
                ).takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }

                val nutriscore = productObj.optString("nutriscore_grade")
                    .trim()
                    .uppercase(Locale.US)
                    .takeIf { it in setOf("A", "B", "C", "D", "E") }

                val novaGroup = productObj.optInt("nova_group", -1).takeIf { it in 1..4 }
                val nutriments = productObj.optJSONObject("nutriments")

                val nutrition = ProductNutritionInfo(
                    nutriscoreGrade = nutriscore,
                    novaGroup = novaGroup,
                    energyKcal100g = nutriments.optPositiveDouble("energy-kcal_100g")
                        ?: nutriments.optPositiveDouble("energy-kcal"),
                    fat100g = nutriments.optPositiveDouble("fat_100g"),
                    saturatedFat100g = nutriments.optPositiveDouble("saturated-fat_100g"),
                    carbohydrates100g = nutriments.optPositiveDouble("carbohydrates_100g"),
                    sugars100g = nutriments.optPositiveDouble("sugars_100g"),
                    proteins100g = nutriments.optPositiveDouble("proteins_100g"),
                    salt100g = nutriments.optPositiveDouble("salt_100g"),
                    fiber100g = nutriments.optPositiveDouble("fiber_100g")
                )

                val referenceUrl = productObj.optString("url")
                    .trim()
                    .takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
                    ?: "https://world.openfoodfacts.org/product/$barcode"

                RemoteLookupResult.Found(
                    ProductInfo(
                        barcode = barcode,
                        formatName = formatName,
                        productName = resolvedName,
                        brand = brand,
                        imageUrl = imageUrl,
                        quantity = quantity,
                        categories = categories,
                        ingredientsText = ingredients,
                        allergens = allergens,
                        packaging = packaging,
                        origins = origins,
                        nutrition = nutrition,
                        sourceDatabase = "Open Food Facts",
                        sourceUrl = referenceUrl,
                        attributionLicense = "Open Database License (ODbL)",
                        fetchedAtEpochMs = System.currentTimeMillis(),
                        isFromCache = false
                    )
                )
            } catch (e: Exception) {
                RemoteLookupResult.NetworkError(
                    message = "Failed to parse Open Food Facts response: ${e.localizedMessage ?: "invalid JSON"}."
                )
            }
        }

        private fun firstNonBlank(vararg values: String?): String {
            for (v in values) {
                if (!v.isNullOrBlank() && v.trim() != "null") {
                    return v.trim()
                }
            }
            return ""
        }

        private fun cleanTagString(raw: String?): String {
            if (raw.isNullOrBlank() || raw.trim() == "null") return ""
            return raw.split(",")
                .map { token ->
                    token.trim()
                        .removePrefix("en:")
                        .removePrefix("fr:")
                        .replace("-", " ")
                        .trim()
                }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
        }

        private fun JSONObject?.optPositiveDouble(key: String): Double? {
            if (this == null || !has(key)) return null
            val v = optDouble(key, Double.NaN)
            return if (!v.isNaN() && v >= 0.0) v else null
        }
    }
}
