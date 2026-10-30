package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.DetectedBarcodeFrame
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.ScanRegionMode
import com.example.engine.BarcodeScannerEngine
import com.example.engine.SampleTestBarcode
import com.example.ui.components.FormatBadge
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlass
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    scanRegionMode: ScanRegionMode,
    isContinuousMode: Boolean,
    lowLightBoostActive: Boolean,
    ambientLuminance: Float,
    liveDetections: List<DetectedBarcodeFrame>,
    onScanRegionChange: (ScanRegionMode) -> Unit,
    onToggleContinuousMode: () -> Unit,
    onToggleLowLightBoost: () -> Unit,
    onFramesDetected: (List<DetectedBarcodeFrame>) -> Unit,
    onLuminanceUpdated: (Float) -> Unit,
    onGalleryImageSelected: (android.net.Uri) -> Unit,
    onTriggerSampleScan: (SampleTestBarcode) -> Unit,
    latestScannedRecord: ScanRecordEntity? = null,
    onInspectLatestRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onGalleryImageSelected(uri)
        }
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(5f) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var showSampleTestSheet by remember { mutableStateOf(false) }
    var cameraErrorText by remember { mutableStateOf<String?>(null) }

    // Clear tap-to-focus reticle after 900ms
    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            kotlinx.coroutines.delay(900L)
            focusTapOffset = null
        }
    }

    if (showSampleTestSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSampleTestSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = glass.primaryAccent
                    )
                    Column {
                        Text(
                            text = "Optical Engine Self-Test Presets",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Renders a real barcode bitmap and decodes it through the dual-engine optical pipeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(BarcodeScannerEngine.sampleOpticalTestPresets, key = { it.id }) { sample ->
                        LiquidGlassCard(
                            onClick = {
                                showSampleTestSheet = false
                                onTriggerSampleScan(sample)
                            },
                            contentPadding = PaddingValues(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sample_preset_${sample.id}")
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
                                        FormatBadge(format = sample.format)
                                        Text(
                                            text = sample.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sample.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Decode Preset",
                                    tint = glass.primaryAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TopOpticsHudSection() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(glass.primaryAccent)
                            )
                            Text(
                                text = "QRYPT OPTICS",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.primaryAccent
                            )
                        }
                        Text(
                            text = if (isContinuousMode) "Continuous Stream" else scanRegionMode.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(
                            icon = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            active = isTorchOn,
                            onClick = { isTorchOn = !isTorchOn },
                            modifier = Modifier.testTag("scanner_torch_button")
                        )
                        GlassIconButton(
                            icon = Icons.Default.NightlightRound,
                            contentDescription = "Low-Light Boost",
                            active = lowLightBoostActive,
                            onClick = onToggleLowLightBoost,
                            modifier = Modifier.testTag("scanner_low_light_button")
                        )
                        GlassIconButton(
                            icon = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            active = lensFacing == CameraSelector.LENS_FACING_FRONT,
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier.testTag("scanner_switch_camera_button")
                        )
                    }
                }
            }

            // Scan Region & Mode Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScanRegionMode.entries.forEach { mode ->
                    LiquidGlassPill(
                        text = mode.title,
                        selected = scanRegionMode == mode,
                        onClick = { onScanRegionChange(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("region_mode_${mode.name}")
                    )
                }
            }

            // Low-Light Telemetry Banner when ambient luma is low or camera error occurred
            AnimatedVisibility(visible = ambientLuminance < 52f || cameraErrorText != null) {
                LiquidGlassCard(
                    accentTint = CautionAmber,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cameraErrorText
                                ?: "Low-light environment detected (${ambientLuminance.toInt()} lux). Enable Torch or Low-Light Boost.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun BottomScannerControlsSection() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            latestScannedRecord?.let { latest ->
                LiquidGlassCard(
                    onClick = onInspectLatestRecord,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_latest_result_banner"),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FormatBadge(format = latest.format)
                                Text(
                                    text = latest.payloadType.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.primaryAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latest.primaryTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = onInspectLatestRecord,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("scanner_open_latest_result_button")
                        ) {
                            Text(
                                text = when (latest.payloadType) {
                                    ParsedPayloadType.URL -> "Open Link"
                                    ParsedPayloadType.PRODUCT, ParsedPayloadType.ISBN -> "View Product"
                                    else -> "Open Result"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            if (hasCameraPermission) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Optical Zoom",
                            tint = glass.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1fx", zoomRatio),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = glass.primaryAccent
                        )
                        Slider(
                            value = zoomRatio,
                            onValueChange = { zoomRatio = it },
                            valueRange = 1f..maxZoomRatio.coerceAtLeast(2f),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("scanner_zoom_slider")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassPill(
                    text = "Scan Image",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scanner_gallery_button")
                )

                LiquidGlassPill(
                    text = if (isContinuousMode) "Continuous: ON" else "Continuous",
                    icon = Icons.Default.AllInclusive,
                    selected = isContinuousMode,
                    onClick = onToggleContinuousMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scanner_continuous_button")
                )

                LiquidGlassPill(
                    text = "Test Sample",
                    icon = Icons.Default.AutoAwesome,
                    onClick = { showSampleTestSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scanner_sample_test_button")
                )
            }
        }
    }

    if (hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraPreviewWithAnalyzer(
                lensFacing = lensFacing,
                isTorchOn = isTorchOn,
                zoomRatio = zoomRatio,
                lowLightBoost = lowLightBoostActive,
                scanRegionMode = scanRegionMode,
                onZoomBoundsUpdated = { current, max ->
                    zoomRatio = current
                    maxZoomRatio = max.coerceAtLeast(2f)
                },
                onFocusTapped = { offset -> focusTapOffset = offset },
                onFramesDetected = onFramesDetected,
                onLuminanceUpdated = onLuminanceUpdated,
                onCameraError = { err -> cameraErrorText = err }
            )

            ScannerViewfinderOverlay(
                scanRegionMode = scanRegionMode,
                liveDetections = liveDetections,
                focusTapOffset = focusTapOffset
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TopOpticsHudSection()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BottomScannerControlsSection()
            }
        }
    } else {
        // Scrollable responsive layout for permission request / offline gallery & self-test mode
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopOpticsHudSection()

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(glass.primaryAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = glass.primaryAccent,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        text = "QRYPT Optical Access",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Scan Smarter. Instantly. QRYPT decodes all 13+ 1D & 2D barcode formats 100% locally on your device. Grant camera access for real-time scanning, or scan directly from your photo library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_camera_permission_button")
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Real-Time Camera")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("permission_gallery_scan_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photo Library", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showSampleTestSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("permission_sample_scan_button")
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Self-Test", maxLines = 1)
                        }
                    }
                }
            }

            BottomScannerControlsSection()
        }
    }
}

