package com.example.engine

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.DetectedBarcodeFrame
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.ScanRegionMode
import com.example.domain.parser.PayloadParser
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.EnumMap
import kotlin.coroutines.resume

data class SampleTestBarcode(
    val id: String,
    val title: String,
    val subtitle: String,
    val format: BarcodeFormatType,
    val payload: String
)

class BarcodeScannerEngine(
    private val onBarcodesDetected: (List<DetectedBarcodeFrame>) -> Unit,
    private val onLuminanceUpdated: (Float) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    var scanRegionMode: ScanRegionMode = ScanRegionMode.PRECISION_SQUARE

    @Volatile
    var isScanningPaused: Boolean = false

    private val mlKitScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAllPotentialBarcodes()
            .build()
        BarcodeScanning.getClient(options)
    }

    private var lastLumaCalculationMs = 0L

    fun close() {
        try {
            mlKitScanner.close()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isScanningPaused) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastLumaCalculationMs > 350L) {
            lastLumaCalculationMs = now
            val yBuffer = imageProxy.planes.firstOrNull()?.buffer
            if (yBuffer != null) {
                val avgLuma = estimateAverageLuma(yBuffer)
                onLuminanceUpdated(avgLuma)
            }
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val frameWidth = (if (isRotated) imageProxy.height else imageProxy.width).toFloat().coerceAtLeast(1f)
        val frameHeight = (if (isRotated) imageProxy.width else imageProxy.height).toFloat().coerceAtLeast(1f)
        val activeRoi = scanRegionMode.toNormalizedRect()

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        mlKitScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val validFrames = barcodes.mapNotNull { barcode ->
                    val raw = barcode.rawValue?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    val box = barcode.boundingBox
                    val normalizedRect = if (box != null) {
                        RectF(
                            (box.left / frameWidth).coerceIn(0f, 1f),
                            (box.top / frameHeight).coerceIn(0f, 1f),
                            (box.right / frameWidth).coerceIn(0f, 1f),
                            (box.bottom / frameHeight).coerceIn(0f, 1f)
                        )
                    } else null

                    // Filter by active scan region if bounding box is available
                    if (normalizedRect != null && scanRegionMode != ScanRegionMode.FULL_FRAME) {
                        val centerX = normalizedRect.centerX()
                        val centerY = normalizedRect.centerY()
                        if (!activeRoi.contains(centerX, centerY)) {
                            return@mapNotNull null
                        }
                    }

                    val format = BarcodeFormatType.fromMlKitFormat(barcode.format)
                    val parsed = PayloadParser.parse(raw, format)
                    val corners = barcode.cornerPoints?.map { pt ->
                        PointF(
                            (pt.x / frameWidth).coerceIn(0f, 1f),
                            (pt.y / frameHeight).coerceIn(0f, 1f)
                        )
                    }.orEmpty()

                    DetectedBarcodeFrame(
                        rawValue = raw,
                        format = format,
                        parsedPayload = parsed,
                        normalizedBox = normalizedRect,
                        cornerPoints = corners
                    )
                }
                onBarcodesDetected(validFrames)
            }
            .addOnFailureListener {
                // Frame drop or transient error; keep camera stream running smoothly
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun estimateAverageLuma(buffer: ByteBuffer): Float {
        val duplicate = buffer.duplicate()
        duplicate.rewind()
        val limit = duplicate.remaining()
        if (limit <= 0) return 128f
        val step = (limit / 256).coerceAtLeast(1)
        var sum = 0L
        var count = 0
        var idx = 0
        while (idx < limit) {
            sum += (duplicate.get(idx).toInt() and 0xFF)
            count++
            idx += step
        }
        return if (count > 0) sum.toFloat() / count else 128f
    }

    companion object {
        private val zxingHints: Map<DecodeHintType, Any> by lazy {
            EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
                put(DecodeHintType.ALSO_INVERTED, java.lang.Boolean.TRUE)
                put(DecodeHintType.CHARACTER_SET, "UTF-8")
                put(
                    DecodeHintType.POSSIBLE_FORMATS,
                    BarcodeFormatType.entries.map { it.zxingFormat }
                )
            }
        }

        /**
         * Decodes one or more barcodes from a static Bitmap using dual-engine (ZXing + ML Kit).
         * Works 100% offline in both Android runtime and JVM/Robolectric tests.
         */
        suspend fun decodeBitmap(bitmap: Bitmap): List<DetectedBarcodeFrame> {
            val zxingResults = decodeWithZxing(bitmap)
            if (zxingResults.isNotEmpty()) {
                return zxingResults
            }
            return decodeWithMlKit(bitmap)
        }

        fun decodeWithZxing(bitmap: Bitmap): List<DetectedBarcodeFrame> {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return emptyList()

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)

            val rawResults = mutableListOf<Result>()

            // 1. Primary single-code HybridBinarizer
            try {
                val reader = MultiFormatReader().apply { setHints(zxingHints) }
                rawResults.add(reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))))
            } catch (_: NotFoundException) {
            } catch (_: Exception) {
            }

            // 2. Fallback to GlobalHistogramBinarizer (ideal for 1D linear & stacked barcodes)
            if (rawResults.isEmpty()) {
                try {
                    val reader = MultiFormatReader().apply { setHints(zxingHints) }
                    rawResults.add(reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source))))
                } catch (_: NotFoundException) {
                } catch (_: Exception) {
                }
            }

            // 3. Multi-code reader pass to pick up any additional codes in frame
            try {
                val reader = MultiFormatReader().apply { setHints(zxingHints) }
                val multiReader = GenericMultipleBarcodeReader(reader)
                val multi = multiReader.decodeMultiple(BinaryBitmap(HybridBinarizer(source)), zxingHints)
                if (multi != null) rawResults.addAll(multi)
            } catch (_: NotFoundException) {
            } catch (_: Exception) {
            }

            return rawResults
                .distinctBy { it.text }
                .mapNotNull { res ->
                    val text = res.text?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    val format = BarcodeFormatType.fromZxingFormat(res.barcodeFormat)
                    val parsed = PayloadParser.parse(text, format)
                    val pts = res.resultPoints?.filterNotNull()?.map { pt ->
                        PointF(
                            (pt.x / width.toFloat()).coerceIn(0f, 1f),
                            (pt.y / height.toFloat()).coerceIn(0f, 1f)
                        )
                    }.orEmpty()
                    DetectedBarcodeFrame(
                        rawValue = text,
                        format = format,
                        parsedPayload = parsed,
                        normalizedBox = RectF(0.15f, 0.15f, 0.85f, 0.85f),
                        cornerPoints = pts
                    )
                }
        }

        private suspend fun decodeWithMlKit(bitmap: Bitmap): List<DetectedBarcodeFrame> =
            suspendCancellableCoroutine { cont ->
                try {
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .enableAllPotentialBarcodes()
                        .build()
                    val scanner = BarcodeScanning.getClient(options)
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val frames = barcodes.mapNotNull { b ->
                                val raw = b.rawValue?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                                val format = BarcodeFormatType.fromMlKitFormat(b.format)
                                DetectedBarcodeFrame(
                                    rawValue = raw,
                                    format = format,
                                    parsedPayload = PayloadParser.parse(raw, format),
                                    normalizedBox = null
                                )
                            }
                            if (cont.isActive) cont.resume(frames)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(emptyList())
                }
            }

        val sampleOpticalTestPresets: List<SampleTestBarcode> = listOf(
            SampleTestBarcode(
                id = "wifi_hq",
                title = "Studio Fiber 6E Wi-Fi",
                subtitle = "QR Code | WPA3 Enterprise Network",
                format = BarcodeFormatType.QR_CODE,
                payload = "WIFI:T:WPA;S:AetherGlass-5G-Ultra;P:Prism#2026!Optic;H:false;;"
            ),
            SampleTestBarcode(
                id = "vcard_exec",
                title = "Executive Business Card",
                subtitle = "QR Code | vCard 3.0 Contact",
                format = BarcodeFormatType.QR_CODE,
                payload = PayloadParser.buildVCardPayload(
                    fullName = "Elena Vance",
                    organization = "Aether Optics Lab",
                    jobTitle = "Principal Systems Architect",
                    phone = "+1-415-890-4412",
                    email = "elena.vance@aetheroptics.io",
                    website = "https://aetheroptics.io",
                    address = "450 Mission St, San Francisco, CA"
                )
            ),
            SampleTestBarcode(
                id = "ean13_coffee",
                title = "Retail Product Barcode",
                subtitle = "EAN-13 | Open Food Facts Product Lookup",
                format = BarcodeFormatType.EAN_13,
                payload = "3017620422003"
            ),
            SampleTestBarcode(
                id = "isbn_book",
                title = "Clean Architecture Handbook",
                subtitle = "EAN-13 / ISBN-13 | Bookland Publication",
                format = BarcodeFormatType.EAN_13,
                payload = "9780134494166"
            ),
            SampleTestBarcode(
                id = "code128_shipment",
                title = "Air Freight Tracking",
                subtitle = "Code 128 | Supply Chain Container",
                format = BarcodeFormatType.CODE_128,
                payload = "AWB-94820-SFO-TYO"
            ),
            SampleTestBarcode(
                id = "suspicious_url",
                title = "Suspicious Phishing Test Link",
                subtitle = "QR Code | Punycode & Unencrypted HTTP",
                format = BarcodeFormatType.QR_CODE,
                payload = "http://secure-login@xn--pple-43d.top/verify-session"
            )
        )

        fun renderSampleBitmap(sample: SampleTestBarcode): Bitmap? {
            val config = QrCustomizationConfig(
                sizePx = 640,
                marginModules = 2,
                roundedDots = false // crisp standard modules for instantaneous optical test
            )
            return when (val res = BarcodeGeneratorEngine.generate(sample.payload, sample.format, config)) {
                is GenerationResult.Success -> res.bitmap
                else -> null
            }
        }
    }
}
