package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ScanRegionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prismDataStore: DataStore<Preferences> by preferencesDataStore(name = "prism_scan_settings")

enum class AppThemeMode(val label: String) {
    DARK("Liquid Dark (Recommended)"),
    LIGHT("Frosted Daylight"),
    SYSTEM("Follow System")
}

enum class AccentColorPreset(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val glowHex: Long
) {
    CYAN_PRISM("QRYPT Cyan", 0xFF00E5FF, 0xFF3B82F6, 0xFF0284C7),
    VIOLET_AETHER("Aether Violet", 0xFFA855F7, 0xFFC084FC, 0xFF7E22CE),
    EMERALD_PULSE("Matrix Emerald", 0xFF10B981, 0xFF34D399, 0xFF047857),
    AMBER_SOLAR("Solar Flare", 0xFFF59E0B, 0xFFFBBF24, 0xFFB45309),
    CRIMSON_LASER("Crimson Laser", 0xFFF43F5E, 0xFFFB7185, 0xFFBE123C)
}

data class UserScanSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val dynamicColorEnabled: Boolean = false,
    val accentPreset: AccentColorPreset = AccentColorPreset.CYAN_PRISM,
    val hapticFeedbackEnabled: Boolean = true,
    val soundFeedbackEnabled: Boolean = true,
    val duplicatePreventionEnabled: Boolean = true,
    val duplicateCooldownSeconds: Int = 3,
    val autoCopyToClipboard: Boolean = false,
    val autoOpenSafeUrls: Boolean = false,
    val automaticProductLookup: Boolean = true,
    val lowLightAutoBoost: Boolean = true,
    val defaultScanRegion: ScanRegionMode = ScanRegionMode.PRECISION_SQUARE,
    val continuousScanMode: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val DUPLICATE_PREVENTION = booleanPreferencesKey("duplicate_prevention")
        val DUPLICATE_COOLDOWN_SEC = intPreferencesKey("duplicate_cooldown_sec")
        val AUTO_COPY = booleanPreferencesKey("auto_copy")
        val AUTO_OPEN_SAFE_URL = booleanPreferencesKey("auto_open_safe_url")
        val AUTO_PRODUCT_LOOKUP = booleanPreferencesKey("auto_product_lookup")
        val LOW_LIGHT_AUTO_BOOST = booleanPreferencesKey("low_light_auto_boost")
        val DEFAULT_SCAN_REGION = stringPreferencesKey("default_scan_region")
        val CONTINUOUS_SCAN_MODE = booleanPreferencesKey("continuous_scan_mode")
    }

    val settingsFlow: Flow<UserScanSettings> = context.prismDataStore.data.map { prefs ->
        UserScanSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                AppThemeMode.entries.firstOrNull { it.name == name }
            } ?: AppThemeMode.DARK,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
            accentPreset = prefs[Keys.ACCENT_PRESET]?.let { name ->
                AccentColorPreset.entries.firstOrNull { it.name == name }
            } ?: AccentColorPreset.CYAN_PRISM,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            soundFeedbackEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            duplicatePreventionEnabled = prefs[Keys.DUPLICATE_PREVENTION] ?: true,
            duplicateCooldownSeconds = prefs[Keys.DUPLICATE_COOLDOWN_SEC] ?: 3,
            autoCopyToClipboard = prefs[Keys.AUTO_COPY] ?: false,
            autoOpenSafeUrls = prefs[Keys.AUTO_OPEN_SAFE_URL] ?: false,
            automaticProductLookup = prefs[Keys.AUTO_PRODUCT_LOOKUP] ?: true,
            lowLightAutoBoost = prefs[Keys.LOW_LIGHT_AUTO_BOOST] ?: true,
            defaultScanRegion = prefs[Keys.DEFAULT_SCAN_REGION]?.let { name ->
                ScanRegionMode.entries.firstOrNull { it.name == name }
            } ?: ScanRegionMode.PRECISION_SQUARE,
            continuousScanMode = prefs[Keys.CONTINUOUS_SCAN_MODE] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.prismDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAccentPreset(preset: AccentColorPreset) {
        context.prismDataStore.edit { it[Keys.ACCENT_PRESET] = preset.name }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setSoundFeedback(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setDuplicatePrevention(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.DUPLICATE_PREVENTION] = enabled }
    }

    suspend fun setDuplicateCooldownSeconds(seconds: Int) {
        context.prismDataStore.edit { it[Keys.DUPLICATE_COOLDOWN_SEC] = seconds.coerceIn(1, 15) }
    }

    suspend fun setAutoCopy(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.AUTO_COPY] = enabled }
    }

    suspend fun setAutoOpenSafeUrls(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.AUTO_OPEN_SAFE_URL] = enabled }
    }

    suspend fun setAutomaticProductLookup(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.AUTO_PRODUCT_LOOKUP] = enabled }
    }

    suspend fun setLowLightAutoBoost(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.LOW_LIGHT_AUTO_BOOST] = enabled }
    }

    suspend fun setDefaultScanRegion(region: ScanRegionMode) {
        context.prismDataStore.edit { it[Keys.DEFAULT_SCAN_REGION] = region.name }
    }

    suspend fun setContinuousScanMode(enabled: Boolean) {
        context.prismDataStore.edit { it[Keys.CONTINUOUS_SCAN_MODE] = enabled }
    }
}
