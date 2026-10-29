package com.example.data.repository

import com.example.data.local.ScanRecordDao
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.parser.PayloadParser
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ScanRepository(private val dao: ScanRecordDao) {

    val allRecords: Flow<List<ScanRecordEntity>> = dao.observeAllRecords()

    suspend fun recordScan(
        rawValue: String,
        format: BarcodeFormatType,
        isGenerated: Boolean = false,
        batchSessionId: String? = null,
        customNotes: String = "",
        duplicatePreventionEnabled: Boolean = true,
        duplicateCooldownSeconds: Int = 3
    ): ScanRecordEntity? {
        val now = System.currentTimeMillis()
        if (duplicatePreventionEnabled && !isGenerated) {
            val latest = dao.findLatestByRawValue(rawValue)
            if (latest != null && (now - latest.timestamp) < (duplicateCooldownSeconds * 1000L)) {
                return null
            }
        }

        val parsed = PayloadParser.parse(rawValue, format)
        val risk = parsed.urlSecurityReport?.riskLevel ?: SecurityRiskLevel.SAFE
        val entity = ScanRecordEntity(
            rawValue = rawValue,
            primaryTitle = parsed.primaryTitle,
            subtitle = parsed.subtitle,
            formatName = format.name,
            payloadTypeName = parsed.type.name,
            securityRiskName = risk.name,
            isFavorite = false,
            isGenerated = isGenerated,
            batchSessionId = batchSessionId,
            customNotes = customNotes,
            timestamp = now
        )
        val insertedId = dao.insertRecord(entity)
        return entity.copy(id = insertedId)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun updateNotes(id: Long, notes: String) {
        dao.updateCustomNotes(id, notes)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearNonFavorites() {
        dao.deleteNonFavorites()
    }

    suspend fun clearAllHistory() {
        dao.clearAll()
    }

    companion object {
        fun exportToCsv(records: List<ScanRecordEntity>): String {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return buildString {
                appendLine("id,timestamp_utc,format,payload_type,title,raw_value,security_risk,is_favorite,is_generated,batch_session,notes")
                for (r in records) {
                    val dateStr = isoFormat.format(Date(r.timestamp))
                    append(r.id).append(',')
                    append(escapeCsv(dateStr)).append(',')
                    append(escapeCsv(r.format.displayName)).append(',')
                    append(escapeCsv(r.payloadType.displayName)).append(',')
                    append(escapeCsv(r.primaryTitle)).append(',')
                    append(escapeCsv(r.rawValue)).append(',')
                    append(escapeCsv(r.securityRisk.name)).append(',')
                    append(r.isFavorite).append(',')
                    append(r.isGenerated).append(',')
                    append(escapeCsv(r.batchSessionId.orEmpty())).append(',')
                    appendLine(escapeCsv(r.customNotes))
                }
            }
        }

        fun exportToJson(records: List<ScanRecordEntity>): String {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return buildString {
                appendLine("{")
                appendLine("  \"application\": \"QRYPT\",")
                appendLine("  \"developer\": \"Arnobrazz\",")
                appendLine("  \"version\": \"1.0.0\",")
                appendLine("  \"exported_count\": ${records.size},")
                appendLine("  \"records\": [")
                records.forEachIndexed { index, r ->
                    val comma = if (index < records.size - 1) "," else ""
                    val batchVal = if (r.batchSessionId != null) "\"${escapeJson(r.batchSessionId)}\"" else "null"
                    appendLine("    {")
                    appendLine("      \"id\": ${r.id},")
                    appendLine("      \"timestamp_utc\": \"${isoFormat.format(Date(r.timestamp))}\",")
                    appendLine("      \"timestamp_epoch_ms\": ${r.timestamp},")
                    appendLine("      \"format\": \"${escapeJson(r.format.name)}\",")
                    appendLine("      \"format_display\": \"${escapeJson(r.format.displayName)}\",")
                    appendLine("      \"payload_type\": \"${escapeJson(r.payloadType.name)}\",")
                    appendLine("      \"title\": \"${escapeJson(r.primaryTitle)}\",")
                    appendLine("      \"subtitle\": \"${escapeJson(r.subtitle)}\",")
                    appendLine("      \"raw_value\": \"${escapeJson(r.rawValue)}\",")
                    appendLine("      \"security_risk\": \"${escapeJson(r.securityRisk.name)}\",")
                    appendLine("      \"is_favorite\": ${r.isFavorite},")
                    appendLine("      \"is_generated\": ${r.isGenerated},")
                    appendLine("      \"batch_session_id\": $batchVal,")
                    appendLine("      \"notes\": \"${escapeJson(r.customNotes)}\"")
                    appendLine("    }$comma")
                }
                appendLine("  ]")
                append("}")
            }
        }

        private fun escapeJson(value: String): String {
            return buildString(value.length) {
                for (ch in value) {
                    when (ch) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(ch)
                    }
                }
            }
        }

        private fun escapeCsv(value: String): String {
            val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
            return if (needsQuotes) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }
    }
}
