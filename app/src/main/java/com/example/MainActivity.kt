package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PrismScanDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.ScanRepository
import com.example.engine.FeedbackController
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.ScanResultBottomSheet
import com.example.ui.screens.BatchScannerScreen
import com.example.ui.screens.GeneratorStudioScreen
import com.example.ui.screens.HistoryVaultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.PrismScanTheme
import com.example.ui.viewmodel.PrismScanViewModel
import kotlinx.coroutines.flow.collectLatest

enum class PrismDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SCANNER(
        route = "scanner",
        label = "Scanner",
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_tab_scanner"
    ),
    BATCH(
        route = "batch",
        label = "Batch",
        selectedIcon = Icons.Filled.BurstMode,
        unselectedIcon = Icons.Outlined.BurstMode,
        testTag = "nav_tab_batch"
    ),
    STUDIO(
        route = "studio",
        label = "Studio",
        selectedIcon = Icons.Filled.AddBox,
        unselectedIcon = Icons.Outlined.AddBox,
        testTag = "nav_tab_studio"
    ),
    VAULT(
        route = "vault",
        label = "Vault",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_vault"
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrismScanAppRoot()
        }
    }
}

@Composable
fun PrismScanAppRoot(
    customViewModel: PrismScanViewModel? = null
) {
    val context = LocalContext.current.applicationContext
    val viewModel: PrismScanViewModel = customViewModel ?: viewModel(
        factory = remember {
            val db = PrismScanDatabase.getInstance(context)
            val scanRepo = ScanRepository(db.scanRecordDao())
            val settingsRepo = SettingsRepository(context)
            val feedback = FeedbackController(context)
            PrismScanViewModel.Factory(context, scanRepo, settingsRepo, feedback)
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val filteredVaultRecords by viewModel.filteredVaultRecords.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedVaultFilter by viewModel.selectedVaultFilter.collectAsStateWithLifecycle()
    val batchSessionId by viewModel.activeBatchSessionId.collectAsStateWithLifecycle()
    val currentBatchRecords by viewModel.currentBatchRecords.collectAsStateWithLifecycle()
    val activeScanRegion by viewModel.activeScanRegion.collectAsStateWithLifecycle()
    val isContinuousMode by viewModel.isContinuousScanActive.collectAsStateWithLifecycle()
    val lowLightBoostActive by viewModel.lowLightBoostActive.collectAsStateWithLifecycle()
    val ambientLuminance by viewModel.ambientLuminance.collectAsStateWithLifecycle()
    val liveDetections by viewModel.liveDetections.collectAsStateWithLifecycle()
    val inspectedRecord by viewModel.inspectedRecord.collectAsStateWithLifecycle()
    val latestScannedRecord by viewModel.latestScannedRecord.collectAsStateWithLifecycle()
    val productLookupState by viewModel.productLookupState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by rememberSaveable { mutableStateOf(PrismDestination.SCANNER) }

    LaunchedEffect(viewModel) {
        viewModel.uiMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = currentDestination != PrismDestination.SCANNER) {
        currentDestination = PrismDestination.SCANNER
    }

    PrismScanTheme(
        themeMode = settings.themeMode,
        accentPreset = settings.accentPreset,
        dynamicColor = settings.dynamicColorEnabled
    ) {
        val glass = LocalLiquidGlass.current

        LiquidGlassBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedScreen = maxWidth >= 640.dp

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        QryptTopGlassBar(
                            isDark = glass.isDark,
                            onToggleTheme = { viewModel.toggleDarkLightMode(glass.isDark) }
                        )
                    },
                    bottomBar = {
                        if (!isExpandedScreen) {
                            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = 4.dp,
                                        bottom = navBottomPadding + 8.dp
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .border(
                                        width = 1.2.dp,
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(
                                                glass.glassSpecularBorderTop,
                                                glass.primaryAccent.copy(alpha = 0.4f),
                                                glass.glassSpecularBorderBottom
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                            ) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    contentColor = glass.textPrimary,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                ) {
                                    PrismDestination.entries.forEach { dest ->
                                        val selected = currentDestination == dest
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentDestination = dest },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (dest == PrismDestination.BATCH && currentBatchRecords.isNotEmpty()) {
                                                            Badge { Text("${currentBatchRecords.size}") }
                                                        } else if (dest == PrismDestination.VAULT && allRecords.isNotEmpty()) {
                                                            Badge { Text("${allRecords.size}") }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = dest.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = glass.primaryAccent,
                                                selectedTextColor = glass.primaryAccent,
                                                unselectedIconColor = glass.textSecondary,
                                                unselectedTextColor = glass.textSecondary,
                                                indicatorColor = glass.primaryAccent.copy(alpha = 0.18f)
                                            ),
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isExpandedScreen) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                contentColor = glass.textPrimary,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                PrismDestination.entries.forEach { dest ->
                                    val selected = currentDestination == dest
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { currentDestination = dest },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = dest.label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }

                        Crossfade(
                            targetState = currentDestination,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            label = "mainTabCrossfade"
                        ) { destination ->
                            when (destination) {
                                PrismDestination.SCANNER -> {
                                    ScannerScreen(
                                        scanRegionMode = activeScanRegion,
                                        isContinuousMode = isContinuousMode,
                                        lowLightBoostActive = lowLightBoostActive,
                                        ambientLuminance = ambientLuminance,
                                        liveDetections = liveDetections,
                                        onScanRegionChange = viewModel::onScanRegionSelected,
                                        onToggleContinuousMode = viewModel::onToggleContinuousMode,
                                        onToggleLowLightBoost = viewModel::onToggleLowLightBoost,
                                        onFramesDetected = { frames ->
                                            viewModel.onCameraFramesDetected(frames, isBatchMode = false)
                                        },
                                        onLuminanceUpdated = viewModel::onLuminanceMeasured,
                                        onGalleryImageSelected = { uri ->
                                            viewModel.scanGalleryImageUri(uri, isBatchMode = false)
                                        },
                                        onTriggerSampleScan = { sample ->
                                            viewModel.triggerOpticalSampleScan(sample, isBatchMode = false)
                                        },
                                        latestScannedRecord = latestScannedRecord,
                                        onInspectLatestRecord = {
                                            latestScannedRecord?.let { viewModel.inspectRecord(it) }
                                        }
                                    )
                                }

                                PrismDestination.BATCH -> {
                                    BatchScannerScreen(
                                        batchSessionId = batchSessionId,
                                        batchRecords = currentBatchRecords,
                                        duplicatePreventionEnabled = settings.duplicatePreventionEnabled,
                                        duplicateCooldownSec = settings.duplicateCooldownSeconds,
                                        onStartNewSession = viewModel::startNewBatchSession,
                                        onTriggerSampleBatchScan = { sample ->
                                            viewModel.triggerOpticalSampleScan(sample, isBatchMode = true)
                                        },
                                        onScanGalleryBatch = { uri ->
                                            viewModel.scanGalleryImageUri(uri, isBatchMode = true)
                                        },
                                        onInspectRecord = viewModel::inspectRecord,
                                        onDeleteRecord = viewModel::deleteRecord,
                                        onAnnotateBatch = viewModel::annotateCurrentBatch,
                                        onClearBatch = viewModel::clearCurrentBatchItems,
                                        onShowMessage = viewModel::emitMessage
                                    )
                                }

                                PrismDestination.STUDIO -> {
                                    GeneratorStudioScreen(
                                        onSaveToVault = viewModel::saveGeneratedBarcodeToVault,
                                        onShowMessage = viewModel::emitMessage
                                    )
                                }

                                PrismDestination.VAULT -> {
                                    HistoryVaultScreen(
                                        allRecordsCount = allRecords.size,
                                        filteredRecords = filteredVaultRecords,
                                        searchQuery = searchQuery,
                                        selectedFilter = selectedVaultFilter,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onSelectFilter = viewModel::selectVaultFilter,
                                        onInspectRecord = viewModel::inspectRecord,
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onDeleteRecord = viewModel::deleteRecord,
                                        onShowMessage = viewModel::emitMessage
                                    )
                                }

                                PrismDestination.SETTINGS -> {
                                    SettingsScreen(
                                        settings = settings,
                                        totalStoredRecords = allRecords.size,
                                        onThemeModeChange = viewModel::setThemeMode,
                                        onDynamicColorChange = viewModel::setDynamicColor,
                                        onAccentPresetChange = viewModel::setAccentPreset,
                                        onHapticChange = viewModel::setHapticFeedback,
                                        onSoundChange = viewModel::setSoundFeedback,
                                        onDuplicatePreventionChange = viewModel::setDuplicatePrevention,
                                        onDuplicateCooldownChange = viewModel::setDuplicateCooldownSeconds,
                                        onAutoCopyChange = viewModel::setAutoCopy,
                                        onAutoOpenSafeUrlsChange = viewModel::setAutoOpenSafeUrls,
                                        onLowLightAutoBoostChange = viewModel::setLowLightAutoBoost,
                                        onDefaultScanRegionChange = viewModel::setDefaultScanRegion,
                                        onContinuousModeChange = viewModel::setContinuousScanMode,
                                        onClearNonFavorites = viewModel::clearNonFavoriteHistory,
                                        onPurgeAllHistory = viewModel::purgeAllHistory,
                                        onAutoProductLookupChange = viewModel::setAutomaticProductLookup
                                    )
                                }
                            }
                        }
                    }
                }

                inspectedRecord?.let { record ->
                    ScanResultBottomSheet(
                        record = record,
                        productLookupState = productLookupState,
                        onRetryProductLookup = viewModel::lookupProductForRecord,
                        onDismiss = { viewModel.inspectRecord(null) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onUpdateNotes = viewModel::updateRecordNotes,
                        onDelete = viewModel::deleteRecord,
                        onShowMessage = viewModel::emitMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun QryptTopGlassBar(
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val glass = LocalLiquidGlass.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val systemTopClearance = maxOf(statusTop, cutoutTop, safeTop, 36.dp)

    val borderBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
        listOf(
            glass.glassSpecularBorderTop,
            glass.primaryAccent.copy(alpha = 0.45f),
            glass.glassSpecularBorderBottom
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = systemTopClearance + 10.dp,
                bottom = 6.dp
            )
            .testTag("top_header_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(glass.glassSurfaceTop, glass.glassSurfaceBottom)
                    )
                )
                .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    glass.primaryAccent.copy(alpha = 0.22f),
                                    glass.secondaryAccent.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(1.dp, glass.primaryAccent.copy(alpha = 0.60f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = glass.primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "QRYPT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary,
                        modifier = Modifier.testTag("top_header_app_name")
                    )
                    Text(
                        text = "Scan Smarter. Instantly.",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.primaryAccent
                    )
                }
            }

            // Instant 1-Tap Dark / Light Mode Switcher Pill
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(glass.primaryAccent.copy(alpha = if (isDark) 0.18f else 0.14f))
                    .border(1.2.dp, glass.primaryAccent.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("top_theme_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = glass.primaryAccent,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = if (isDark) "Dark" else "Light",
                    style = MaterialTheme.typography.labelLarge,
                    color = glass.textPrimary
                )
            }
        }
    }
}
