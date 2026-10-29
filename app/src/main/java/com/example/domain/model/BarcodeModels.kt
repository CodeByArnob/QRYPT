package com.example.domain.model

import android.graphics.PointF
import android.graphics.RectF
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat

enum class BarcodeFormatType(
    val displayName: String,
    val shortTag: String,
    val is2D: Boolean,
    val mlKitFormat: Int,
    val zxingFormat: BarcodeFormat,
    val formatHint: String,
    val samplePayload: String
) {
    QR_CODE(
        displayName = "QR Code",
        shortTag = "QR",
        is2D = true,
        mlKitFormat = Barcode.FORMAT_QR_CODE,
        zxingFormat = BarcodeFormat.QR_CODE,
        formatHint = "Any text, URL, Wi-Fi, vCard, or structured data (up to 4,296 chars)",
        samplePayload = "https://developer.android.com/jetpack/compose"
    ),
    EAN_13(
        displayName = "EAN-13",
        shortTag = "EAN13",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_EAN_13,
        zxingFormat = BarcodeFormat.EAN_13,
        formatHint = "12 or 13 digits (retail product GTIN-13; check digit auto-calculated)",
        samplePayload = "5901234123457"
    ),
    EAN_8(
        displayName = "EAN-8",
        shortTag = "EAN8",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_EAN_8,
        zxingFormat = BarcodeFormat.EAN_8,
        formatHint = "7 or 8 digits (compact retail package barcode)",
        samplePayload = "96385074"
    ),
    UPC_A(
        displayName = "UPC-A",
        shortTag = "UPCA",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_UPC_A,
        zxingFormat = BarcodeFormat.UPC_A,
        formatHint = "11 or 12 digits (North American retail GTIN-12)",
        samplePayload = "036000291452"
    ),
    UPC_E(
        displayName = "UPC-E",
        shortTag = "UPCE",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_UPC_E,
        zxingFormat = BarcodeFormat.UPC_E,
        formatHint = "7 or 8 digits starting with 0 or 1 (zero-suppressed UPC)",
        samplePayload = "04252614"
    ),
    CODE_128(
        displayName = "Code 128",
        shortTag = "C128",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_CODE_128,
        zxingFormat = BarcodeFormat.CODE_128,
        formatHint = "ASCII alphanumeric logistics & shipping barcode",
        samplePayload = "PRISM-2026-X99"
    ),
    CODE_39(
        displayName = "Code 39",
        shortTag = "C39",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_CODE_39,
        zxingFormat = BarcodeFormat.CODE_39,
        formatHint = "Uppercase letters A-Z, digits 0-9, and - . $ / + % SPACE",
        samplePayload = "ASSET-4092-B"
    ),
    CODE_93(
        displayName = "Code 93",
        shortTag = "C93",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_CODE_93,
        zxingFormat = BarcodeFormat.CODE_93,
        formatHint = "High-density uppercase alphanumeric industrial barcode",
        samplePayload = "INV-93048"
    ),
    CODABAR(
        displayName = "Codabar",
        shortTag = "CBAR",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_CODABAR,
        zxingFormat = BarcodeFormat.CODABAR,
        formatHint = "Digits 0-9 wrapped in start/stop A, B, C, or D (e.g., A40156B)",
        samplePayload = "A40156789B"
    ),
    ITF(
        displayName = "ITF (Interleaved 2 of 5)",
        shortTag = "ITF",
        is2D = false,
        mlKitFormat = Barcode.FORMAT_ITF,
        zxingFormat = BarcodeFormat.ITF,
        formatHint = "Even number of digits (minimum 6 digits, carton/supply chain)",
        samplePayload = "05012345678900"
    ),
    PDF417(
        displayName = "PDF417",
        shortTag = "PDF417",
        is2D = true,
        mlKitFormat = Barcode.FORMAT_PDF417,
        zxingFormat = BarcodeFormat.PDF_417,
        formatHint = "2D stacked barcode used on boarding passes and ID cards",
        samplePayload = "BOARDING:AZ-8841|SEAT:14A|GATE:C12"
    ),
    AZTEC(
        displayName = "Aztec Code",
        shortTag = "AZTEC",
        is2D = true,
        mlKitFormat = Barcode.FORMAT_AZTEC,
        zxingFormat = BarcodeFormat.AZTEC,
        formatHint = "High-density 2D matrix used for rail and airline tickets",
        samplePayload = "EUROSTAR-TKT-998123-LON-PAR"
    ),
    DATA_MATRIX(
        displayName = "Data Matrix",
        shortTag = "DMATRIX",
        is2D = true,
        mlKitFormat = Barcode.FORMAT_DATA_MATRIX,
        zxingFormat = BarcodeFormat.DATA_MATRIX,
        formatHint = "Compact 2D matrix for electronics, medical & aerospace parts",
        samplePayload = "SN:DM-2026-884920"
    );

    companion object {
        fun fromMlKitFormat(format: Int): BarcodeFormatType {
            return entries.firstOrNull { it.mlKitFormat == format } ?: QR_CODE
        }

        fun fromZxingFormat(format: BarcodeFormat): BarcodeFormatType {
            return entries.firstOrNull { it.zxingFormat == format } ?: QR_CODE
        }

        fun fromNameOrDefault(name: String): BarcodeFormatType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: QR_CODE
        }
    }
}

