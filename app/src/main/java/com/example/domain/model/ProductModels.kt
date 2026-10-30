package com.example.domain.model

import com.example.domain.parser.PayloadParser
import java.util.Locale

data class ProductNutritionInfo(
    val nutriscoreGrade: String? = null,
    val novaGroup: Int? = null,
    val energyKcal100g: Double? = null,
    val fat100g: Double? = null,
    val saturatedFat100g: Double? = null,
    val carbohydrates100g: Double? = null,
    val sugars100g: Double? = null,
    val proteins100g: Double? = null,
    val salt100g: Double? = null,
    val fiber100g: Double? = null
) {
    val hasAnyData: Boolean
        get() = !nutriscoreGrade.isNullOrBlank() ||
            novaGroup != null ||
            energyKcal100g != null ||
            fat100g != null ||
            saturatedFat100g != null ||
            carbohydrates100g != null ||
            sugars100g != null ||
            proteins100g != null ||
            salt100g != null ||
            fiber100g != null
}

data class ProductInfo(
    val barcode: String,
    val formatName: String,
    val productName: String,
    val brand: String,
    val imageUrl: String? = null,
    val quantity: String = "",
    val categories: String = "",
    val ingredientsText: String = "",
    val allergens: String = "",
    val packaging: String = "",
    val origins: String = "",
    val nutrition: ProductNutritionInfo = ProductNutritionInfo(),
    val sourceDatabase: String = "Open Food Facts",
    val sourceUrl: String = "https://world.openfoodfacts.org/product/$barcode",
    val attributionLicense: String = "Open Database License (ODbL)",
    val fetchedAtEpochMs: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false
) {
    fun buildShareSummary(): String = buildString {
        appendLine("Product: $productName")
        if (brand.isNotBlank()) appendLine("Brand: $brand")
        appendLine("Barcode ($formatName): $barcode")
        if (quantity.isNotBlank()) appendLine("Quantity: $quantity")
        if (categories.isNotBlank()) appendLine("Category: $categories")
        if (!nutrition.nutriscoreGrade.isNullOrBlank()) {
            appendLine("Nutri-Score: ${nutrition.nutriscoreGrade.uppercase(Locale.US)}")
        }
        if (ingredientsText.isNotBlank()) {
            appendLine("Ingredients: $ingredientsText")
        }
        append("Source ($sourceDatabase): $sourceUrl")
    }
}

sealed interface ProductLookupState {
    data object Idle : ProductLookupState

    data class Loading(
        val barcode: String,
        val formatDisplayName: String
    ) : ProductLookupState

    data class Success(
        val product: ProductInfo
    ) : ProductLookupState

    data class NotFound(
        val barcode: String,
        val formatDisplayName: String,
        val gs1Origin: String,
        val message: String = "No matching product record was found in Open Food Facts for barcode $barcode. Open Food Facts is an open food and beverage database, so non-food items or regional barcodes may not be listed yet."
    ) : ProductLookupState

    data class Offline(
        val barcode: String,
        val formatDisplayName: String,
        val gs1Origin: String,
        val message: String = "Online product lookup is unavailable because the device is currently offline. Your decoded barcode is ready below and can be searched once internet connectivity is restored."
    ) : ProductLookupState

    data class Error(
        val barcode: String,
        val formatDisplayName: String,
        val errorMessage: String
    ) : ProductLookupState

    data class InvalidBarcode(
        val rawInput: String,
        val reason: String
    ) : ProductLookupState
}

object BarcodeValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val normalizedBarcode: String,
        val detectedSymbology: String,
        val isIsbn: Boolean,
        val gs1Origin: String,
        val errorReason: String? = null
    )

    fun isProductBarcodeCandidate(rawValue: String, format: BarcodeFormatType, payloadType: ParsedPayloadType): Boolean {
        if (payloadType == ParsedPayloadType.PRODUCT || payloadType == ParsedPayloadType.ISBN) {
            return true
        }
        if (format in listOf(
                BarcodeFormatType.EAN_13,
                BarcodeFormatType.EAN_8,
                BarcodeFormatType.UPC_A,
                BarcodeFormatType.UPC_E
            )
        ) {
            return true
        }
        val cleaned = rawValue.trim().replace("-", "").replace(" ", "")
        return cleaned.length in listOf(8, 12, 13, 14) && cleaned.all { it.isDigit() }
    }

    fun validateProductBarcode(
        rawInput: String,
        format: BarcodeFormatType = BarcodeFormatType.EAN_13
    ): ValidationResult {
        val cleaned = rawInput.trim().replace("-", "").replace(" ", "")
        if (cleaned.isEmpty()) {
            return ValidationResult(
                isValid = false,
                normalizedBarcode = "",
                detectedSymbology = format.displayName,
                isIsbn = false,
                gs1Origin = "Unknown",
                errorReason = "Barcode input cannot be empty."
            )
        }

        // Check ISBN-10 (9 digits + digit or X)
        if (cleaned.length == 10 && cleaned.take(9).all { it.isDigit() } &&
            (cleaned.last().isDigit() || cleaned.last().uppercaseChar() == 'X')
        ) {
            if (!isValidIsbn10(cleaned)) {
                return ValidationResult(
                    isValid = false,
                    normalizedBarcode = cleaned,
                    detectedSymbology = "ISBN-10",
                    isIsbn = true,
                    gs1Origin = "Bookland (ISBN)",
                    errorReason = "Invalid ISBN-10 check character for $cleaned."
                )
            }
            val isbn13 = convertIsbn10ToIsbn13(cleaned)
            return ValidationResult(
                isValid = true,
                normalizedBarcode = isbn13,
                detectedSymbology = "ISBN-10 / ISBN-13",
                isIsbn = true,
                gs1Origin = "Bookland (ISBN)"
            )
        }

        if (!cleaned.all { it.isDigit() }) {
            return ValidationResult(
                isValid = false,
                normalizedBarcode = cleaned,
                detectedSymbology = format.displayName,
                isIsbn = false,
                gs1Origin = "Unknown",
                errorReason = "Product barcode must contain only numeric digits (8, 12, 13, or 14 digits)."
            )
        }

        if (cleaned.length !in listOf(8, 12, 13, 14)) {
            return ValidationResult(
                isValid = false,
                normalizedBarcode = cleaned,
                detectedSymbology = format.displayName,
                isIsbn = false,
                gs1Origin = "Unknown",
                errorReason = "Unsupported product barcode length (${cleaned.length} digits). Expected EAN-8 (8), UPC-A (12), EAN-13 / ISBN-13 (13), or GTIN-14 (14)."
            )
        }

        // For UPC-E (8 digits starting with 0 or 1), or standard Modulo-10 GTINs:
        val isUpcE = format == BarcodeFormatType.UPC_E && cleaned.length == 8 && (cleaned.startsWith("0") || cleaned.startsWith("1"))
        val validCheckDigit = PayloadParser.isValidGtin(cleaned) || isUpcE
        if (!validCheckDigit) {
            return ValidationResult(
                isValid = false,
                normalizedBarcode = cleaned,
                detectedSymbology = format.displayName,
                isIsbn = false,
                gs1Origin = PayloadParser.lookupGs1Country(cleaned),
                errorReason = "Invalid GS1 Modulo-10 check digit for barcode $cleaned."
            )
        }

        val isIsbn13 = cleaned.length == 13 && (cleaned.startsWith("978") || cleaned.startsWith("979"))
        val symbologyName = when {
            isIsbn13 -> "ISBN-13"
            cleaned.length == 8 && format == BarcodeFormatType.UPC_E -> "UPC-E"
            cleaned.length == 8 -> "EAN-8"
            cleaned.length == 12 -> "UPC-A"
            cleaned.length == 13 -> "EAN-13"
            cleaned.length == 14 -> "GTIN-14"
            else -> format.displayName
        }

        return ValidationResult(
            isValid = true,
            normalizedBarcode = cleaned,
            detectedSymbology = symbologyName,
            isIsbn = isIsbn13,
            gs1Origin = PayloadParser.lookupGs1Country(cleaned)
        )
    }

    private fun isValidIsbn10(isbn10: String): Boolean {
        if (isbn10.length != 10) return false
        var sum = 0
        for (i in 0 until 9) {
            val ch = isbn10[i]
            if (!ch.isDigit()) return false
            sum += (10 - i) * ch.digitToInt()
        }
        val last = isbn10[9].uppercaseChar()
        val lastVal = when {
            last == 'X' -> 10
            last.isDigit() -> last.digitToInt()
            else -> return false
        }
        sum += lastVal
        return sum % 11 == 0
    }

    private fun convertIsbn10ToIsbn13(isbn10: String): String {
        val base12 = "978" + isbn10.take(9)
        val check = PayloadParser.calculateGtinCheckDigit(base12)
        return "$base12$check"
    }
}
