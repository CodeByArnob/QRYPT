package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PrismScanDatabase
import com.example.data.local.ScanRecordEntity
import com.example.data.preferences.AccentColorPreset
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.UserScanSettings
import com.example.data.repository.AndroidNetworkConnectivityChecker
import com.example.data.repository.ProductLookupRepository
import com.example.data.repository.ScanRepository
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.BarcodeValidator
import com.example.domain.model.DetectedBarcodeFrame
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.ProductLookupState
import com.example.domain.model.ScanRegionMode
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.parser.PayloadParser
import com.example.engine.BarcodeGeneratorEngine
import com.example.engine.BarcodeScannerEngine
import com.example.engine.FeedbackController
import com.example.engine.SampleTestBarcode
import com.example.util.ActionIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

enum class VaultFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    SCANNED("Scanned"),
    GENERATED("Created"),
    URLS("URLs"),
    WIFI("Wi-Fi"),
    PRODUCTS("Products & ISBN"),
    CONTACTS("Contacts"),
    SUSPICIOUS("Security Alerts")
}

enum class GeneratorTemplate(val label: String, val defaultFormat: BarcodeFormatType) {
    URL("Website URL", BarcodeFormatType.QR_CODE),
    WIFI("Wi-Fi Network", BarcodeFormatType.QR_CODE),
    VCARD("Contact vCard", BarcodeFormatType.QR_CODE),
    PLAIN_TEXT("Plain Text", BarcodeFormatType.QR_CODE),
    PRODUCT_GTIN("Retail Barcode", BarcodeFormatType.EAN_13),
    EMAIL("Email Message", BarcodeFormatType.QR_CODE),
    SMS("SMS Text", BarcodeFormatType.QR_CODE),
    PHONE("Phone Call", BarcodeFormatType.QR_CODE),
    GEO("Geo Location", BarcodeFormatType.QR_CODE),
    CALENDAR("Calendar Event", BarcodeFormatType.QR_CODE)
}