@Composable
private fun CameraPreviewWithAnalyzer(
    lensFacing: Int,
    isTorchOn: Boolean,
    zoomRatio: Float,
    lowLightBoost: Boolean,
    scanRegionMode: ScanRegionMode,
    onZoomBoundsUpdated: (Float, Float) -> Unit,
    onFocusTapped: (Offset) -> Unit,
    onFramesDetected: (List<DetectedBarcodeFrame>) -> Unit,
    onLuminanceUpdated: (Float) -> Unit,
    onCameraError: (String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val analyzer = remember {
        BarcodeScannerEngine(
            onBarcodesDetected = onFramesDetected,
            onLuminanceUpdated = onLuminanceUpdated
        )
    }

    LaunchedEffect(scanRegionMode) {
        analyzer.scanRegionMode = scanRegionMode
    }

    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    // Bind CameraX use cases on lensFacing or lifecycle changes
    DisposableEffect(lifecycleOwner, lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, analyzer)
                    }

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
                activeCamera = cam
                onCameraError(null)
                val zoomState = cam.cameraInfo.zoomState.value
                if (zoomState != null) {
                    onZoomBoundsUpdated(zoomState.zoomRatio, zoomState.maxZoomRatio)
                }
            } catch (e: Exception) {
                onCameraError("Camera stream unavailable in current environment. Use Scan Image or Test Sample.")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }

    // Apply Torch, Zoom, and Low-Light Exposure Compensation
    LaunchedEffect(activeCamera, isTorchOn, zoomRatio, lowLightBoost) {
        val cam = activeCamera ?: return@LaunchedEffect
        try {
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(isTorchOn)
            }
            cam.cameraControl.setZoomRatio(zoomRatio)

            val exposureState = cam.cameraInfo.exposureState
            if (exposureState.isExposureCompensationSupported) {
                val targetIndex = if (lowLightBoost) {
                    (exposureState.exposureCompensationRange.upper / 2).coerceAtLeast(0)
                } else {
                    0
                }
                cam.cameraControl.setExposureCompensationIndex(targetIndex)
            }
        } catch (_: Exception) {
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val cam = activeCamera ?: return@detectTransformGestures
                    val current = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                    val max = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 5f
                    val next = (current * zoomChange).coerceIn(1f, max)
                    onZoomBoundsUpdated(next, max)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    onFocusTapped(tapOffset)
                    val cam = activeCamera ?: return@detectTapGestures
                    try {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(tapOffset.x, tapOffset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        cam.cameraControl.startFocusAndMetering(action)
                    } catch (_: Exception) {
                    }
                }
            }
    )
}

