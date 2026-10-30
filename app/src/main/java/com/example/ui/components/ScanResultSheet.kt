package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.BarcodeValidator
import com.example.domain.model.ParsedPayload
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.ProductInfo
import com.example.domain.model.ProductLookupState
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.SecurityRiskLevel
import com.example.engine.BarcodeGeneratorEngine
import com.example.engine.GenerationResult
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.SafeEmerald
import com.example.util.ActionIntentHelper
import com.example.util.UrlValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingExternalAction(
    val title: String,
    val description: String,
    val destination: String,
    val isSuspicious: Boolean = false,
    val warnings: List<String> = emptyList(),
    val confirmLabel: String = "Confirm & Open",
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    record: ScanRecordEntity,
    productLookupState: ProductLookupState = ProductLookupState.Idle,
    onDismiss: () -> Unit,
    onToggleFavorite: (ScanRecordEntity) -> Unit,
    onUpdateNotes: (ScanRecordEntity, String) -> Unit,
    onDelete: (ScanRecordEntity) -> Unit,
    onRetryProductLookup: (ScanRecordEntity, Boolean) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit
) {
    val parsed = remember(record.rawValue, record.formatName) { record.toParsedPayload() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        SimpleDateFormat("MMM d, yyyy | HH:mm:ss", Locale.US).format(Date(record.timestamp))
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
            productLookupState = productLookupState,
            previewBitmap = previewBitmap,
            dateFormatted = dateFormatted,
            onDismiss = onDismiss,
            onToggleFavorite = onToggleFavorite,
            onUpdateNotes = onUpdateNotes,
            onDelete = onDelete,
            onRetryProductLookup = onRetryProductLookup,
            onShowMessage = onShowMessage
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultSheetContent(
    record: ScanRecordEntity,
    parsed: ParsedPayload = remember(record.rawValue, record.formatName) { record.toParsedPayload() },
    productLookupState: ProductLookupState = ProductLookupState.Idle,
    previewBitmap: android.graphics.Bitmap? = null,
    dateFormatted: String = remember(record.timestamp) {
        SimpleDateFormat("MMM d, yyyy | HH:mm:ss", Locale.US).format(Date(record.timestamp))
    },
    onDismiss: () -> Unit = {},
    onToggleFavorite: (ScanRecordEntity) -> Unit = {},
    onOpenSuspiciousUrlDialog: () -> Unit = {},
    onOpenEditNotesDialog: () -> Unit = {},
    onUpdateNotes: (ScanRecordEntity, String) -> Unit = { _, _ -> },
    onDelete: (ScanRecordEntity) -> Unit = {},
    onRetryProductLookup: (ScanRecordEntity, Boolean) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    var pendingExternalAction by remember { mutableStateOf<PendingExternalAction?>(null) }
    var showProductDetailsDialog by remember { mutableStateOf(false) }
    var showEditNotesModal by remember { mutableStateOf(false) }
    var notesDraft by remember(record.customNotes) { mutableStateOf(record.customNotes) }

    val isProductBarcode = remember(record.rawValue, record.format, record.payloadType) {
        BarcodeValidator.isProductBarcodeCandidate(record.rawValue, record.format, record.payloadType)
    }

    val matchedProduct: ProductInfo? = (productLookupState as? ProductLookupState.Success)?.product

    // Confirmation Dialog before opening any external application
    pendingExternalAction?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingExternalAction = null },
            modifier = Modifier.testTag("external_action_confirm_dialog"),
            icon = {
                Icon(
                    imageVector = if (pending.isSuspicious) Icons.Default.WarningAmber else Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = pending.title,
                    tint = if (pending.isSuspicious) DangerCrimson else glass.primaryAccent,
                    modifier = Modifier.size(30.dp)
                )
            },
            title = {
                Text(
                    text = pending.title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = pending.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (pending.warnings.isNotEmpty()) {
                        pending.warnings.forEachIndexed { index, warn ->
                            Text(
                                text = "${index + 1}. $warn",
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerCrimson,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Destination: ${pending.destination}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = glass.textPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pending.onConfirm
                        pendingExternalAction = null
                        action()
                    },
                    colors = if (pending.isSuspicious) {
                        ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.testTag("confirm_external_action_button")
                ) {
                    Text(pending.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingExternalAction = null },
                    modifier = Modifier.testTag("cancel_external_action_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showProductDetailsDialog && matchedProduct != null) {
        ProductDetailsModalDialog(
            product = matchedProduct,
            onDismiss = { showProductDetailsDialog = false },
            onCopyBarcode = {
                ActionIntentHelper.copyToClipboard(context, "Product Barcode", matchedProduct.barcode)
                onShowMessage("Barcode ${matchedProduct.barcode} copied to clipboard")
            },
            onShareProduct = {
                ActionIntentHelper.shareText(context, matchedProduct.buildShareSummary(), "Share Product Details")
            },
            onOpenReferenceUrl = {
                showProductDetailsDialog = false
                pendingExternalAction = PendingExternalAction(
                    title = "Open Product Database Page",
                    description = "You are about to open this product record on Open Food Facts in your browser.",
                    destination = matchedProduct.sourceUrl,
                    confirmLabel = "Open Page",
                    onConfirm = {
                        ActionIntentHelper.openUrlSafely(context, matchedProduct.sourceUrl)
                    }
                )
            },
            onSearchOnline = {
                showProductDetailsDialog = false
                val query = "${matchedProduct.brand} ${matchedProduct.productName} ${matchedProduct.barcode}".trim()
                pendingExternalAction = PendingExternalAction(
                    title = "Search Product Online",
                    description = "Open your browser to search online for this product.",
                    destination = query,
                    confirmLabel = "Search Online",
                    onConfirm = {
                        ActionIntentHelper.searchOnline(context, query)
                    }
                )
            }
        )
    }

    if (showEditNotesModal) {
        AlertDialog(
            onDismissRequest = { showEditNotesModal = false },
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
                        showEditNotesModal = false
                        onShowMessage("Annotation saved to Vault")
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNotesModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    fun requestOpenUrlAction(rawUrl: String) {
        val validation = ActionIntentHelper.validateExternalHttpUrl(rawUrl)
        if (validation is UrlValidationResult.Rejected) {
            onShowMessage(validation.reason)
            return
        }
        val validUrl = (validation as UrlValidationResult.Valid).normalizedUrl
        val security = parsed.urlSecurityReport
        val isRisky = security != null && security.riskLevel != SecurityRiskLevel.SAFE
        onOpenSuspiciousUrlDialog()
        pendingExternalAction = PendingExternalAction(
            title = if (isRisky) "Suspicious Link Warning" else "Open Website Link",
            description = if (isRisky) {
                "QRYPT's offline URL heuristic engine flagged potential risks with this link. Review before opening in an external browser:"
            } else {
                "Do you want to open this link in your external browser?"
            },
            destination = validUrl,
            isSuspicious = isRisky,
            warnings = security?.warnings.orEmpty(),
            confirmLabel = if (isRisky) "Proceed Anyway" else "Open Link",
            onConfirm = {
                val opened = ActionIntentHelper.openUrlSafely(context, validUrl)
                if (!opened) {
                    onShowMessage("Unable to open link on this device")
                }
            }
        )
    }

    fun requestSearchOnlineAction(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return
        pendingExternalAction = PendingExternalAction(
            title = "Search Online",
            description = "Do you want to search for this content in your web browser?",
            destination = cleanQuery,
            confirmLabel = "Search Online",
            onConfirm = {
                ActionIntentHelper.searchOnline(context, cleanQuery)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .testTag("scan_result_sheet_root"),
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
                        color = glass.textSecondary,
                        modifier = Modifier.testTag("sheet_content_type_label")
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = matchedProduct?.productName ?: record.primaryTitle.ifEmpty { parsed.primaryTitle },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    modifier = Modifier.testTag("sheet_primary_title")
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
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("sheet_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Details"
                )
            }
        }

        // URL Security Banner if URL or Blocked Scheme
        parsed.urlSecurityReport?.let { security ->
            val bannerColor = when (security.riskLevel) {
                SecurityRiskLevel.SAFE -> glass.primaryAccent
                SecurityRiskLevel.CAUTION -> CautionAmber
                SecurityRiskLevel.SUSPICIOUS -> DangerCrimson
            }
            LiquidGlassCard(
                accentTint = bannerColor,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_url_security_banner")
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
                        security.warnings.forEachIndexed { idx, warn ->
                            Text(
                                text = "${idx + 1}. $warn",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Context-Aware Primary Smart Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (parsed.type) {
                ParsedPayloadType.URL -> {
                    val isRisky = parsed.urlSecurityReport?.riskLevel != SecurityRiskLevel.SAFE
                    Button(
                        onClick = { requestOpenUrlAction(record.rawValue) },
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
                        Text("Open Link")
                    }
                }

                ParsedPayloadType.PRODUCT, ParsedPayloadType.ISBN -> {
                    if (matchedProduct != null) {
                        Button(
                            onClick = { showProductDetailsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sheet_view_product_button")
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Product")
                        }
                    } else {
                        Button(
                            onClick = { onRetryProductLookup(record, true) },
                            enabled = productLookupState !is ProductLookupState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sheet_lookup_product_button")
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (productLookupState is ProductLookupState.Loading) {
                                    "Looking Up Product..."
                                } else {
                                    "Lookup Product Details"
                                }
                            )
                        }
                    }
                }

                ParsedPayloadType.WIFI -> {
                    Button(
                        onClick = {
                            val msg = ActionIntentHelper.connectWifi(context, parsed)
                            onShowMessage(msg)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_wifi_action_button")
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Password & Wi-Fi Settings")
                    }
                }

                ParsedPayloadType.CONTACT_VCARD -> {
                    Button(
                        onClick = {
                            pendingExternalAction = PendingExternalAction(
                                title = "Add Contact",
                                description = "Open your Contacts app to save ${parsed.primaryTitle}?",
                                destination = parsed.primaryTitle,
                                confirmLabel = "Add Contact",
                                onConfirm = { ActionIntentHelper.insertContact(context, parsed) }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_add_contact_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Contact")
                    }
                }

                ParsedPayloadType.PHONE -> {
                    Button(
                        onClick = {
                            pendingExternalAction = PendingExternalAction(
                                title = "Call Phone Number",
                                description = "Open your phone dialer with this number?",
                                destination = parsed.primaryTitle,
                                confirmLabel = "Call",
                                onConfirm = { ActionIntentHelper.dialPhone(context, record.rawValue) }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call")
                    }
                }

                ParsedPayloadType.SMS -> {
                    Button(
                        onClick = {
                            pendingExternalAction = PendingExternalAction(
                                title = "Send SMS Message",
                                description = "Open your messaging app to compose this SMS?",
                                destination = parsed.primaryTitle,
                                confirmLabel = "Send Message",
                                onConfirm = { ActionIntentHelper.sendSms(context, parsed) }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_send_sms_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Message")
                    }
                }

                ParsedPayloadType.EMAIL -> {
                    Button(
                        onClick = {
                            pendingExternalAction = PendingExternalAction(
                                title = "Send Email",
                                description = "Open your email client to compose a message to ${parsed.primaryTitle}?",
                                destination = parsed.primaryTitle,
                                confirmLabel = "Send Email",
                                onConfirm = { ActionIntentHelper.sendEmail(context, parsed) }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_send_email_button")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Email")
                    }
                }

                ParsedPayloadType.GEO -> {
                    Button(
                        onClick = {
                            pendingExternalAction = PendingExternalAction(
                                title = "View Location",
                                description = "Open these coordinates in your maps application?",
                                destination = parsed.primaryTitle,
                                confirmLabel = "View Location",
                                onConfirm = { ActionIntentHelper.openGeoLocation(context, record.rawValue) }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_view_location_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Location")
                    }
                }

                ParsedPayloadType.CALENDAR -> {
                    Button(
                        onClick = {
                            pendingExternalAction = PendingExternalAction(
                                title = "Add Calendar Event",
                                description = "Open your calendar application to save ${parsed.primaryTitle}?",
                                destination = parsed.primaryTitle,
                                confirmLabel = "Add to Calendar",
                                onConfirm = { ActionIntentHelper.insertCalendarEvent(context, parsed) }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_calendar_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Calendar")
                    }
                }

                ParsedPayloadType.PLAIN_TEXT -> {
                    Button(
                        onClick = {
                            ActionIntentHelper.copyToClipboard(context, parsed.primaryTitle, record.rawValue)
                            onShowMessage("Copied to clipboard")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_primary_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                }
            }
        }

        // Secondary Action Grid (Copy, Share, Search Online, Share Image, Note, Delete)
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
                Text("Copy")
            }

            OutlinedButton(
                onClick = {
                    val sharePayload = matchedProduct?.buildShareSummary() ?: record.rawValue
                    ActionIntentHelper.shareText(context, sharePayload)
                },
                modifier = Modifier.testTag("sheet_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }

            OutlinedButton(
                onClick = {
                    val query = matchedProduct?.let { "${it.brand} ${it.productName} ${it.barcode}".trim() }
                        ?: record.rawValue
                    requestSearchOnlineAction(query)
                },
                modifier = Modifier.testTag("sheet_search_online_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search Online")
            }

            if (previewBitmap != null) {
                OutlinedButton(
                    onClick = {
                        ActionIntentHelper.shareBitmapPng(context, previewBitmap, "qrypt_${record.format.shortTag}")
                    },
                    modifier = Modifier.testTag("sheet_share_image_button")
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Image")
                }
            }

            OutlinedButton(
                onClick = {
                    onOpenEditNotesDialog()
                    showEditNotesModal = true
                },
                modifier = Modifier.testTag("sheet_edit_note_button")
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerCrimson),
                modifier = Modifier.testTag("sheet_delete_button")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete")
            }
        }

        // Product Identification Section for Product & ISBN Barcodes
        if (isProductBarcode || productLookupState !is ProductLookupState.Idle) {
            ProductIdentificationSection(
                state = productLookupState,
                rawBarcode = record.rawValue,
                formatDisplayName = record.format.displayName,
                onViewFullProduct = { showProductDetailsDialog = true },
                onRetryLookup = { onRetryProductLookup(record, true) },
                onSearchOnline = { requestSearchOnlineAction(record.rawValue) },
                onOpenSourceUrl = { url ->
                    pendingExternalAction = PendingExternalAction(
                        title = "Open Product Database Page",
                        description = "Open this product entry on Open Food Facts in your external browser?",
                        destination = url,
                        confirmLabel = "Open Page",
                        onConfirm = { ActionIntentHelper.openUrlSafely(context, url) }
                    )
                }
            )
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
                            .testTag("sheet_raw_payload_text")
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

@Composable
private fun ProductIdentificationSection(
    state: ProductLookupState,
    rawBarcode: String,
    formatDisplayName: String,
    onViewFullProduct: () -> Unit,
    onRetryLookup: () -> Unit,
    onSearchOnline: () -> Unit,
    onOpenSourceUrl: (String) -> Unit
) {
    val glass = LocalLiquidGlass.current

    when (state) {
        is ProductLookupState.Idle -> {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_idle"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "PRODUCT IDENTIFICATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Query Open Food Facts for barcode $rawBarcode ($formatDisplayName).",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onRetryLookup,
                        modifier = Modifier.testTag("product_idle_lookup_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lookup")
                    }
                }
            }
        }

        is ProductLookupState.Loading -> {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_loading"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = glass.primaryAccent,
                        strokeWidth = 2.8.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IDENTIFYING PRODUCT...",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Querying Open Food Facts for ${state.barcode} (${state.formatDisplayName})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Only the numeric barcode is transmitted over HTTPS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }
        }

        is ProductLookupState.Success -> {
            val product = state.product
            LiquidGlassCard(
                accentTint = SafeEmerald,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_success_card"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = SafeEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (product.isFromCache) {
                                    "VERIFIED PRODUCT (LOCAL CACHE)"
                                } else {
                                    "VERIFIED PRODUCT IDENTIFIED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = product.formatName,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                                .border(1.dp, glass.glassSpecularBorderTop, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!product.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.productName,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = product.productName,
                                    tint = glass.primaryAccent,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.productName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("product_card_name")
                            )
                            if (product.brand.isNotBlank()) {
                                Text(
                                    text = "Brand: ${product.brand}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.primaryAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("product_card_brand")
                                )
                            }
                            val metaLine = listOfNotNull(
                                product.quantity.takeIf { it.isNotBlank() }?.let { "Size: $it" },
                                "GTIN: ${product.barcode}"
                            ).joinToString(" | ")
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    if (product.categories.isNotBlank()) {
                        Text(
                            text = "Category: ${product.categories}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (product.nutrition.hasAnyData) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            product.nutrition.nutriscoreGrade?.let { grade ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SafeEmerald.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "Nutri-Score $grade",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            product.nutrition.novaGroup?.let { nova ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = glass.primaryAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "NOVA Group $nova",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.primaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            product.nutrition.energyKcal100g?.let { kcal ->
                                Text(
                                    text = "${kcal.toInt()} kcal / 100g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }

                    if (product.ingredientsText.isNotBlank()) {
                        Text(
                            text = "Ingredients: ${product.ingredientsText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onViewFullProduct,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_card_view_details_button")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Product Details")
                        }
                        OutlinedButton(
                            onClick = { onOpenSourceUrl(product.sourceUrl) },
                            modifier = Modifier.testTag("product_card_open_source_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Database")
                        }
                    }

                    Text(
                        text = "Source: ${product.sourceDatabase} | License: ${product.attributionLicense}",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }
            }
        }

        is ProductLookupState.NotFound -> {
            LiquidGlassCard(
                accentTint = CautionAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_not_found"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Product Not Found",
                            tint = CautionAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Product Not Found in Open Food Facts",
                            style = MaterialTheme.typography.titleSmall,
                            color = CautionAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Barcode: ${state.barcode} (${state.formatDisplayName}) | Origin: ${state.gs1Origin}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = glass.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSearchOnline,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_not_found_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Online")
                        }
                        OutlinedButton(
                            onClick = onRetryLookup,
                            modifier = Modifier.testTag("product_not_found_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }

        is ProductLookupState.Offline -> {
            LiquidGlassCard(
                accentTint = CautionAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_offline"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = CautionAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Offline Mode: Product Lookup Unavailable",
                            style = MaterialTheme.typography.titleSmall,
                            color = CautionAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Decoded Barcode: ${state.barcode} (${state.formatDisplayName}) | ${state.gs1Origin}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = glass.textSecondary
                    )
                    OutlinedButton(
                        onClick = onRetryLookup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_offline_retry_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Product Lookup")
                    }
                }
            }
        }

        is ProductLookupState.Error -> {
            LiquidGlassCard(
                accentTint = DangerCrimson,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_error"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Product Lookup Error",
                            tint = DangerCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Product Lookup Error",
                            style = MaterialTheme.typography.titleSmall,
                            color = DangerCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRetryLookup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_error_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Lookup")
                        }
                        OutlinedButton(
                            onClick = onSearchOnline,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_error_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Online")
                        }
                    }
                }
            }
        }

        is ProductLookupState.InvalidBarcode -> {
            LiquidGlassCard(
                accentTint = CautionAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_lookup_invalid"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Invalid Product Barcode Format",
                        style = MaterialTheme.typography.titleSmall,
                        color = CautionAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailsModalDialog(
    product: ProductInfo,
    onDismiss: () -> Unit,
    onCopyBarcode: () -> Unit,
    onShareProduct: () -> Unit,
    onOpenReferenceUrl: () -> Unit,
    onSearchOnline: () -> Unit
) {
    val glass = LocalLiquidGlass.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("product_details_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PRODUCT SPECIFICATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.primaryAccent
                )
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (product.brand.isNotBlank()) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.primaryAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.productName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                ProductDetailRow("Barcode (${product.formatName})", product.barcode)
                if (product.quantity.isNotBlank()) {
                    ProductDetailRow("Package Quantity", product.quantity)
                }
                if (product.categories.isNotBlank()) {
                    ProductDetailRow("Categories", product.categories)
                }
                if (product.packaging.isNotBlank()) {
                    ProductDetailRow("Packaging", product.packaging)
                }
                if (product.origins.isNotBlank()) {
                    ProductDetailRow("Origin", product.origins)
                }
                if (product.allergens.isNotBlank()) {
                    ProductDetailRow("Allergens", product.allergens)
                }

                if (product.nutrition.hasAnyData) {
                    HorizontalDivider(color = glass.glassSpecularBorderBottom)
                    Text(
                        text = "NUTRITION FACTS (PER 100G / 100ML)",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                    product.nutrition.nutriscoreGrade?.let {
                        ProductDetailRow("Nutri-Score Grade", it)
                    }
                    product.nutrition.novaGroup?.let {
                        ProductDetailRow("NOVA Processing Group", "Group $it")
                    }
                    product.nutrition.energyKcal100g?.let {
                        ProductDetailRow("Energy", "${String.format(Locale.US, "%.1f", it)} kcal")
                    }
                    product.nutrition.fat100g?.let {
                        ProductDetailRow("Fat", "${String.format(Locale.US, "%.1f", it)} g")
                    }
                    product.nutrition.saturatedFat100g?.let {
                        ProductDetailRow("Saturated Fat", "${String.format(Locale.US, "%.1f", it)} g")
                    }
                    product.nutrition.carbohydrates100g?.let {
                        ProductDetailRow("Carbohydrates", "${String.format(Locale.US, "%.1f", it)} g")
                    }
                    product.nutrition.sugars100g?.let {
                        ProductDetailRow("Sugars", "${String.format(Locale.US, "%.1f", it)} g")
                    }
                    product.nutrition.fiber100g?.let {
                        ProductDetailRow("Fiber", "${String.format(Locale.US, "%.1f", it)} g")
                    }
                    product.nutrition.proteins100g?.let {
                        ProductDetailRow("Proteins", "${String.format(Locale.US, "%.1f", it)} g")
                    }
                    product.nutrition.salt100g?.let {
                        ProductDetailRow("Salt", "${String.format(Locale.US, "%.2f", it)} g")
                    }
                }

                if (product.ingredientsText.isNotBlank()) {
                    HorizontalDivider(color = glass.glassSpecularBorderBottom)
                    Text(
                        text = "INGREDIENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                    Text(
                        text = product.ingredientsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                }

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyBarcode,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Barcode")
                    }
                    OutlinedButton(
                        onClick = onShareProduct,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share Info")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenReferenceUrl,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Food Facts")
                    }
                    OutlinedButton(
                        onClick = onSearchOnline,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Search Online")
                    }
                }

                Text(
                    text = "Data provided by ${product.sourceDatabase} under the ${product.attributionLicense}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_product_details_dialog_button")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ProductDetailRow(label: String, value: String) {
    val glass = LocalLiquidGlass.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = glass.textPrimary,
            modifier = Modifier.weight(0.58f)
        )
    }
}
