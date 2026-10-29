package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.QrErrorCorrection
import com.example.domain.parser.PayloadParser
import com.example.engine.BarcodeGeneratorEngine
import com.example.engine.GenerationResult
import com.example.ui.components.FormatBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.GeneratorTemplate
import com.example.util.ActionIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ColorPairPreset(
    val name: String,
    val fgArgb: Int,
    val bgArgb: Int
)

private val COLOR_PAIR_PRESETS = listOf(
    ColorPairPreset("Obsidian Ink", 0xFF0A0F1D.toInt(), 0xFFFFFFFF.toInt()),
    ColorPairPreset("Arctic Cyan", 0xFF0369A1.toInt(), 0xFFF0F9FF.toInt()),
    ColorPairPreset("Aether Indigo", 0xFF4338CA.toInt(), 0xFFEEF2FF.toInt()),
    ColorPairPreset("Emerald Vault", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
    ColorPairPreset("Crimson Studio", 0xFF9F1239.toInt(), 0xFFFFF1F2.toInt()),
    ColorPairPreset("Cyber Dark", 0xFF38BDF8.toInt(), 0xFF070B19.toInt())
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorStudioScreen(
    onSaveToVault: (String, BarcodeFormatType, String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    var selectedTemplate by remember { mutableStateOf(GeneratorTemplate.URL) }
    var selectedFormat by remember { mutableStateOf(BarcodeFormatType.QR_CODE) }

    // Template specific inputs
    var urlInput by remember { mutableStateOf("https://developer.android.com/jetpack/compose") }
    var plainTextInput by remember { mutableStateOf("QRYPT Liquid-Glass Optical Studio") }
    var productGtinInput by remember { mutableStateOf("5901234123457") }

    // Wi-Fi inputs
    var wifiSsid by remember { mutableStateOf("AetherGlass-5G") }
    var wifiPassword by remember { mutableStateOf("Prism#2026!Secure") }
    var wifiAuth by remember { mutableStateOf("WPA") }
    var wifiHidden by remember { mutableStateOf(false) }

    // Contact vCard inputs
    var vcardName by remember { mutableStateOf("Aria Chen") }
    var vcardOrg by remember { mutableStateOf("Prism Optical Systems") }
    var vcardTitle by remember { mutableStateOf("Lead Hardware Architect") }
    var vcardPhone by remember { mutableStateOf("+1-415-891-0192") }
    var vcardEmail by remember { mutableStateOf("aria.chen@prismoptics.io") }
    var vcardWebsite by remember { mutableStateOf("https://prismoptics.io") }
    var vcardAddress by remember { mutableStateOf("500 Howard St, San Francisco, CA") }

    // Email inputs
    var emailTo by remember { mutableStateOf("support@prismoptics.io") }
    var emailSubject by remember { mutableStateOf("Optical Calibration Report") }
    var emailBody by remember { mutableStateOf("Attached are the verified GTIN-13 and Aztec test matrices.") }

    // SMS inputs
    var smsPhone by remember { mutableStateOf("+14158910192") }
    var smsMessage by remember { mutableStateOf("Confirmed arrival at Gate B12.") }

    // Phone input
    var phoneDial by remember { mutableStateOf("+14158910192") }

    // Geo inputs
    var geoLat by remember { mutableStateOf("37.7897") }
    var geoLng by remember { mutableStateOf("-122.4014") }
    var geoLabel by remember { mutableStateOf("San Francisco Design Center") }

    // Calendar inputs
    var calTitle by remember { mutableStateOf("Next-Gen Optics Keynote") }
    var calLocation by remember { mutableStateOf("Moscone West, Hall 3") }
    var calStart by remember { mutableStateOf("20261104T093000Z") }
    var calEnd by remember { mutableStateOf("20261104T110000Z") }
    var calDesc by remember { mutableStateOf("Live demonstration of multi-code AR barcode recognition.") }

    // Customization states
    var selectedColorPresetIndex by remember { mutableIntStateOf(0) }
    var selectedSizePx by remember { mutableIntStateOf(1024) }
    var selectedErrorCorrection by remember { mutableStateOf(QrErrorCorrection.M) }
    var roundedQrDots by remember { mutableStateOf(true) }

    val activeColorPreset = COLOR_PAIR_PRESETS[selectedColorPresetIndex]

    val computedPayload = remember(
        selectedTemplate, selectedFormat,
        urlInput, plainTextInput, productGtinInput,
        wifiSsid, wifiPassword, wifiAuth, wifiHidden,
        vcardName, vcardOrg, vcardTitle, vcardPhone, vcardEmail, vcardWebsite, vcardAddress,
        emailTo, emailSubject, emailBody,
        smsPhone, smsMessage, phoneDial,
        geoLat, geoLng, geoLabel,
        calTitle, calLocation, calStart, calEnd, calDesc
    ) {
        if (selectedFormat != BarcodeFormatType.QR_CODE &&
            selectedFormat != BarcodeFormatType.AZTEC &&
            selectedFormat != BarcodeFormatType.PDF417 &&
            selectedFormat != BarcodeFormatType.DATA_MATRIX
        ) {
            productGtinInput
        } else {
            when (selectedTemplate) {
                GeneratorTemplate.URL -> urlInput
                GeneratorTemplate.WIFI -> PayloadParser.buildWifiPayload(wifiSsid, wifiPassword, wifiAuth, wifiHidden)
                GeneratorTemplate.VCARD -> PayloadParser.buildVCardPayload(
                    vcardName, vcardOrg, vcardTitle, vcardPhone, vcardEmail, vcardWebsite, vcardAddress
                )
                GeneratorTemplate.PLAIN_TEXT -> plainTextInput
                GeneratorTemplate.PRODUCT_GTIN -> productGtinInput
                GeneratorTemplate.EMAIL -> PayloadParser.buildEmailPayload(emailTo, emailSubject, emailBody)
                GeneratorTemplate.SMS -> PayloadParser.buildSmsPayload(smsPhone, smsMessage)
                GeneratorTemplate.PHONE -> "tel:${phoneDial.trim()}"
                GeneratorTemplate.GEO -> PayloadParser.buildGeoPayload(geoLat, geoLng, geoLabel)
                GeneratorTemplate.CALENDAR -> PayloadParser.buildCalendarPayload(calTitle, calLocation, calStart, calEnd, calDesc)
            }
        }
    }

    val qrConfig = remember(activeColorPreset, selectedSizePx, selectedErrorCorrection, roundedQrDots) {
        QrCustomizationConfig(
            sizePx = selectedSizePx,
            marginModules = 2,
            errorCorrection = selectedErrorCorrection,
            foregroundArgb = activeColorPreset.fgArgb,
            backgroundArgb = activeColorPreset.bgArgb,
            roundedDots = roundedQrDots
        )
    }

    val generationResult: GenerationResult = remember(
        computedPayload,
        selectedFormat,
        qrConfig
    ) {
        BarcodeGeneratorEngine.generate(computedPayload, selectedFormat, qrConfig)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Live Preview Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
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
                    Column {
                        Text(
                            text = "OPTICAL GENERATOR STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primaryAccent
                        )
                        Text(
                            text = "${selectedFormat.displayName} Matrix",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FormatBadge(format = selectedFormat)
                }

                when (val res = generationResult) {
                    is GenerationResult.Success -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(activeColorPreset.bgArgb))
                                .border(1.dp, glass.glassSpecularBorderTop, RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = res.bitmap.asImageBitmap(),
                                contentDescription = "Generated ${selectedFormat.displayName}",
                                modifier = Modifier
                                    .size(
                                        width = if (selectedFormat.is2D) 210.dp else 260.dp,
                                        height = if (selectedFormat.is2D) 210.dp else 115.dp
                                    )
                                    .testTag("studio_generated_bitmap"),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Text(
                            text = res.normalizedPayload.lineSequence().firstOrNull()?.take(56) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = glass.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSaveToVault(
                                        res.normalizedPayload,
                                        selectedFormat,
                                        "Created in Studio (${selectedTemplate.label})"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("studio_save_vault_button")
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Vault")
                            }

                            OutlinedButton(
                                onClick = {
                                    ActionIntentHelper.shareBitmapPng(
                                        context,
                                        res.bitmap,
                                        "qrypt_${selectedFormat.shortTag.lowercase()}"
                                    )
                                },
                                modifier = Modifier.testTag("studio_share_png_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share PNG")
                            }

                            OutlinedButton(
                                onClick = {
                                    ActionIntentHelper.copyToClipboard(
                                        context,
                                        selectedFormat.displayName,
                                        res.normalizedPayload
                                    )
                                    onShowMessage("Payload copied to clipboard")
                                },
                                modifier = Modifier.testTag("studio_copy_payload_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Payload", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    is GenerationResult.ValidationError -> {
                        LiquidGlassCard(
                            accentTint = CautionAmber,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Format Requirement (${selectedFormat.displayName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CautionAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = res.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Hint: ${selectedFormat.formatHint}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Symbology Selector (All 13 Formats)
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BARCODE SYMBOLOGY (13 FORMATS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.primaryAccent
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BarcodeFormatType.entries.forEach { format ->
                        LiquidGlassPill(
                            text = format.displayName,
                            selected = selectedFormat == format,
                            onClick = {
                                selectedFormat = format
                                if (!format.is2D) {
                                    selectedTemplate = GeneratorTemplate.PRODUCT_GTIN
                                    productGtinInput = format.samplePayload
                                } else if (format != BarcodeFormatType.QR_CODE) {
                                    selectedTemplate = GeneratorTemplate.PLAIN_TEXT
                                    plainTextInput = format.samplePayload
                                }
                            },
                            modifier = Modifier.testTag("studio_format_${format.name}")
                        )
                    }
                }
                Text(
                    text = selectedFormat.formatHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        // Structured Payload Templates (When 2D QR / Matrix selected)
        if (selectedFormat == BarcodeFormatType.QR_CODE) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PAYLOAD SCHEMA TEMPLATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GeneratorTemplate.entries.forEach { tpl ->
                            LiquidGlassPill(
                                text = tpl.label,
                                selected = selectedTemplate == tpl,
                                onClick = {
                                    selectedTemplate = tpl
                                    if (tpl == GeneratorTemplate.PRODUCT_GTIN) {
                                        selectedFormat = BarcodeFormatType.EAN_13
                                        productGtinInput = BarcodeFormatType.EAN_13.samplePayload
                                    }
                                },
                                modifier = Modifier.testTag("studio_template_${tpl.name}")
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Form Inputs Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PAYLOAD PARAMETERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.primaryAccent
                )

                if (!selectedFormat.is2D || selectedTemplate == GeneratorTemplate.PRODUCT_GTIN) {
                    OutlinedTextField(
                        value = productGtinInput,
                        onValueChange = { productGtinInput = it },
                        label = { Text("${selectedFormat.displayName} Code Value") },
                        supportingText = { Text(selectedFormat.formatHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_input_gtin")
                    )
                } else {
                    when (selectedTemplate) {
                        GeneratorTemplate.URL -> {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("Website URL (HTTPS recommended)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_input_url")
                            )
                        }

                        GeneratorTemplate.WIFI -> {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Network Name (SSID)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_input_wifi_ssid")
                            )
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("Password") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_input_wifi_password")
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("WPA", "WEP", "NOPASS").forEach { auth ->
                                    FilterChip(
                                        selected = wifiAuth == auth,
                                        onClick = { wifiAuth = auth },
                                        label = { Text(if (auth == "NOPASS") "Open" else auth) }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Hidden", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = wifiHidden, onCheckedChange = { wifiHidden = it })
                            }
                        }

                        GeneratorTemplate.VCARD -> {
                            OutlinedTextField(
                                value = vcardName,
                                onValueChange = { vcardName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = vcardOrg,
                                    onValueChange = { vcardOrg = it },
                                    label = { Text("Organization") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = vcardTitle,
                                    onValueChange = { vcardTitle = it },
                                    label = { Text("Title / Role") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = vcardPhone,
                                    onValueChange = { vcardPhone = it },
                                    label = { Text("Phone Number") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = vcardEmail,
                                    onValueChange = { vcardEmail = it },
                                    label = { Text("Email") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = vcardWebsite,
                                onValueChange = { vcardWebsite = it },
                                label = { Text("Website URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = vcardAddress,
                                onValueChange = { vcardAddress = it },
                                label = { Text("Street Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        GeneratorTemplate.PLAIN_TEXT -> {
                            OutlinedTextField(
                                value = plainTextInput,
                                onValueChange = { plainTextInput = it },
                                label = { Text("Plain Text Content") },
                                minLines = 2,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_input_plain_text")
                            )
                        }

                        GeneratorTemplate.EMAIL -> {
                            OutlinedTextField(
                                value = emailTo,
                                onValueChange = { emailTo = it },
                                label = { Text("Recipient Email") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = { Text("Subject Line") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = emailBody,
                                onValueChange = { emailBody = it },
                                label = { Text("Email Message") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        GeneratorTemplate.SMS -> {
                            OutlinedTextField(
                                value = smsPhone,
                                onValueChange = { smsPhone = it },
                                label = { Text("Recipient Phone Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = smsMessage,
                                onValueChange = { smsMessage = it },
                                label = { Text("SMS Message") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        GeneratorTemplate.PHONE -> {
                            OutlinedTextField(
                                value = phoneDial,
                                onValueChange = { phoneDial = it },
                                label = { Text("Telephone Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        GeneratorTemplate.GEO -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = geoLat,
                                    onValueChange = { geoLat = it },
                                    label = { Text("Latitude") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = geoLng,
                                    onValueChange = { geoLng = it },
                                    label = { Text("Longitude") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = geoLabel,
                                onValueChange = { geoLabel = it },
                                label = { Text("Place Name / Query") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        GeneratorTemplate.CALENDAR -> {
                            OutlinedTextField(
                                value = calTitle,
                                onValueChange = { calTitle = it },
                                label = { Text("Event Summary") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = calLocation,
                                onValueChange = { calLocation = it },
                                label = { Text("Venue / Location") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = calStart,
                                    onValueChange = { calStart = it },
                                    label = { Text("Start (YYYYMMDDTHHMMSSZ)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = calEnd,
                                    onValueChange = { calEnd = it },
                                    label = { Text("End (YYYYMMDDTHHMMSSZ)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = calDesc,
                                onValueChange = { calDesc = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> {}
                    }
                }
            }
        }

        // Customization & Optical Styling Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = glass.primaryAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "COLOR PALETTE & MATRIX PRECISION",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                }

                // Color Pair Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    COLOR_PAIR_PRESETS.forEachIndexed { index, preset ->
                        val isSelected = selectedColorPresetIndex == index
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) glass.primaryAccent.copy(alpha = 0.22f)
                                    else glass.glassSurfaceTop
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) glass.primaryAccent else glass.glassSpecularBorderTop,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorPresetIndex = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.fgArgb))
                                    .border(1.dp, Color(preset.bgArgb), CircleShape)
                            )
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // Output Resolution Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = glass.textSecondary, modifier = Modifier.size(16.dp))
                    Text("Resolution:", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                    listOf(512 to "512px", 1024 to "1024px HD", 1536 to "1536px Print").forEach { (px, label) ->
                        FilterChip(
                            selected = selectedSizePx == px,
                            onClick = { selectedSizePx = px },
                            label = { Text(label) }
                        )
                    }
                }

                // QR-specific Error Correction & Rounded Module Toggle
                if (selectedFormat == BarcodeFormatType.QR_CODE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QrErrorCorrection.entries.forEach { ec ->
                            FilterChip(
                                selected = selectedErrorCorrection == ec,
                                onClick = { selectedErrorCorrection = ec },
                                label = { Text(ec.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Liquid Rounded Matrix Modules",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Softens QR module corners while preserving optical scanner compatibility",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        Switch(
                            checked = roundedQrDots,
                            onCheckedChange = { roundedQrDots = it },
                            modifier = Modifier.testTag("studio_rounded_dots_switch")
                        )
                    }
                }
            }
        }
    }
}