class PrismScanViewModel(
    private val appContext: Context,
    private val scanRepository: ScanRepository,
    private val settingsRepository: SettingsRepository,
    private val feedbackController: FeedbackController,
    private val productLookupRepository: ProductLookupRepository = ProductLookupRepository(
        productCacheDao = PrismScanDatabase.getInstance(appContext).productCacheDao(),
        connectivityChecker = AndroidNetworkConnectivityChecker(appContext)
    )
) : ViewModel() {

    private val _settings = MutableStateFlow(UserScanSettings())
    val settings: StateFlow<UserScanSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { loaded ->
                _settings.value = loaded
            }
        }
    }

    val allRecords: StateFlow<List<ScanRecordEntity>> = scanRepository.allRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Toast / Snackbar messages
    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val uiMessage = _uiMessage.asSharedFlow()

    fun emitMessage(message: String) {
        _uiMessage.tryEmit(message)
    }

    // Scanner Live States
    private val _liveDetections = MutableStateFlow<List<DetectedBarcodeFrame>>(emptyList())
    val liveDetections: StateFlow<List<DetectedBarcodeFrame>> = _liveDetections.asStateFlow()

    private val _ambientLuminance = MutableStateFlow(130f)
    val ambientLuminance: StateFlow<Float> = _ambientLuminance.asStateFlow()

    private val _lowLightBoostActive = MutableStateFlow(false)
    val lowLightBoostActive: StateFlow<Boolean> = _lowLightBoostActive.asStateFlow()

    private val _scanRegionOverride = MutableStateFlow<ScanRegionMode?>(null)
    val activeScanRegion: StateFlow<ScanRegionMode> = combine(
        _scanRegionOverride,
        settings
    ) { override, prefs ->
        override ?: prefs.defaultScanRegion
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanRegionMode.PRECISION_SQUARE)

    private val _continuousModeOverride = MutableStateFlow<Boolean?>(null)
    val isContinuousScanActive: StateFlow<Boolean> = combine(
        _continuousModeOverride,
        settings
    ) { override, prefs ->
        override ?: prefs.continuousScanMode
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Currently inspected scan result in bottom sheet
    private val _inspectedRecord = MutableStateFlow<ScanRecordEntity?>(null)
    val inspectedRecord: StateFlow<ScanRecordEntity?> = _inspectedRecord.asStateFlow()

    // Latest scanned record for quick access banner on ScannerScreen
    private val _latestScannedRecord = MutableStateFlow<ScanRecordEntity?>(null)
    val latestScannedRecord: StateFlow<ScanRecordEntity?> = _latestScannedRecord.asStateFlow()

    // Product identification lookup state for the currently inspected record
    private val _productLookupState = MutableStateFlow<ProductLookupState>(ProductLookupState.Idle)
    val productLookupState: StateFlow<ProductLookupState> = _productLookupState.asStateFlow()

    private var activeProductLookupJob: Job? = null

    // Batch Scan Session State
    private val _activeBatchSessionId = MutableStateFlow(UUID.randomUUID().toString().take(8).uppercase(Locale.US))
    val activeBatchSessionId: StateFlow<String> = _activeBatchSessionId.asStateFlow()

    val currentBatchRecords: StateFlow<List<ScanRecordEntity>> = combine(
        allRecords,
        _activeBatchSessionId
    ) { records, sessionId ->
        records.filter { it.batchSessionId == sessionId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVaultFilter = MutableStateFlow(VaultFilter.ALL)
    val selectedVaultFilter: StateFlow<VaultFilter> = _selectedVaultFilter.asStateFlow()

    val filteredVaultRecords: StateFlow<List<ScanRecordEntity>> = combine(
        allRecords,
        _searchQuery,
        _selectedVaultFilter
    ) { records, query, filter ->
        val trimmedQuery = query.trim().lowercase(Locale.US)
        records.filter { r ->
            val matchesFilter = when (filter) {
                VaultFilter.ALL -> true
                VaultFilter.FAVORITES -> r.isFavorite
                VaultFilter.SCANNED -> !r.isGenerated
                VaultFilter.GENERATED -> r.isGenerated
                VaultFilter.URLS -> r.payloadType == ParsedPayloadType.URL
                VaultFilter.WIFI -> r.payloadType == ParsedPayloadType.WIFI
                VaultFilter.PRODUCTS -> r.payloadType == ParsedPayloadType.PRODUCT || r.payloadType == ParsedPayloadType.ISBN
                VaultFilter.CONTACTS -> r.payloadType == ParsedPayloadType.CONTACT_VCARD
                VaultFilter.SUSPICIOUS -> r.securityRisk != SecurityRiskLevel.SAFE
            }
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                r.rawValue.lowercase(Locale.US).contains(trimmedQuery) ||
                    r.primaryTitle.lowercase(Locale.US).contains(trimmedQuery) ||
                    r.subtitle.lowercase(Locale.US).contains(trimmedQuery) ||
                    r.format.displayName.lowercase(Locale.US).contains(trimmedQuery) ||
                    r.customNotes.lowercase(Locale.US).contains(trimmedQuery)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastCapturedRawValue: String = ""
    private var lastCaptureTimeMs: Long = 0L

    fun onScanRegionSelected(mode: ScanRegionMode) {
        _scanRegionOverride.value = mode
    }

    fun onToggleContinuousMode() {
        _continuousModeOverride.value = !isContinuousScanActive.value
    }

    fun onToggleLowLightBoost() {
        _lowLightBoostActive.value = !_lowLightBoostActive.value
    }

    fun onLuminanceMeasured(luma: Float) {
        _ambientLuminance.value = luma
        if (settings.value.lowLightAutoBoost && luma < 45f && !_lowLightBoostActive.value) {
            _lowLightBoostActive.value = true
        }
    }

    fun inspectRecord(record: ScanRecordEntity?) {
        _inspectedRecord.value = record
        activeProductLookupJob?.cancel()
        if (record == null) {
            _productLookupState.value = ProductLookupState.Idle
            return
        }
        val isProductCandidate = BarcodeValidator.isProductBarcodeCandidate(
            rawValue = record.rawValue,
            format = record.format,
            payloadType = record.payloadType
        )
        if (isProductCandidate && settings.value.automaticProductLookup) {
            lookupProductForRecord(record, forceRefresh = false)
        } else {
            _productLookupState.value = ProductLookupState.Idle
        }
    }

    fun lookupProductForRecord(record: ScanRecordEntity, forceRefresh: Boolean = false) {
        activeProductLookupJob?.cancel()
        activeProductLookupJob = viewModelScope.launch {
            _productLookupState.value = ProductLookupState.Loading(
                barcode = record.rawValue.trim(),
                formatDisplayName = record.format.displayName
            )
            val resultState = productLookupRepository.lookupProduct(
                rawBarcode = record.rawValue,
                format = record.format,
                forceRefresh = forceRefresh
            )
            _productLookupState.value = resultState

            if (resultState is ProductLookupState.Success) {
                val product = resultState.product
                val enrichedSubtitle = listOfNotNull(
                    product.brand.takeIf { it.isNotBlank() },
                    product.quantity.takeIf { it.isNotBlank() },
                    record.format.displayName
                ).joinToString(" | ").ifEmpty { record.subtitle }

                scanRepository.updateTitleAndSubtitle(
                    id = record.id,
                    title = product.productName,
                    subtitle = enrichedSubtitle
                )
                if (_inspectedRecord.value?.id == record.id) {
                    _inspectedRecord.value = _inspectedRecord.value?.copy(
                        primaryTitle = product.productName,
                        subtitle = enrichedSubtitle
                    )
                }
                if (_latestScannedRecord.value?.id == record.id) {
                    _latestScannedRecord.value = _latestScannedRecord.value?.copy(
                        primaryTitle = product.productName,
                        subtitle = enrichedSubtitle
                    )
                }
            }
        }
    }

    private fun enrichBatchProductInBackground(record: ScanRecordEntity) {
        if (!settings.value.automaticProductLookup) return
        val isProductCandidate = BarcodeValidator.isProductBarcodeCandidate(
            rawValue = record.rawValue,
            format = record.format,
            payloadType = record.payloadType
        )
        if (!isProductCandidate) return
        viewModelScope.launch {
            val result = productLookupRepository.lookupProduct(
                rawBarcode = record.rawValue,
                format = record.format,
                forceRefresh = false
            )
            if (result is ProductLookupState.Success) {
                val product = result.product
                val enrichedSubtitle = listOfNotNull(
                    product.brand.takeIf { it.isNotBlank() },
                    product.quantity.takeIf { it.isNotBlank() },
                    record.format.displayName
                ).joinToString(" | ").ifEmpty { record.subtitle }
                scanRepository.updateTitleAndSubtitle(record.id, product.productName, enrichedSubtitle)
            }
        }
    }

    fun onCameraFramesDetected(
        frames: List<DetectedBarcodeFrame>,
        isBatchMode: Boolean = false
    ) {
        _liveDetections.value = frames
        if (frames.isEmpty()) return

        // If single-scan sheet is already open and not in continuous/batch mode, wait until dismissed
        if (!isBatchMode && !isContinuousScanActive.value && _inspectedRecord.value != null) {
            return
        }

        val primary = frames.first()
        val now = System.currentTimeMillis()
        val currentSettings = settings.value
        val cooldownMs = if (currentSettings.duplicatePreventionEnabled) {
            currentSettings.duplicateCooldownSeconds * 1000L
        } else {
            900L
        }

        if (primary.rawValue == lastCapturedRawValue && (now - lastCaptureTimeMs) < cooldownMs) {
            return
        }

        lastCapturedRawValue = primary.rawValue
        lastCaptureTimeMs = now

        viewModelScope.launch {
            val saved = scanRepository.recordScan(
                rawValue = primary.rawValue,
                format = primary.format,
                isGenerated = false,
                batchSessionId = if (isBatchMode) _activeBatchSessionId.value else null,
                duplicatePreventionEnabled = currentSettings.duplicatePreventionEnabled,
                duplicateCooldownSeconds = currentSettings.duplicateCooldownSeconds
            )
            if (saved != null) {
                _latestScannedRecord.value = saved
                val isRisky = saved.securityRisk != SecurityRiskLevel.SAFE
                feedbackController.triggerScanSuccess(
                    hapticEnabled = currentSettings.hapticFeedbackEnabled,
                    soundEnabled = currentSettings.soundFeedbackEnabled,
                    isWarning = isRisky
                )

                if (currentSettings.autoCopyToClipboard) {
                    ActionIntentHelper.copyToClipboard(appContext, saved.primaryTitle, saved.rawValue)
                }

                if (isBatchMode) {
                    enrichBatchProductInBackground(saved)
                    emitMessage("Batch +1: ${saved.primaryTitle}")
                } else if (isContinuousScanActive.value) {
                    enrichBatchProductInBackground(saved)
                    emitMessage("Captured [${saved.format.shortTag}]: ${saved.primaryTitle}")
                } else {
                    // Never automatically launch external links without user interaction.
                    // Always open the ScanResultBottomSheet with context-aware actions.
                    inspectRecord(saved)
                }
            }
        }
    }

    fun scanGalleryImageUri(uri: Uri, isBatchMode: Boolean = false) {
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    appContext.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                    var sampleSize = 1
                    while (options.outWidth / sampleSize > 1600 || options.outHeight / sampleSize > 1600) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bitmap = appContext.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, decodeOpts)
                    }
                    if (bitmap != null) {
                        BarcodeScannerEngine.decodeBitmap(bitmap)
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            if (decoded.isEmpty()) {
                emitMessage("No recognizable barcode or QR code found in selected image")
            } else {
                val currentSettings = settings.value
                var firstSaved: ScanRecordEntity? = null
                for (frame in decoded) {
                    val saved = scanRepository.recordScan(
                        rawValue = frame.rawValue,
                        format = frame.format,
                        isGenerated = false,
                        batchSessionId = if (isBatchMode) _activeBatchSessionId.value else null,
                        duplicatePreventionEnabled = false
                    )
                    if (saved != null) {
                        if (isBatchMode) {
                            enrichBatchProductInBackground(saved)
                        }
                        if (firstSaved == null) {
                            firstSaved = saved
                        }
                    }
                }
                if (firstSaved != null) {
                    _latestScannedRecord.value = firstSaved
                }
                feedbackController.triggerScanSuccess(
                    hapticEnabled = currentSettings.hapticFeedbackEnabled,
                    soundEnabled = currentSettings.soundFeedbackEnabled,
                    isWarning = firstSaved?.securityRisk != SecurityRiskLevel.SAFE
                )
                if (!isBatchMode && firstSaved != null) {
                    inspectRecord(firstSaved)
                }
                emitMessage("Decoded ${decoded.size} barcode(s) from image")
            }
        }
    }

    /**
     * Renders a real barcode bitmap from a sample preset and runs it through the optical
     * decoder pipeline to verify end-to-end scanning on any device or browser emulator.
     */
    fun triggerOpticalSampleScan(sample: SampleTestBarcode, isBatchMode: Boolean = false) {
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.Default) {
                val bmp = BarcodeScannerEngine.renderSampleBitmap(sample)
                if (bmp != null) {
                    BarcodeScannerEngine.decodeBitmap(bmp)
                } else {
                    emptyList()
                }
            }
            val detected = decoded.firstOrNull() ?: DetectedBarcodeFrame(
                rawValue = sample.payload,
                format = sample.format,
                parsedPayload = PayloadParser.parse(sample.payload, sample.format),
                normalizedBox = null
            )

            val saved = scanRepository.recordScan(
                rawValue = detected.rawValue,
                format = detected.format,
                isGenerated = false,
                batchSessionId = if (isBatchMode) _activeBatchSessionId.value else null,
                duplicatePreventionEnabled = false
            )
            if (saved != null) {
                _latestScannedRecord.value = saved
                val currentSettings = settings.value
                feedbackController.triggerScanSuccess(
                    hapticEnabled = currentSettings.hapticFeedbackEnabled,
                    soundEnabled = currentSettings.soundFeedbackEnabled,
                    isWarning = saved.securityRisk != SecurityRiskLevel.SAFE
                )
                if (isBatchMode) {
                    enrichBatchProductInBackground(saved)
                    emitMessage("Batch +1 [${saved.format.shortTag}]: ${saved.primaryTitle}")
                } else {
                    inspectRecord(saved)
                }
            }
        }
    }

    // Batch Controls
    fun startNewBatchSession() {
        _activeBatchSessionId.value = UUID.randomUUID().toString().take(8).uppercase(Locale.US)
        emitMessage("Started new batch session #${_activeBatchSessionId.value}")
    }

    fun annotateCurrentBatch(tagNote: String) {
        val clean = tagNote.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val items = currentBatchRecords.value
            for (item in items) {
                scanRepository.updateNotes(item.id, clean)
            }
            emitMessage("Applied note '$clean' to ${items.size} batch items")
        }
    }

    fun clearCurrentBatchItems() {
        viewModelScope.launch {
            val items = currentBatchRecords.value
            for (item in items) {
                scanRepository.deleteRecord(item.id)
            }
            emitMessage("Cleared ${items.size} items from current batch")
        }
    }

    // Vault Operations
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectVaultFilter(filter: VaultFilter) {
        _selectedVaultFilter.value = filter
    }

    fun toggleFavorite(record: ScanRecordEntity) {
        viewModelScope.launch {
            val next = !record.isFavorite
            scanRepository.toggleFavorite(record.id, next)
            if (_inspectedRecord.value?.id == record.id) {
                _inspectedRecord.value = record.copy(isFavorite = next)
            }
        }
    }

    fun updateRecordNotes(record: ScanRecordEntity, notes: String) {
        viewModelScope.launch {
            scanRepository.updateNotes(record.id, notes)
            if (_inspectedRecord.value?.id == record.id) {
                _inspectedRecord.value = record.copy(customNotes = notes)
            }
        }
    }

    fun deleteRecord(record: ScanRecordEntity) {
        viewModelScope.launch {
            scanRepository.deleteRecord(record.id)
            if (_inspectedRecord.value?.id == record.id) {
                inspectRecord(null)
            }
            if (_latestScannedRecord.value?.id == record.id) {
                _latestScannedRecord.value = null
            }
            emitMessage("Removed '${record.primaryTitle}' from Vault")
        }
    }

    fun clearNonFavoriteHistory() {
        viewModelScope.launch {
            scanRepository.clearNonFavorites()
            emitMessage("Cleared non-favorite scan history")
        }
    }

    fun purgeAllHistory() {
        viewModelScope.launch {
            scanRepository.clearAllHistory()
            productLookupRepository.clearCache()
            inspectRecord(null)
            _latestScannedRecord.value = null
            emitMessage("Optical Vault completely purged")
        }
    }

    fun saveGeneratedBarcodeToVault(
        rawValue: String,
        format: BarcodeFormatType,
        customNotes: String = ""
    ) {
        viewModelScope.launch {
            val validated = BarcodeGeneratorEngine.validateAndNormalize(rawValue, format)
            val normalized = validated.getOrElse {
                emitMessage(it.message ?: "Invalid payload")
                return@launch
            }
            val saved = scanRepository.recordScan(
                rawValue = normalized,
                format = format,
                isGenerated = true,
                customNotes = customNotes,
                duplicatePreventionEnabled = false
            )
            if (saved != null) {
                emitMessage("Saved ${format.displayName} to Vault")
            }
        }
    }

    // Settings Operations (Immediate 0ms UI update + async DataStore persistence)
    fun setThemeMode(mode: AppThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun toggleDarkLightMode(currentlyDark: Boolean) {
        val next = if (currentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(next)
    }

    fun setDynamicColor(enabled: Boolean) {
        _settings.value = _settings.value.copy(dynamicColorEnabled = enabled)
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setAccentPreset(preset: AccentColorPreset) {
        _settings.value = _settings.value.copy(accentPreset = preset)
        viewModelScope.launch { settingsRepository.setAccentPreset(preset) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        _settings.value = _settings.value.copy(hapticFeedbackEnabled = enabled)
        viewModelScope.launch { settingsRepository.setHapticFeedback(enabled) }
    }

    fun setSoundFeedback(enabled: Boolean) {
        _settings.value = _settings.value.copy(soundFeedbackEnabled = enabled)
        viewModelScope.launch { settingsRepository.setSoundFeedback(enabled) }
    }

    fun setDuplicatePrevention(enabled: Boolean) {
        _settings.value = _settings.value.copy(duplicatePreventionEnabled = enabled)
        viewModelScope.launch { settingsRepository.setDuplicatePrevention(enabled) }
    }

    fun setDuplicateCooldownSeconds(sec: Int) {
        _settings.value = _settings.value.copy(duplicateCooldownSeconds = sec.coerceIn(1, 15))
        viewModelScope.launch { settingsRepository.setDuplicateCooldownSeconds(sec) }
    }

    fun setAutoCopy(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoCopyToClipboard = enabled)
        viewModelScope.launch { settingsRepository.setAutoCopy(enabled) }
    }

    fun setAutoOpenSafeUrls(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoOpenSafeUrls = enabled)
        viewModelScope.launch { settingsRepository.setAutoOpenSafeUrls(enabled) }
    }

    fun setAutomaticProductLookup(enabled: Boolean) {
        _settings.value = _settings.value.copy(automaticProductLookup = enabled)
        viewModelScope.launch { settingsRepository.setAutomaticProductLookup(enabled) }
    }

    fun setLowLightAutoBoost(enabled: Boolean) {
        _settings.value = _settings.value.copy(lowLightAutoBoost = enabled)
        viewModelScope.launch { settingsRepository.setLowLightAutoBoost(enabled) }
    }

    fun setDefaultScanRegion(region: ScanRegionMode) {
        _settings.value = _settings.value.copy(defaultScanRegion = region)
        viewModelScope.launch { settingsRepository.setDefaultScanRegion(region) }
    }

    fun setContinuousScanMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(continuousScanMode = enabled)
        viewModelScope.launch { settingsRepository.setContinuousScanMode(enabled) }
    }

    class Factory(
        private val appContext: Context,
        private val scanRepository: ScanRepository,
        private val settingsRepository: SettingsRepository,
        private val feedbackController: FeedbackController,
        private val productLookupRepository: ProductLookupRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val lookupRepo = productLookupRepository ?: ProductLookupRepository(
                productCacheDao = PrismScanDatabase.getInstance(appContext).productCacheDao(),
                connectivityChecker = AndroidNetworkConnectivityChecker(appContext)
            )
            return PrismScanViewModel(
                appContext = appContext,
                scanRepository = scanRepository,
                settingsRepository = settingsRepository,
                feedbackController = feedbackController,
                productLookupRepository = lookupRepo
            ) as T
        }
    }
}
