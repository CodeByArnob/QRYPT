package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.QrErrorCorrection
import com.example.domain.parser.PayloadParser
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap
import java.util.Locale

sealed class GenerationResult {
    data class Success(
        val bitmap: Bitmap,
        val normalizedPayload: String,
        val format: BarcodeFormatType
    ) : GenerationResult()

    data class ValidationError(val message: String) : GenerationResult()
}

object BarcodeGeneratorEngine {

    fun validateAndNormalize(rawInput: String, format: BarcodeFormatType): Result<String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter content to encode"))
        }

        return when (format) {
            BarcodeFormatType.QR_CODE -> {
                if (trimmed.length > 2953) {
                    Result.failure(IllegalArgumentException("QR payload exceeds maximum capacity (2,953 bytes)"))
                } else {
                    Result.success(trimmed)
                }
            }

            BarcodeFormatType.EAN_13 -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length !in 12..13) {
                    Result.failure(IllegalArgumentException("EAN-13 requires 12 or 13 numeric digits (currently ${digits.length})"))
                } else {
                    val base12 = digits.take(12)
                    val check = PayloadParser.calculateGtinCheckDigit(base12)
                    Result.success("$base12$check")
                }
            }

            BarcodeFormatType.EAN_8 -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length !in 7..8) {
                    Result.failure(IllegalArgumentException("EAN-8 requires 7 or 8 numeric digits (currently ${digits.length})"))
                } else {
                    val base7 = digits.take(7)
                    val check = PayloadParser.calculateGtinCheckDigit(base7)
                    Result.success("$base7$check")
                }
            }

            BarcodeFormatType.UPC_A -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length !in 11..12) {
                    Result.failure(IllegalArgumentException("UPC-A requires 11 or 12 numeric digits (currently ${digits.length})"))
                } else {
                    val base11 = digits.take(11)
                    val check = PayloadParser.calculateGtinCheckDigit(base11)
                    Result.success("$base11$check")
                }
            }

            BarcodeFormatType.UPC_E -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length !in 7..8 || (digits.firstOrNull() != '0' && digits.firstOrNull() != '1')) {
                    Result.failure(IllegalArgumentException("UPC-E requires 7 or 8 digits starting with 0 or 1"))
                } else {
                    val base7 = digits.take(7)
                    val check = PayloadParser.calculateGtinCheckDigit(base7)
                    Result.success("$base7$check")
                }
            }

            BarcodeFormatType.ITF -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length < 6) {
                    Result.failure(IllegalArgumentException("ITF requires at least 6 numeric digits"))
                } else {
                    val evenDigits = if (digits.length % 2 != 0) "0$digits" else digits
                    Result.success(evenDigits)
                }
            }

            BarcodeFormatType.CODABAR -> {
                val upper = trimmed.uppercase(Locale.US)
                val validGuard = setOf('A', 'B', 'C', 'D')
                val hasGuards = upper.length >= 3 && upper.first() in validGuard && upper.last() in validGuard
                val core = if (hasGuards) upper.substring(1, upper.length - 1) else upper
                if (core.isEmpty() || !core.all { it.isDigit() || it in setOf('-', '$', ':', '/', '.', '+') }) {
                    Result.failure(IllegalArgumentException("Codabar accepts digits 0-9 and - $ : / . +"))
                } else {
                    val formatted = if (hasGuards) upper else "A${core}B"
                    Result.success(formatted)
                }
            }

            BarcodeFormatType.CODE_39 -> {
                val upper = trimmed.uppercase(Locale.US)
                val allowed = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
                if (!upper.all { it in allowed } || upper.length > 48) {
                    Result.failure(IllegalArgumentException("Code 39 supports A-Z, 0-9, and - . $ / + % SPACE (max 48 chars)"))
                } else {
                    Result.success(upper)
                }
            }

            BarcodeFormatType.CODE_93 -> {
                val upper = trimmed.uppercase(Locale.US)
                val allowed = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
                if (!upper.all { it in allowed } || upper.length > 48) {
                    Result.failure(IllegalArgumentException("Code 93 supports uppercase ASCII & digits (max 48 chars)"))
                } else {
                    Result.success(upper)
                }
            }

            BarcodeFormatType.CODE_128 -> {
                if (!trimmed.all { it.code in 32..126 } || trimmed.length > 80) {
                    Result.failure(IllegalArgumentException("Code 128 supports standard ASCII characters (max 80 chars)"))
                } else {
                    Result.success(trimmed)
                }
            }

            BarcodeFormatType.PDF417,
            BarcodeFormatType.AZTEC,
            BarcodeFormatType.DATA_MATRIX -> {
                if (!trimmed.all { it.code in 9..126 }) {
                    Result.failure(IllegalArgumentException("${format.displayName} requires standard printable ASCII text"))
                } else {
                    Result.success(trimmed)
                }
            }
        }
    }

    fun generate(
        rawInput: String,
        format: BarcodeFormatType,
        config: QrCustomizationConfig = QrCustomizationConfig()
    ): GenerationResult {
        val validated = validateAndNormalize(rawInput, format)
        val payload = validated.getOrElse { err ->
            return GenerationResult.ValidationError(err.message ?: "Invalid barcode input")
        }

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                if (format == BarcodeFormatType.QR_CODE) {
                    put(EncodeHintType.MARGIN, config.marginModules.coerceIn(2, 6))
                    val ecLevel = when (config.errorCorrection) {
                        QrErrorCorrection.L -> ErrorCorrectionLevel.L
                        QrErrorCorrection.M -> ErrorCorrectionLevel.M
                        QrErrorCorrection.Q -> ErrorCorrectionLevel.Q
                        QrErrorCorrection.H -> ErrorCorrectionLevel.H
                    }
                    put(EncodeHintType.ERROR_CORRECTION, ecLevel)
                } else if (format == BarcodeFormatType.PDF417) {
                    put(EncodeHintType.MARGIN, 30)
                }
            }

            val targetWidth = config.sizePx.coerceIn(256, 2048)
            val targetHeight = if (format.is2D) {
                if (format == BarcodeFormatType.PDF417) (targetWidth * 0.45f).toInt() else targetWidth
            } else {
                (targetWidth * 0.42f).toInt()
            }

            val writer = MultiFormatWriter()
            if (format == BarcodeFormatType.QR_CODE && config.roundedDots) {
                // Encode native matrix size first to render crisp rounded modules
                val rawMatrix = writer.encode(payload, format.zxingFormat, 0, 0, hints)
                val bitmap = renderStyledMatrix(rawMatrix, targetWidth, targetHeight, config)
                GenerationResult.Success(bitmap, payload, format)
            } else {
                val bitMatrix = writer.encode(payload, format.zxingFormat, targetWidth, targetHeight, hints)
                val w = bitMatrix.width
                val h = bitMatrix.height
                val pad = if (format == BarcodeFormatType.QR_CODE) 0 else 24
                val outW = w + pad * 2
                val outH = h + pad * 2
                val pixels = IntArray(outW * outH) { config.backgroundArgb }
                val fg = config.foregroundArgb
                val bg = config.backgroundArgb
                for (y in 0 until h) {
                    val offset = (y + pad) * outW
                    for (x in 0 until w) {
                        pixels[offset + x + pad] = if (bitMatrix.get(x, y)) fg else bg
                    }
                }
                val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, outW, 0, 0, outW, outH)
                GenerationResult.Success(bitmap, payload, format)
            }
        } catch (e: Exception) {
            GenerationResult.ValidationError(
                e.message?.takeIf { it.isNotBlank() }
                    ?: "Unable to encode payload for ${format.displayName}"
            )
        }
    }

    private fun renderStyledMatrix(
        matrix: com.google.zxing.common.BitMatrix,
        widthPx: Int,
        heightPx: Int,
        config: QrCustomizationConfig
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(config.backgroundArgb)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.foregroundArgb
            style = Paint.Style.FILL
        }

        val matrixW = matrix.width.coerceAtLeast(1)
        val matrixH = matrix.height.coerceAtLeast(1)
        val cellSize = widthPx.toFloat() / matrixW.toFloat()
        val cornerRadius = cellSize * 0.34f
        val rect = RectF()

        for (y in 0 until matrixH) {
            for (x in 0 until matrixW) {
                if (matrix.get(x, y)) {
                    val left = x * cellSize
                    val top = y * cellSize
                    rect.set(left, top, left + cellSize + 0.4f, top + cellSize + 0.4f)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                }
            }
        }
        return bitmap
    }
}
