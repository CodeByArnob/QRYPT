package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.data.repository.ScanRepository
import com.example.engine.BarcodeScannerEngine
import com.example.engine.SampleTestBarcode
import com.example.ui.components.FormatBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlass
import com.example.util.ActionIntentHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchScannerScreen(
    batchSessionId: String,
    batchRecords: List<ScanRecordEntity>,
    duplicatePreventionEnabled: Boolean,
    duplicateCooldownSec: Int,
    onStartNewSession: () -> Unit,
    onTriggerSampleBatchScan: (SampleTestBarcode) -> Unit,
    onScanGalleryBatch: (android.net.Uri) -> Unit,
    onInspectRecord: (ScanRecordEntity) -> Unit,
    onDeleteRecord: (ScanRecordEntity) -> Unit,
    onAnnotateBatch: (String) -> Unit,
    onClearBatch: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    var sampleCursor by remember { mutableIntStateOf(0) }
    var showBulkTagDialog by remember { mutableStateOf(false) }
    var bulkTagText by remember { mutableStateOf("") }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onScanGalleryBatch(uri)
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = ScanRepository.exportToCsv(batchRecords)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Saved batch CSV (${batchRecords.size} records)")
            } catch (_: Exception) {
                onShowMessage("Unable to write CSV file")
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = ScanRepository.exportToJson(batchRecords)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Saved batch JSON (${batchRecords.size} records)")
            } catch (_: Exception) {
                onShowMessage("Unable to write JSON file")
            }
        }
    }

    if (showBulkTagDialog) {
        AlertDialog(
            onDismissRequest = { showBulkTagDialog = false },
            title = { Text("Assign Batch Tag / Warehouse Note") },
            text = {
                OutlinedTextField(
                    value = bulkTagText,
                    onValueChange = { bulkTagText = it },
                    label = { Text("Batch Note") },
                    placeholder = { Text("e.g., Audit Q4 - Aisle 3") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAnnotateBatch(bulkTagText)
                        showBulkTagDialog = false
                    }
                ) {
                    Text("Apply to All (${batchRecords.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Batch Session Header Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BATCH SESSION #$batchSessionId",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = glass.primaryAccent
                        )
                        Text(
                            text = "${batchRecords.size} Codes Captured",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (duplicatePreventionEnabled) {
                                "Duplicate guard active (${duplicateCooldownSec}s cooldown)"
                            } else {
                                "Rapid multi-scan unrestricted"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = onStartNewSession,
                        modifier = Modifier.testTag("batch_new_session_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Session")
                    }
                }

                // Rapid Batch Input Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val presets = BarcodeScannerEngine.sampleOpticalTestPresets
                            val nextSample = presets[sampleCursor % presets.size]
                            sampleCursor++
                            onTriggerSampleBatchScan(nextSample)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_rapid_sample_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Sample")
                    }

                    OutlinedButton(
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_gallery_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("From Image")
                    }
                }

                // Bulk Actions Row
                if (batchRecords.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassPill(
                            text = "Export CSV",
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                createCsvLauncher.launch("qrypt_batch_${batchSessionId}.csv")
                            },
                            modifier = Modifier.testTag("batch_export_csv_button")
                        )
                        LiquidGlassPill(
                            text = "Export JSON",
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                createJsonLauncher.launch("qrypt_batch_${batchSessionId}.json")
                            },
                            modifier = Modifier.testTag("batch_export_json_button")
                        )
                        LiquidGlassPill(
                            text = "Share CSV",
                            icon = Icons.Default.AddCircleOutline,
                            onClick = {
                                val csv = ScanRepository.exportToCsv(batchRecords)
                                ActionIntentHelper.shareExportFile(
                                    context = context,
                                    content = csv,
                                    filename = "qrypt_batch_${batchSessionId}.csv",
                                    mimeType = "text/csv"
                                )
                            }
                        )
                        LiquidGlassPill(
                            text = "Tag Batch",
                            icon = Icons.AutoMirrored.Filled.Label,
                            onClick = { showBulkTagDialog = true },
                            modifier = Modifier.testTag("batch_tag_button")
                        )
                        LiquidGlassPill(
                            text = "Clear",
                            icon = Icons.Default.DeleteSweep,
                            onClick = onClearBatch,
                            modifier = Modifier.testTag("batch_clear_button")
                        )
                    }
                }
            }
        }

        // Batch Queue List
        if (batchRecords.isEmpty()) {
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
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = glass.primaryAccent,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Batch Queue Ready",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Capture multiple retail barcodes, shipping labels, or QR codes in rapid succession, then bulk-annotate and export as RFC-4180 CSV or JSON.",
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
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(batchRecords, key = { it.id }) { item ->
                    LiquidGlassCard(
                        onClick = { onInspectRecord(item) },
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FormatBadge(format = item.format)
                                    Text(
                                        text = item.primaryTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.rawValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = glass.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.customNotes.isNotBlank()) {
                                    Text(
                                        text = "Tag: ${item.customNotes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.primaryAccent
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteRecord(item) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Remove Item",
                                    tint = DangerCrimson
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
