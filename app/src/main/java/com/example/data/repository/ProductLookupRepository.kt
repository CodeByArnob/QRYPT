package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.ProductCacheDao
import com.example.data.local.ProductCacheEntity
import com.example.data.remote.OpenFoodFactsService
import com.example.data.remote.ProductLookupRemoteDataSource
import com.example.data.remote.RemoteLookupResult
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.BarcodeValidator
import com.example.domain.model.ProductLookupState

fun interface NetworkConnectivityChecker {
    fun isNetworkAvailable(): Boolean
}

class AndroidNetworkConnectivityChecker(
    private val context: Context
) : NetworkConnectivityChecker {
    override fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }
}

class ProductLookupRepository(
    private val productCacheDao: ProductCacheDao,
    private val remoteDataSource: ProductLookupRemoteDataSource = OpenFoodFactsService(),
    private val connectivityChecker: NetworkConnectivityChecker
) {

    suspend fun lookupProduct(
        rawBarcode: String,
        format: BarcodeFormatType = BarcodeFormatType.EAN_13,
        forceRefresh: Boolean = false
    ): ProductLookupState {
        val validation = BarcodeValidator.validateProductBarcode(rawBarcode, format)
        if (!validation.isValid) {
            return ProductLookupState.InvalidBarcode(
                rawInput = rawBarcode,
                reason = validation.errorReason ?: "Invalid product barcode format."
            )
        }

        val normalized = validation.normalizedBarcode

        // 1. Check local Room cache first unless forceRefresh is requested
        if (!forceRefresh) {
            val cached = productCacheDao.getCachedProduct(normalized)
            if (cached != null) {
                return ProductLookupState.Success(cached.toDomainModel(isFromCache = true))
            }
        }

        // 2. Check network connectivity
        if (!connectivityChecker.isNetworkAvailable()) {
            val fallbackCached = productCacheDao.getCachedProduct(normalized)
            if (fallbackCached != null) {
                return ProductLookupState.Success(fallbackCached.toDomainModel(isFromCache = true))
            }
            return ProductLookupState.Offline(
                barcode = normalized,
                formatDisplayName = validation.detectedSymbology,
                gs1Origin = validation.gs1Origin
            )
        }

        // 3. Query Open Food Facts API v3 over HTTPS
        return when (val remoteResult = remoteDataSource.fetchProductByBarcode(normalized, validation.detectedSymbology)) {
            is RemoteLookupResult.Found -> {
                val product = remoteResult.product
                productCacheDao.insertCachedProduct(ProductCacheEntity.fromDomainModel(product))
                ProductLookupState.Success(product)
            }

            is RemoteLookupResult.NotFound -> {
                ProductLookupState.NotFound(
                    barcode = normalized,
                    formatDisplayName = validation.detectedSymbology,
                    gs1Origin = validation.gs1Origin
                )
            }

            is RemoteLookupResult.NetworkError -> {
                val fallbackCached = productCacheDao.getCachedProduct(normalized)
                if (fallbackCached != null) {
                    ProductLookupState.Success(fallbackCached.toDomainModel(isFromCache = true))
                } else {
                    ProductLookupState.Error(
                        barcode = normalized,
                        formatDisplayName = validation.detectedSymbology,
                        errorMessage = remoteResult.message
                    )
                }
            }
        }
    }

    suspend fun clearCache() {
        productCacheDao.clearAllCachedProducts()
    }
}
