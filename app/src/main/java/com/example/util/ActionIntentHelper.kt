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
import java.io.File
import java.io.FileOutputStream

object ActionIntentHelper {

    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share Barcode Payload") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
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

    fun openUrlSafely(context: Context, rawUrl: String) {
        try {
            val normalized = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) {
                rawUrl.trim()
            } else {
                "https://${rawUrl.trim()}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun dialPhone(context: Context, phone: String) {
        try {
            val clean = phone.removePrefix("tel:").removePrefix("TEL:").trim()
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun sendSms(context: Context, parsed: ParsedPayload) {
        try {
            val map = parsed.structuredFields.toMap()
            val phone = map["Phone Number"].orEmpty()
            val body = map["Message Body"].orEmpty()
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun sendEmail(context: Context, parsed: ParsedPayload) {
        try {
            val map = parsed.structuredFields.toMap()
            val recipient = map["Recipient"].orEmpty()
            val subject = map["Subject"].orEmpty()
            val body = map["Message"] ?: map["Body"].orEmpty()
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openGeoLocation(context: Context, rawGeo: String) {
        try {
            val uri = if (rawGeo.startsWith("geo:", true)) Uri.parse(rawGeo) else Uri.parse("geo:0,0?q=$rawGeo")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun insertContact(context: Context, parsed: ParsedPayload) {
        try {
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
        } catch (_: Exception) {
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

    fun insertCalendarEvent(context: Context, parsed: ParsedPayload) {
        try {
            val map = parsed.structuredFields.toMap()
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, map["Event Title"] ?: parsed.primaryTitle)
                putExtra(CalendarContract.Events.EVENT_LOCATION, map["Location"].orEmpty())
                putExtra(CalendarContract.Events.DESCRIPTION, map["Details"].orEmpty())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
