package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.data.repository.ScanRepository
import com.example.domain.model.SecurityRiskLevel
import com.example.ui.components.FormatBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.SecurityRiskBadge
import com.example.ui.components.iconForPayloadType
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.VaultFilter
import com.example.util.ActionIntentHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryVaultScreen(
    allRecordsCount: Int,
    filteredRecords: List<ScanRecordEntity>,
    searchQuery: String,
    selectedFilter: VaultFilter,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (VaultFilter) -> Unit,
    onInspectRecord: (ScanRecordEntity) -> Unit,
    onToggleFavorite: (ScanRecordEntity) -> Unit,
    onDeleteRecord: (ScanRecordEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = ScanRepository.exportToCsv(filteredRecords)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Exported ${filteredRecords.size} records to CSV")
            } catch (_: Exception) {
                onShowMessage("CSV export failed")
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = ScanRepository.exportToJson(filteredRecords)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Exported ${filteredRecords.size} records to JSON")
            } catch (_: Exception) {
                onShowMessage("JSON export failed")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vault Header & Export Bar
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LOCAL OPTICAL VAULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent,
                            maxLines = 1
                        )
                        Text(
                            text = "${filteredRecords.size} of $allRecordsCount Records",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    if (filteredRecords.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LiquidGlassPill(
                                text = "CSV",
                                icon = Icons.Default.FileDownload,
                                onClick = {
                                    exportCsvLauncher.launch("qrypt_vault_export.csv")
                                },
                                modifier = Modifier.testTag("vault_export_csv_button")
                            )
                            LiquidGlassPill(
                                text = "JSON",
                                icon = Icons.Default.FileDownload,
                                onClick = {
                                    exportJsonLauncher.launch("qrypt_vault_export.json")
                                },
                                modifier = Modifier.testTag("vault_export_json_button")
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search payload, GTIN, SSID, format, or note…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = glass.primaryAccent)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultFilter.entries.forEach { filter ->
                        LiquidGlassPill(
                            text = filter.label,
                            selected = selectedFilter == filter,
                            onClick = { onSelectFilter(filter) },
                            modifier = Modifier.testTag("vault_filter_${filter.name}")
                        )
                    }
                }
            }
        }

        // Records List or Empty State
        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = glass.primaryAccent,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = if (allRecordsCount == 0) "Your Optical Vault is Empty" else "No Matching Records",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (allRecordsCount == 0) {
                                "Scanned and generated QR codes & barcodes are stored 100% offline in encrypted-ready local SQLite storage with instant search and CSV/JSON export."
                            } else {
                                "Try clearing your search query or switching the active category filter."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("vault_records_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    VaultRecordCard(
                        record = record,
                        onClick = { onInspectRecord(record) },
                        onToggleFavorite = { onToggleFavorite(record) },
                        onCopy = {
                            ActionIntentHelper.copyToClipboard(context, record.primaryTitle, record.rawValue)
                            onShowMessage("Copied to clipboard")
                        },
                        onDelete = { onDeleteRecord(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultRecordCard(
    record: ScanRecordEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = LocalLiquidGlass.current
    val timeFormatted = remember(record.timestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(record.timestamp))
    }
    val sourceLabel = if (record.isGenerated) "Studio" else "Scanned"

    LiquidGlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_record_card_${record.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(glass.primaryAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForPayloadType(record.payloadType),
                        contentDescription = record.payloadType.displayName,
                        tint = glass.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FormatBadge(format = record.format)
                        Text(
                            text = "$sourceLabel • $timeFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = record.primaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.rawValue,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("vault_fav_${record.id}")
                    ) {
                        Icon(
                            imageVector = if (record.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (record.isFavorite) glass.primaryAccent else glass.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = glass.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = glass.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (record.securityRisk != SecurityRiskLevel.SAFE || record.customNotes.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (record.securityRisk != SecurityRiskLevel.SAFE) {
                        SecurityRiskBadge(riskLevel = record.securityRisk)
                    }
                    if (record.customNotes.isNotBlank()) {
                        Text(
                            text = "Note: ${record.customNotes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}
