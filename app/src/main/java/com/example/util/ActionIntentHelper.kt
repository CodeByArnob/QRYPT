package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.domain.model.ParsedPayload
import com.example.domain.parser.PayloadParser
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

sealed interface UrlValidationResult {
    data class Valid(val normalizedUrl: String, val host: String, val scheme: String) : UrlValidationResult
    data class Rejected(val reason: String) : UrlValidationResult
}

object ActionIntentHelper {

    fun validateExternalHttpUrl(rawUrl: String): UrlValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Rejected("URL cannot be empty.")
        }
        if (trimmed.contains(" ") || trimmed.contains("\n") || trimmed.contains("\r")) {
            return UrlValidationResult.Rejected("Malformed URL containing whitespace.")
        }
        if (PayloadParser.hasUnsafeUriScheme(trimmed)) {
            val badScheme = trimmed.substringBefore(":").lowercase(Locale.US)
            return UrlValidationResult.Rejected("Unsafe URI scheme '$badScheme:' is blocked. Only http and https links are allowed.")
        }

        val lower = trimmed.lowercase(Locale.US)
        val normalized = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> trimmed
            PayloadParser.isSafeWebUrl(trimmed) -> "https://$trimmed"
            else -> {
                if (lower.contains(":")) {
                    val scheme = lower.substringBefore(":")
                    return UrlValidationResult.Rejected("Unsupported URI scheme '$scheme:'. Only http and https links are allowed.")
                }
                return UrlValidationResult.Rejected("Invalid web URL syntax.")
            }
        }

        return try {
            val uri = URI(normalized)
            val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
            val host = uri.host?.trim().orEmpty()
            if (scheme != "http" && scheme != "https") {
                UrlValidationResult.Rejected("Only http and https links are allowed.")
            } else if (host.isEmpty()) {
                UrlValidationResult.Rejected("URL is missing a valid destination host.")
            } else {
                UrlValidationResult.Valid(
                    normalizedUrl = normalized,
                    host = host,
                    scheme = scheme
                )
            }
        } catch (_: Exception) {
            UrlValidationResult.Rejected("Malformed URL syntax.")
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share Barcode Payload"): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareBitmapPng(context: Context, bitmap: Bitmap, filenamePrefix: String = "prism_code") {
        try {
            val cacheDir = File(context.cacheDir, "codes").apply { mkdirs() }
            val outFile = File(cacheDir, "${filenamePrefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Barcode Image").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    fun shareExportFile(
        context: Context,
        content: String,
        filename: String,
        mimeType: String
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, filename)
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Export Scan Vault").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    fun openUrlSafely(context: Context, rawUrl: String): Boolean {
        val validation = validateExternalHttpUrl(rawUrl)
        if (validation !is UrlValidationResult.Valid) {
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validation.normalizedUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun buildSearchOnlineUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return "https://www.google.com/search?q=$encoded"
    }

    fun searchOnline(context: Context, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return false
        val searchUrl = buildSearchOnlineUrl(trimmed)
        return openUrlSafely(context, searchUrl)
    }

    fun dialPhone(context: Context, phone: String): Boolean {
        return try {
            val clean = phone.removePrefix("tel:").removePrefix("TEL:").trim()
            if (clean.isEmpty()) return false
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sendSms(context: Context, parsed: ParsedPayload): Boolean {
        return try {
            val map = parsed.structuredFields.toMap()
            val phone = map["Phone Number"].orEmpty().ifEmpty { parsed.primaryTitle }
            val body = map["Message Body"].orEmpty()
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sendEmail(context: Context, parsed: ParsedPayload): Boolean {
        return try {
            val map = parsed.structuredFields.toMap()
            val recipient = map["Recipient"].orEmpty().ifEmpty { parsed.primaryTitle }
            val subject = map["Subject"].orEmpty()
            val body = map["Message"] ?: map["Body"].orEmpty()
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openGeoLocation(context: Context, rawGeo: String): Boolean {
        return try {
            val uri = if (rawGeo.startsWith("geo:", true)) Uri.parse(rawGeo) else Uri.parse("geo:0,0?q=$rawGeo")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun insertContact(context: Context, parsed: ParsedPayload): Boolean {
        return try {
            val map = parsed.structuredFields.toMap()
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, map["Name"] ?: map["Full Name"] ?: parsed.primaryTitle)
                putExtra(ContactsContract.Intents.Insert.COMPANY, map["Organization"].orEmpty())
                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, map["Role / Title"].orEmpty())
                putExtra(ContactsContract.Intents.Insert.PHONE, map["Phone"].orEmpty())
                putExtra(ContactsContract.Intents.Insert.EMAIL, map["Email"].orEmpty())
                putExtra(ContactsContract.Intents.Insert.POSTAL, map["Address"].orEmpty())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun connectWifi(context: Context, parsed: ParsedPayload): String {
        val map = parsed.structuredFields.toMap()
        val ssid = map["Network Name (SSID)"] ?: parsed.primaryTitle
        val password = map["Password"].orEmpty()
        if (password.isNotEmpty()) {
            copyToClipboard(context, "Wi-Fi Password ($ssid)", password)
        }
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
        return if (password.isNotEmpty()) {
            "Password for '$ssid' copied to clipboard"
        } else {
            "Opening Wi-Fi settings for '$ssid'"
        }
    }

    fun insertCalendarEvent(context: Context, parsed: ParsedPayload): Boolean {
        return try {
            val map = parsed.structuredFields.toMap()
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, map["Event Title"] ?: parsed.primaryTitle)
                putExtra(CalendarContract.Events.EVENT_LOCATION, map["Location"].orEmpty())
                putExtra(CalendarContract.Events.DESCRIPTION, map["Details"].orEmpty())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
