package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.ParsedPayload
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.parser.PayloadParser

@Entity(
    tableName = "scan_records",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isFavorite"]),
        Index(value = ["rawValue"])
    ]
)
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawValue: String,
    val primaryTitle: String,
    val subtitle: String,
    val formatName: String,
    val payloadTypeName: String,
    val securityRiskName: String,
    val isFavorite: Boolean = false,
    val isGenerated: Boolean = false,
    val batchSessionId: String? = null,
    val customNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val format: BarcodeFormatType
        get() = BarcodeFormatType.fromNameOrDefault(formatName)

    val payloadType: ParsedPayloadType
        get() = ParsedPayloadType.entries.firstOrNull { it.name == payloadTypeName }
            ?: ParsedPayloadType.PLAIN_TEXT

    val securityRisk: SecurityRiskLevel
        get() = SecurityRiskLevel.entries.firstOrNull { it.name == securityRiskName }
            ?: SecurityRiskLevel.SAFE

    fun toParsedPayload(): ParsedPayload {
        return PayloadParser.parse(rawValue, format)
    }
}