@Composable
private fun ScannerViewfinderOverlay(
    scanRegionMode: ScanRegionMode,
    liveDetections: List<DetectedBarcodeFrame>,
    focusTapOffset: Offset?
) {
    val glass = LocalLiquidGlass.current
    val infiniteTransition = rememberInfiniteTransition(label = "laserSweep")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLine"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val roi = scanRegionMode.toNormalizedRect()
        val rectLeft = roi.left * w
        val rectTop = roi.top * h
        val rectWidth = roi.width() * w
        val rectHeight = roi.height() * h

        // Darken outer mask using native layer for clear cutout
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            drawRect(color = Color(0x88040812))
            drawRoundRect(
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
            restoreToCount(checkPoint)
        }

        // Subtle glass border around active scan region
        drawRoundRect(
            topLeft = Offset(rectLeft, rectTop),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Glowing corner brackets
        val bracketLen = 32.dp.toPx()
        val strokeW = 4.dp.toPx()
        val cornerPath = Path().apply {
            // Top-left
            moveTo(rectLeft, rectTop + bracketLen)
            lineTo(rectLeft, rectTop)
            lineTo(rectLeft + bracketLen, rectTop)
            // Top-right
            moveTo(rectLeft + rectWidth - bracketLen, rectTop)
            lineTo(rectLeft + rectWidth, rectTop)
            lineTo(rectLeft + rectWidth, rectTop + bracketLen)
            // Bottom-left
            moveTo(rectLeft, rectTop + rectHeight - bracketLen)
            lineTo(rectLeft, rectTop + rectHeight)
            lineTo(rectLeft + bracketLen, rectTop + rectHeight)
            // Bottom-right
            moveTo(rectLeft + rectWidth - bracketLen, rectTop + rectHeight)
            lineTo(rectLeft + rectWidth, rectTop + rectHeight)
            lineTo(rectLeft + rectWidth, rectTop + rectHeight - bracketLen)
        }
        drawPath(
            path = cornerPath,
            color = glass.primaryAccent,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Animated laser scan beam inside ROI
        val laserY = rectTop + (rectHeight * laserProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    glass.primaryAccent.copy(alpha = 0.9f),
                    glass.secondaryAccent,
                    glass.primaryAccent.copy(alpha = 0.9f),
                    Color.Transparent
                ),
                startX = rectLeft,
                endX = rectLeft + rectWidth
            ),
            start = Offset(rectLeft + 12.dp.toPx(), laserY),
            end = Offset(rectLeft + rectWidth - 12.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx()
        )

        // Multi-code AR Bounding Boxes
        liveDetections.forEach { det ->
            val box = det.normalizedBox ?: return@forEach
            val bLeft = box.left * w
            val bTop = box.top * h
            val bWidth = (box.width() * w).coerceAtLeast(36f)
            val bHeight = (box.height() * h).coerceAtLeast(36f)
            drawRoundRect(
                color = glass.secondaryAccent.copy(alpha = 0.25f),
                topLeft = Offset(bLeft, bTop),
                size = Size(bWidth, bHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = glass.secondaryAccent,
                topLeft = Offset(bLeft, bTop),
                size = Size(bWidth, bHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // Tap-to-focus reticle
        focusTapOffset?.let { tap ->
            drawCircle(
                color = glass.primaryAccent,
                radius = 28.dp.toPx(),
                center = tap,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = glass.primaryAccent.copy(alpha = 0.3f),
                radius = 6.dp.toPx(),
                center = tap
            )
        }
    }
}
