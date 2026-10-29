package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.SecurityRiskLevel
import com.example.engine.BarcodeGeneratorEngine
import com.example.engine.GenerationResult
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlass
import com.example.util.ActionIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanResultBottomSheet(
    record: ScanRecordEntity,
    onDismiss: () -> Unit,
    onToggleFavorite: (ScanRecordEntity) -> Unit,
    onUpdateNotes: (ScanRecordEntity, String) -> Unit,
    onDelete: (ScanRecordEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current
    val parsed = remember(record.rawValue, record.formatName) { record.toParsedPayload() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showSuspiciousUrlWarning by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }
    var notesDraft by remember(record.customNotes) { mutableStateOf(record.customNotes) }

    val previewBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, record.rawValue, record.formatName) {
        value = withContext(Dispatchers.Default) {
            val res = BarcodeGeneratorEngine.generate(
                rawInput = record.rawValue,
                format = record.format,
                config = QrCustomizationConfig(sizePx = 512, marginModules = 2)
            )
            (res as? GenerationResult.Success)?.bitmap
        }
    }

    val dateFormatted = remember(record.timestamp) {
        SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.US).format(Date(record.timestamp))
    }

    if (showSuspiciousUrlWarning) {
        val security = parsed.urlSecurityReport
        AlertDialog(
            onDismissRequest = { showSuspiciousUrlWarning = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Security Warning",
                    tint = DangerCrimson,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Suspicious Link Warning",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QRYPT's offline URL heuristic engine flagged potential risks with this link:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    security?.warnings?.forEach { warn ->
                        Text(
                            text = "• $warn",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Destination: ${record.rawValue}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuspiciousUrlWarning = false
                        ActionIntentHelper.openUrlSafely(context, record.rawValue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                ) {
                    Text("Proceed Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuspiciousUrlWarning = false }) {
                    Text("Cancel (Safe)")
                }
            }
        )
    }

    if (showEditNotesDialog) {
        AlertDialog(
            onDismissRequest = { showEditNotesDialog = false },
            title = { Text("Custom Vault Annotation") },
            text = {
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { notesDraft = it },
                    label = { Text("Notes or inventory tag") },
                    placeholder = { Text("e.g., Shelf B-14, Verified Invoice, Office Guest Wi-Fi") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNotes(record, notesDraft.trim())
                        showEditNotesDialog = false
                        onShowMessage("Annotation saved to Vault")
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNotesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        ScanResultSheetContent(
            record = record,
            parsed = parsed,
            previewBitmap = previewBitmap,
            dateFormatted = dateFormatted,
            onDismiss = onDismiss,
            onToggleFavorite = onToggleFavorite,
            onOpenSuspiciousUrlDialog = { showSuspiciousUrlWarning = true },
            onOpenEditNotesDialog = { showEditNotesDialog = true },
            onDelete = onDelete,
            onShowMessage = onShowMessage
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultSheetContent(
    record: ScanRecordEntity,
    parsed: com.example.domain.model.ParsedPayload = remember(record.rawValue, record.formatName) { record.toParsedPayload() },
    previewBitmap: android.graphics.Bitmap? = null,
    dateFormatted: String = remember(record.timestamp) {
        SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.US).format(Date(record.timestamp))
    },
    onDismiss: () -> Unit = {},
    onToggleFavorite: (ScanRecordEntity) -> Unit = {},
    onOpenSuspiciousUrlDialog: () -> Unit = {},
    onOpenEditNotesDialog: () -> Unit = {},
    onDelete: (ScanRecordEntity) -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(glass.primaryAccent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForPayloadType(parsed.type),
                        contentDescription = parsed.type.displayName,
                        tint = glass.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormatBadge(format = record.format)
                        Text(
                            text = parsed.type.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parsed.primaryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                IconButton(
                    onClick = { onToggleFavorite(record) },
                    modifier = Modifier.testTag("sheet_favorite_button")
                ) {
                    Icon(
                        imageVector = if (record.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (record.isFavorite) glass.primaryAccent else glass.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Details"
                    )
                }
            }

            // URL Security Banner if URL
            parsed.urlSecurityReport?.let { security ->
                val bannerColor = when (security.riskLevel) {
                    SecurityRiskLevel.SAFE -> glass.primaryAccent
                    SecurityRiskLevel.CAUTION -> CautionAmber
                    SecurityRiskLevel.SUSPICIOUS -> DangerCrimson
                }
                LiquidGlassCard(
                    accentTint = bannerColor,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (security.riskLevel == SecurityRiskLevel.SAFE) {
                                    Icons.Default.Shield
                                } else {
                                    Icons.Default.WarningAmber
                                },
                                contentDescription = security.riskLevel.label,
                                tint = bannerColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = security.riskLevel.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = bannerColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (security.warnings.isEmpty()) {
                            Text(
                                text = "Encrypted HTTPS domain with standard ASCII syntax and no known redirect obfuscation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        } else {
                            security.warnings.forEach { warn ->
                                Text(
                                    text = "• $warn",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Context-Aware Primary Smart Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (parsed.type) {
                    ParsedPayloadType.URL -> {
                        val isRisky = parsed.urlSecurityReport?.riskLevel != SecurityRiskLevel.SAFE
                        Button(
                            onClick = {
                                if (isRisky) {
                                    onOpenSuspiciousUrlDialog()
                                } else {
                                    ActionIntentHelper.openUrlSafely(context, record.rawValue)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sheet_open_url_button"),
                            colors = if (isRisky) {
                                ButtonDefaults.buttonColors(containerColor = CautionAmber, contentColor = Color.Black)
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRisky) "Inspect & Open Link" else "Open Website Safely")
                        }
                    }

                    ParsedPayloadType.WIFI -> {
                        Button(
                            onClick = {
                                val msg = ActionIntentHelper.connectWifi(context, parsed)
                                onShowMessage(msg)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Password & Wi-Fi Settings")
                        }
                    }

                    ParsedPayloadType.CONTACT_VCARD -> {
                        Button(
                            onClick = { ActionIntentHelper.insertContact(context, parsed) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Contacts")
                        }
                    }

                    ParsedPayloadType.PHONE -> {
                        Button(
                            onClick = { ActionIntentHelper.dialPhone(context, record.rawValue) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dial Number")
                        }
                    }

                    ParsedPayloadType.SMS -> {
                        Button(
                            onClick = { ActionIntentHelper.sendSms(context, parsed) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compose SMS")
                        }
                    }

                    ParsedPayloadType.EMAIL -> {
                        Button(
                            onClick = { ActionIntentHelper.sendEmail(context, parsed) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compose Email")
                        }
                    }

                    ParsedPayloadType.GEO -> {
                        Button(
                            onClick = { ActionIntentHelper.openGeoLocation(context, record.rawValue) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Coordinates in Maps")
                        }
                    }

                    ParsedPayloadType.CALENDAR -> {
                        Button(
                            onClick = { ActionIntentHelper.insertCalendarEvent(context, parsed) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Calendar")
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                ActionIntentHelper.copyToClipboard(context, parsed.primaryTitle, record.rawValue)
                                onShowMessage("Payload copied to clipboard")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Payload")
                        }
                    }
                }
            }

            // Secondary Action Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        ActionIntentHelper.copyToClipboard(context, parsed.primaryTitle, record.rawValue)
                        onShowMessage("Copied to clipboard")
                    },
                    modifier = Modifier.testTag("sheet_copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Raw")
                }

                OutlinedButton(
                    onClick = { ActionIntentHelper.shareText(context, record.rawValue) }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Text")
                }

                if (previewBitmap != null) {
                    OutlinedButton(
                        onClick = {
                            previewBitmap?.let { bmp ->
                                ActionIntentHelper.shareBitmapPng(context, bmp, "qrypt_${record.format.shortTag}")
                            }
                        }
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Image")
                    }
                }

                OutlinedButton(
                    onClick = { onOpenEditNotesDialog() }
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (record.customNotes.isBlank()) "Add Note" else "Edit Note")
                }

                OutlinedButton(
                    onClick = {
                        onDelete(record)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerCrimson)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }

            // Structured Metadata Breakdown
            if (parsed.structuredFields.isNotEmpty() || record.customNotes.isNotBlank()) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "STRUCTURED METADATA",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent
                        )
                        parsed.structuredFields.forEachIndexed { idx, (label, value) ->
                            if (idx > 0) {
                                HorizontalDivider(color = glass.glassSpecularBorderBottom)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    modifier = Modifier.weight(0.38f)
                                )
                                SelectionContainer(modifier = Modifier.weight(0.62f)) {
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = glass.textPrimary
                                    )
                                }
                            }
                        }
                        if (record.customNotes.isNotBlank()) {
                            HorizontalDivider(color = glass.glassSpecularBorderBottom)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Vault Note",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.primaryAccent,
                                    modifier = Modifier.weight(0.38f)
                                )
                                Text(
                                    text = record.customNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textPrimary,
                                    modifier = Modifier.weight(0.62f)
                                )
                            }
                        }
                    }
                }
            }

            // Raw Payload Monospace Box + Regenerated Code Preview
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RAW PAYLOAD (${record.rawValue.length} BYTES)",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Text(
                            text = record.format.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent
                        )
                    }
                    SelectionContainer {
                        Text(
                            text = record.rawValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = glass.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                                .padding(12.dp)
                        )
                    }

                    previewBitmap?.let { bmp ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Regenerated ${record.format.displayName}",
                                modifier = Modifier
                                    .size(if (record.format.is2D) 168.dp else 220.dp, if (record.format.is2D) 168.dp else 96.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
}