enum class ParsedPayloadType(val displayName: String) {
    URL("Website URL"),
    WIFI("Wi-Fi Network"),
    CONTACT_VCARD("Contact Card"),
    EMAIL("Email Message"),
    PHONE("Phone Number"),
    SMS("SMS Text"),
    GEO("Geo Coordinates"),
    CALENDAR("Calendar Event"),
    ISBN("Book (ISBN)"),
    PRODUCT("Retail Product"),
    PLAIN_TEXT("Plain Text")
}

enum class SecurityRiskLevel(val label: String) {
    SAFE("Verified Safe Syntax"),
    CAUTION("Unencrypted / Redirect"),
    SUSPICIOUS("Suspicious Link Warning")
}

data class UrlSecurityReport(
    val riskLevel: SecurityRiskLevel,
    val host: String,
    val scheme: String,
    val warnings: List<String>
)

data class ParsedPayload(
    val type: ParsedPayloadType,
    val rawValue: String,
    val primaryTitle: String,
    val subtitle: String,
    val structuredFields: List<Pair<String, String>> = emptyList(),
    val urlSecurityReport: UrlSecurityReport? = null,
    val productCountryOrigin: String? = null
)

enum class ScanRegionMode(
    val title: String,
    val subtitle: String,
    val widthFraction: Float,
    val heightFraction: Float
) {
    PRECISION_SQUARE("Precision Square", "Balanced 2D & 1D target zone", 0.72f, 0.44f),
    BARCODE_SLIT("1D Laser Slit", "Wide horizontal retail barcode window", 0.86f, 0.24f),
    FULL_FRAME("Full Sensor", "Multi-code wide-angle detection", 0.92f, 0.76f);

    fun toNormalizedRect(): RectF {
        val left = (1f - widthFraction) / 2f
        val top = (1f - heightFraction) / 2f
        return RectF(left, top, left + widthFraction, top + heightFraction)
    }
}

data class DetectedBarcodeFrame(
    val rawValue: String,
    val format: BarcodeFormatType,
    val parsedPayload: ParsedPayload,
    val normalizedBox: RectF?,
    val cornerPoints: List<PointF> = emptyList()
)

enum class QrErrorCorrection(val label: String, val recoveryPercent: String) {
    L("Low (L)", "~7% recovery"),
    M("Medium (M)", "~15% recovery"),
    Q("Quartile (Q)", "~25% recovery"),
    H("High (H)", "~30% recovery")
}

data class QrCustomizationConfig(
    val sizePx: Int = 1024,
    val marginModules: Int = 2,
    val errorCorrection: QrErrorCorrection = QrErrorCorrection.M,
    val foregroundArgb: Int = 0xFF0A0F1D.toInt(),
    val backgroundArgb: Int = 0xFFFFFFFF.toInt(),
    val roundedDots: Boolean = true
)
