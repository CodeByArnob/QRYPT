package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AccentColorPreset
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserScanSettings
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.ScanRegionMode
import com.example.ui.components.FormatBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.LocalLiquidGlass

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserScanSettings,
    totalStoredRecords: Int,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAccentPresetChange: (AccentColorPreset) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onDuplicatePreventionChange: (Boolean) -> Unit,
    onDuplicateCooldownChange: (Int) -> Unit,
    onAutoCopyChange: (Boolean) -> Unit,
    onAutoOpenSafeUrlsChange: (Boolean) -> Unit,
    onLowLightAutoBoostChange: (Boolean) -> Unit,
    onDefaultScanRegionChange: (ScanRegionMode) -> Unit,
    onContinuousModeChange: (Boolean) -> Unit,
    onClearNonFavorites: () -> Unit,
    onPurgeAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = DangerCrimson
                )
            },
            title = { Text("Purge Entire Local Vault?") },
            text = {
                Text(
                    "This will permanently delete all $totalStoredRecords scanned and generated records (including favorites) from local SQLite storage. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeConfirmDialog = false
                        onPurgeAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson),
                    modifier = Modifier.testTag("confirm_purge_vault_button")
                ) {
                    Text("Purge Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // QRYPT Identity & Developer Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QRYPT",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.primaryAccent
                )
                Text(
                    text = "Scan Smarter. Instantly.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Developer: Arnobrazz • Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        // Appearance & Liquid Glass Theme Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = glass.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIQUID GLASS APPEARANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                }

                Text(
                    text = "Surface Theme Mode (${if (glass.isDark) "Dark Active" else "Light Active"})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val title = when (mode) {
                            AppThemeMode.DARK -> "Dark Mode"
                            AppThemeMode.LIGHT -> "Light Mode"
                            AppThemeMode.SYSTEM -> "System"
                        }
                        LiquidGlassPill(
                            text = title,
                            selected = settings.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_theme_${mode.name}")
                        )
                    }
                }

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                Text(
                    text = "Prism Accent Wavelength",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccentColorPreset.entries.forEach { preset ->
                        val isSelected = settings.accentPreset == preset
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(preset.primaryHex).copy(alpha = 0.22f)
                                    else glass.glassSurfaceTop
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(preset.primaryHex) else glass.glassSpecularBorderTop,
                                    shape = CircleShape
                                )
                                .clickable { onAccentPresetChange(preset) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("settings_accent_${preset.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.primaryHex))
                            )
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                SettingToggleRow(
                    title = "Android 12+ Material You Dynamic Palette",
                    subtitle = "Blend system wallpaper tones with Liquid Glass surfaces",
                    checked = settings.dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange,
                    testTag = "settings_dynamic_color_switch"
                )
            }
        }

        // Optical Scanner Engine Behavior Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = glass.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPTICAL ENGINE & FEEDBACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                }

                SettingToggleRow(
                    title = "Tactile Haptic Feedback",
                    subtitle = "Crisp micro-vibration pulse on barcode lock",
                    checked = settings.hapticFeedbackEnabled,
                    onCheckedChange = onHapticChange,
                    testTag = "settings_haptic_switch"
                )

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                SettingToggleRow(
                    title = "Acoustic Beep Tone",
                    subtitle = "Synthesized audio confirmation (distinct alert on suspicious links)",
                    checked = settings.soundFeedbackEnabled,
                    onCheckedChange = onSoundChange,
                    testTag = "settings_sound_switch"
                )

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                SettingToggleRow(
                    title = "Duplicate-Scan Guard",
                    subtitle = "Prevents accidental repeated captures of the same barcode",
                    checked = settings.duplicatePreventionEnabled,
                    onCheckedChange = onDuplicatePreventionChange,
                    testTag = "settings_duplicate_switch"
                )

                if (settings.duplicatePreventionEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duplicate Cooldown Window",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${settings.duplicateCooldownSeconds} seconds",
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.primaryAccent
                            )
                        }
                        Slider(
                            value = settings.duplicateCooldownSeconds.toFloat(),
                            onValueChange = { onDuplicateCooldownChange(it.toInt()) },
                            valueRange = 1f..15f,
                            steps = 13
                        )
                    }
                }

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                SettingToggleRow(
                    title = "Automatic Clipboard Copy",
                    subtitle = "Immediately copy decoded payload to system clipboard",
                    checked = settings.autoCopyToClipboard,
                    onCheckedChange = onAutoCopyChange,
                    testTag = "settings_auto_copy_switch"
                )

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                SettingToggleRow(
                    title = "Auto-Open Verified Safe URLs",
                    subtitle = "Automatically launch HTTPS links that pass all anti-phishing checks",
                    checked = settings.autoOpenSafeUrls,
                    onCheckedChange = onAutoOpenSafeUrlsChange,
                    testTag = "settings_auto_open_switch"
                )

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                SettingToggleRow(
                    title = "Low-Light Automatic Exposure Boost",
                    subtitle = "Auto-increase Camera2 sensor exposure index when ambient luma < 45 lux",
                    checked = settings.lowLightAutoBoost,
                    onCheckedChange = onLowLightAutoBoostChange,
                    testTag = "settings_low_light_switch"
                )

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                SettingToggleRow(
                    title = "Default Continuous Stream Mode",
                    subtitle = "Keep scanning without opening result sheet after each capture",
                    checked = settings.continuousScanMode,
                    onCheckedChange = onContinuousModeChange,
                    testTag = "settings_continuous_switch"
                )

                HorizontalDivider(color = glass.glassSpecularBorderBottom)

                Text(
                    text = "Default Viewfinder Target Region",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanRegionMode.entries.forEach { region ->
                        LiquidGlassPill(
                            text = region.title,
                            selected = settings.defaultScanRegion == region,
                            onClick = { onDefaultScanRegionChange(region) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Supported Formats Matrix Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = glass.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SUPPORTED OFFLINE SYMBOLOGIES (13 FORMATS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                }
                Text(
                    text = "Dual-engine architecture (Google ML Kit Barcode Scanning + ZXing Multi-Format Core) with zero network dependencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BarcodeFormatType.entries.forEach { format ->
                        FormatBadge(format = format)
                    }
                }
            }
        }

        // Privacy & Local Storage Management Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = glass.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIVACY & LOCAL VAULT MANAGEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primaryAccent
                    )
                }

                Text(
                    text = "100% Offline Guarantee: QRYPT requires zero accounts, zero cloud sync, and zero internet permissions. All $totalStoredRecords records reside strictly on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearNonFavorites,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_clear_non_favorites_button")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Unfavorited")
                    }

                    Button(
                        onClick = { showPurgeConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_purge_vault_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purge Vault")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val glass = LocalLiquidGlass.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
