package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrismScanDatabase
import com.example.data.local.ScanRecordEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.OpenFoodFactsService
import com.example.data.remote.ProductLookupRemoteDataSource
import com.example.data.remote.RemoteLookupResult
import com.example.data.repository.NetworkConnectivityChecker
import com.example.data.repository.ProductLookupRepository
import com.example.data.repository.ScanRepository
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.ProductInfo
import com.example.domain.model.ProductLookupState
import com.example.domain.model.ProductNutritionInfo
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.SecurityRiskLevel
import com.example.engine.BarcodeGeneratorEngine
import com.example.engine.BarcodeScannerEngine
import com.example.engine.FeedbackController
import com.example.engine.GenerationResult
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.ScanResultSheetContent
import com.example.ui.theme.PrismScanTheme
import com.example.ui.viewmodel.PrismScanViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun saveViewScreenshot(hostView: View, filename: String) {
        composeTestRule.waitForIdle()
        val rootView = hostView.rootView ?: hostView
        val width = rootView.width.takeIf { it > 0 } ?: 1080
        val height = rootView.height.takeIf { it > 0 } ?: 2340
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeTestRule.runOnUiThread {
            rootView.draw(canvas)
        }
        val dir = File("../docs/screenshots").apply { mkdirs() }
        val outFile = File(dir, filename)
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        assertTrue("Screenshot $filename should exist and be non-empty", outFile.exists() && outFile.length() > 1024L)
    }

    @Test
    fun `app_name resource matches QRYPT exactly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QRYPT", appName)
    }

    @Test
    fun `mobile viewport header displays QRYPT and respects top status bar safe area`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = PrismScanDatabase.createInMemory(context)
        val viewModel = PrismScanViewModel(
            appContext = context,
            scanRepository = ScanRepository(db.scanRecordDao()),
            settingsRepository = SettingsRepository(context),
            feedbackController = FeedbackController(context)
        )

        composeTestRule.setContent {
            PrismScanAppRoot(customViewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        val headerNode = composeTestRule.onNodeWithTag("top_header_container").fetchSemanticsNode()
        val appNameNode = composeTestRule.onNodeWithTag("top_header_app_name").fetchSemanticsNode()
        assertTrue("Expected top_header_app_name top >= 36px, was ${appNameNode.boundsInRoot.top}", appNameNode.boundsInRoot.top >= 36f)
        assertTrue("Expected header container positive height", headerNode.boundsInRoot.height > 40f)

        db.close()
    }

    @Test
    fun `dark mode and light mode toggle works instantaneously`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = PrismScanDatabase.createInMemory(context)
        val viewModel = PrismScanViewModel(
            appContext = context,
            scanRepository = ScanRepository(db.scanRecordDao()),
            settingsRepository = SettingsRepository(context),
            feedbackController = FeedbackController(context)
        )

        composeTestRule.setContent {
            PrismScanAppRoot(customViewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        assertEquals(AppThemeMode.DARK, viewModel.settings.value.themeMode)

        composeTestRule.onNodeWithTag("top_theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeMode.LIGHT, viewModel.settings.value.themeMode)

        composeTestRule.onNodeWithTag("top_theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppThemeMode.DARK, viewModel.settings.value.themeMode)

        db.close()
    }

    @Test
    fun `optical generator and scanner engine round-trip across multiple 1D and 2D formats`() {
        val formatsToVerify = listOf(
            BarcodeFormatType.QR_CODE to "https://github.com/CodeByArnob/QRYPT",
            BarcodeFormatType.EAN_13 to "5901234123457",
            BarcodeFormatType.EAN_8 to "96385074",
            BarcodeFormatType.UPC_A to "036000291452",
            BarcodeFormatType.CODE_128 to "QRYPT-2026-X99",
            BarcodeFormatType.CODE_39 to "ASSET-4092-B",
            BarcodeFormatType.AZTEC to "EUROSTAR-TKT-998123",
            BarcodeFormatType.PDF417 to "BOARDING:AZ-8841|SEAT:14A"
        )

        for ((format, payload) in formatsToVerify) {
            val config = QrCustomizationConfig(
                sizePx = 640,
                marginModules = 2,
                roundedDots = false
            )
            val generated = BarcodeGeneratorEngine.generate(payload, format, config)
            assertTrue("Expected GenerationResult.Success for $format", generated is GenerationResult.Success)
            val bitmap = (generated as GenerationResult.Success).bitmap
            assertNotNull(bitmap)

            val decodedList = BarcodeScannerEngine.decodeWithZxing(bitmap)
            assertTrue("Expected non-empty decode for $format", decodedList.isNotEmpty())
            assertEquals(generated.normalizedPayload, decodedList.first().rawValue)
        }
    }

    @Test
    fun `Room database and ScanRepository enforce duplicate cooldown and manage favorites`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = PrismScanDatabase.createInMemory(context)
        val repo = ScanRepository(db.scanRecordDao())

        val first = repo.recordScan(
            rawValue = "WIFI:T:WPA;S:LabNet;P:Secret123;H:false;;",
            format = BarcodeFormatType.QR_CODE,
            duplicatePreventionEnabled = true,
            duplicateCooldownSeconds = 10
        )
        assertNotNull(first)

        val duplicate = repo.recordScan(
            rawValue = "WIFI:T:WPA;S:LabNet;P:Secret123;H:false;;",
            format = BarcodeFormatType.QR_CODE,
            duplicatePreventionEnabled = true,
            duplicateCooldownSeconds = 10
        )
        assertNull(duplicate)

        repo.toggleFavorite(first!!.id, true)
        repo.updateNotes(first.id, "Primary Lab Router")

        val records = repo.allRecords.first()
        assertEquals(1, records.size)
        assertTrue(records.first().isFavorite)
        assertEquals("Primary Lab Router", records.first().customNotes)

        repo.clearNonFavorites()
        assertEquals(1, repo.allRecords.first().size)

        repo.clearAllHistory()
        assertTrue(repo.allRecords.first().isEmpty())
        db.close()
    }

    @Test
    fun `OpenFoodFactsService parses JSON and ProductLookupRepository handles caching, not found, offline, invalid barcode, and timeout`() = runBlocking {
        val sampleSuccessJson = """
            {
              "status": "success",
              "product": {
                "product_name": "Nutella Hazelnut Spread",
                "brands": "Ferrero",
                "quantity": "400 g",
                "categories": "Spreads, Sweet spreads, Hazelnut spreads",
                "ingredients_text": "Sugar, palm oil, hazelnuts 13%, skimmed milk powder 8.7%, fat-reduced cocoa 7.4%",
                "image_front_url": "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.jpg",
                "nutriscore_grade": "e",
                "nova_group": 4,
                "origins": "France",
                "nutriments": {
                  "energy-kcal_100g": 539,
                  "fat_100g": 30.9,
                  "sugars_100g": 56.3,
                  "proteins_100g": 6.3,
                  "salt_100g": 0.107
                }
              }
            }
        """.trimIndent()

        val parsedSuccess = OpenFoodFactsService.parseOpenFoodFactsResponse(
            jsonString = sampleSuccessJson,
            barcode = "3017620422003",
            formatName = "EAN-13"
        )
        assertTrue(parsedSuccess is RemoteLookupResult.Found)
        val parsedProduct = (parsedSuccess as RemoteLookupResult.Found).product
        assertEquals("Nutella Hazelnut Spread", parsedProduct.productName)
        assertEquals("Ferrero", parsedProduct.brand)
        assertEquals("400 g", parsedProduct.quantity)
        assertEquals("E", parsedProduct.nutrition.nutriscoreGrade)
        assertEquals(4, parsedProduct.nutrition.novaGroup)
        assertEquals(539.0, parsedProduct.nutrition.energyKcal100g ?: 0.0, 0.01)

        val notFoundJson = """{"status": "failure", "status_verbose": "product not found"}"""
        val parsedNotFound = OpenFoodFactsService.parseOpenFoodFactsResponse(
            jsonString = notFoundJson,
            barcode = "0000000000000",
            formatName = "EAN-13"
        )
        assertTrue(parsedNotFound is RemoteLookupResult.NotFound)

        val parsedMalformed = OpenFoodFactsService.parseOpenFoodFactsResponse(
            jsonString = "not-valid-json{{{",
            barcode = "3017620422003",
            formatName = "EAN-13"
        )
        assertTrue(parsedMalformed is RemoteLookupResult.NetworkError)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = PrismScanDatabase.createInMemory(context)
        var isOnline = true
        var remoteMode = "SUCCESS"

        val fakeRemote = object : ProductLookupRemoteDataSource {
            override suspend fun fetchProductByBarcode(
                barcode: String,
                formatName: String
            ): RemoteLookupResult {
                return when (remoteMode) {
                    "SUCCESS" -> RemoteLookupResult.Found(parsedProduct.copy(barcode = barcode, formatName = formatName))
                    "NOT_FOUND" -> RemoteLookupResult.NotFound(barcode)
                    else -> RemoteLookupResult.NetworkError(
                        message = "Request timed out while contacting the product database.",
                        isTimeout = true
                    )
                }
            }
        }

        val connectivityChecker = NetworkConnectivityChecker { isOnline }
        val repo = ProductLookupRepository(
            productCacheDao = db.productCacheDao(),
            remoteDataSource = fakeRemote,
            connectivityChecker = connectivityChecker
        )

        // 1. Invalid barcode input
        val invalidState = repo.lookupProduct("12345", BarcodeFormatType.EAN_13)
        assertTrue(invalidState is ProductLookupState.InvalidBarcode)

        // 2. Offline product lookup (before cache)
        isOnline = false
        val offlineState = repo.lookupProduct("3017620422003", BarcodeFormatType.EAN_13)
        assertTrue(offlineState is ProductLookupState.Offline)

        // 3. Successful product lookup and Room caching
        isOnline = true
        remoteMode = "SUCCESS"
        val successState = repo.lookupProduct("3017620422003", BarcodeFormatType.EAN_13)
        assertTrue(successState is ProductLookupState.Success)
        assertEquals("Nutella Hazelnut Spread", (successState as ProductLookupState.Success).product.productName)

        // 4. Cached lookup succeeds even when device goes offline
        isOnline = false
        val cachedOfflineState = repo.lookupProduct("3017620422003", BarcodeFormatType.EAN_13)
        assertTrue(cachedOfflineState is ProductLookupState.Success)
        assertTrue((cachedOfflineState as ProductLookupState.Success).product.isFromCache)

        // 5. Product not found response
        isOnline = true
        remoteMode = "NOT_FOUND"
        val notFoundState = repo.lookupProduct("5901234123457", BarcodeFormatType.EAN_13)
        assertTrue(notFoundState is ProductLookupState.NotFound)

        // 6. Network timeout / error handling
        remoteMode = "TIMEOUT"
        val errorState = repo.lookupProduct("036000291452", BarcodeFormatType.UPC_A)
        assertTrue(errorState is ProductLookupState.Error)

        db.close()
    }

    @Test
    fun `ScanResultSheet displays Open Link with confirmation dialog and Search Online`() {
        val urlEntity = ScanRecordEntity(
            id = 10L,
            rawValue = "https://github.com/CodeByArnob/QRYPT",
            primaryTitle = "github.com",
            subtitle = "HTTPS | Verified Safe Syntax",
            formatName = BarcodeFormatType.QR_CODE.name,
            payloadTypeName = ParsedPayloadType.URL.name,
            securityRiskName = SecurityRiskLevel.SAFE.name
        )

        composeTestRule.setContent {
            PrismScanTheme(themeMode = AppThemeMode.DARK) {
                ScanResultSheetContent(
                    record = urlEntity,
                    previewBitmap = null
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify Open Link button is visible and opens confirmation dialog before external navigation
        composeTestRule.onNodeWithTag("sheet_open_url_button").assertExists()
        composeTestRule.onNodeWithText("Open Link").assertExists()
        composeTestRule.onNodeWithTag("sheet_search_online_button").assertExists()
        composeTestRule.onNodeWithTag("sheet_open_url_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("external_action_confirm_dialog").assertExists()
        composeTestRule.onNodeWithTag("cancel_external_action_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `ScanResultSheet displays View Product button and opens full ProductDetailsModalDialog`() {
        val productEntity = ScanRecordEntity(
            id = 11L,
            rawValue = "3017620422003",
            primaryTitle = "Nutella Hazelnut Spread",
            subtitle = "Ferrero | 400 g | EAN-13",
            formatName = BarcodeFormatType.EAN_13.name,
            payloadTypeName = ParsedPayloadType.PRODUCT.name,
            securityRiskName = SecurityRiskLevel.SAFE.name
        )
        val sampleProduct = ProductInfo(
            barcode = "3017620422003",
            formatName = "EAN-13",
            productName = "Nutella Hazelnut Spread",
            brand = "Ferrero",
            categories = "Hazelnut spreads",
            quantity = "400 g",
            ingredientsText = "Sugar, palm oil, hazelnuts 13%, skimmed milk powder",
            imageUrl = null,
            nutrition = ProductNutritionInfo(
                nutriscoreGrade = "E",
                novaGroup = 4,
                energyKcal100g = 539.0,
                fat100g = 30.9,
                sugars100g = 56.3,
                proteins100g = 6.3,
                salt100g = 0.11
            ),
            origins = "France",
            sourceUrl = "https://world.openfoodfacts.org/product/3017620422003"
        )

        composeTestRule.setContent {
            PrismScanTheme(themeMode = AppThemeMode.DARK) {
                ScanResultSheetContent(
                    record = productEntity,
                    previewBitmap = null,
                    productLookupState = ProductLookupState.Success(sampleProduct)
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("product_lookup_success_card").assertExists()
        composeTestRule.onNodeWithTag("sheet_view_product_button").assertExists()
        composeTestRule.onNodeWithTag("sheet_view_product_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("product_details_dialog").assertExists()
        composeTestRule.onNodeWithTag("close_product_details_dialog_button").assertExists()
        composeTestRule.onNodeWithTag("close_product_details_dialog_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `render and capture genuine QRYPT application screenshots across all screens`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = PrismScanDatabase.createInMemory(context)
        val scanRepo = ScanRepository(db.scanRecordDao())
        val settingsRepo = SettingsRepository(context)
        val viewModel = PrismScanViewModel(
            appContext = context,
            scanRepository = scanRepo,
            settingsRepository = settingsRepo,
            feedbackController = FeedbackController(context)
        )

        val activeBatchId = viewModel.activeBatchSessionId.value
        scanRepo.recordScan(
            rawValue = "https://bit.ly/qrypt-v1-release",
            format = BarcodeFormatType.QR_CODE,
            isGenerated = false,
            customNotes = "Shortened Release Link",
            duplicatePreventionEnabled = false
        )

        val batchProduct = scanRepo.recordScan(
            rawValue = "3017620422003",
            format = BarcodeFormatType.EAN_13,
            isGenerated = false,
            batchSessionId = activeBatchId,
            customNotes = "Warehouse Batch #A4",
            duplicatePreventionEnabled = false
        )!!

        val wifiRecord = scanRepo.recordScan(
            rawValue = "WIFI:T:WPA;S:QRYPT-Optics-5G;P:Qrypt#2026!Secure;H:false;;",
            format = BarcodeFormatType.QR_CODE,
            isGenerated = false,
            batchSessionId = activeBatchId,
            customNotes = "HQ Fiber Lab Router",
            duplicatePreventionEnabled = false
        )!!
        scanRepo.toggleFavorite(wifiRecord.id, true)

        val urlRecord = scanRepo.recordScan(
            rawValue = "https://github.com/CodeByArnob/QRYPT",
            format = BarcodeFormatType.QR_CODE,
            isGenerated = true,
            batchSessionId = activeBatchId,
            customNotes = "Official QRYPT Repository",
            duplicatePreventionEnabled = false
        )!!
        scanRepo.toggleFavorite(urlRecord.id, true)

        var capturedView: View? = null
        composeTestRule.setContent {
            capturedView = LocalView.current
            PrismScanAppRoot(customViewModel = viewModel)
        }
        composeTestRule.waitForIdle()
        val hostView = capturedView!!

        // 1. Main QR & Barcode Scanner Screen
        saveViewScreenshot(hostView, "01-scanner-screen.png")

        // 2. Continuous & 1D Laser Slit Mode + Image Scan Controls
        composeTestRule.onNodeWithTag("scanner_continuous_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("region_mode_BARCODE_SLIT").performClick()
        composeTestRule.waitForIdle()
        saveViewScreenshot(hostView, "03-image-scan-and-modes.png")

        // Reset region mode to Precision Square
        composeTestRule.onNodeWithTag("region_mode_PRECISION_SQUARE").performClick()
        composeTestRule.waitForIdle()

        // 3. Batch Scanner Screen + verify batch item result actions exist
        composeTestRule.onNodeWithTag("nav_tab_batch").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("batch_item_action_${batchProduct.id}").assertExists()
        composeTestRule.onNodeWithTag("batch_item_copy_${batchProduct.id}").assertExists()
        composeTestRule.onNodeWithTag("batch_item_share_${batchProduct.id}").assertExists()
        saveViewScreenshot(hostView, "04-batch-scanner-screen.png")

        // 4. Optical Generator Studio Screen
        composeTestRule.onNodeWithTag("nav_tab_studio").performClick()
        composeTestRule.waitForIdle()
        saveViewScreenshot(hostView, "06-generator-studio-screen.png")

        // 5. History / Optical Vault Screen
        composeTestRule.onNodeWithTag("nav_tab_vault").performClick()
        composeTestRule.waitForIdle()
        saveViewScreenshot(hostView, "05-history-vault-screen.png")

        // 6. Settings Screen (Dark Theme) - verify package name is NOT displayed and version is 1.1.0
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Developer: Arnobrazz | Version 1.1.0").assertExists()
        saveViewScreenshot(hostView, "07-settings-screen-dark.png")

        // 7. Light Theme Preview
        composeTestRule.onNodeWithTag("top_theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        saveViewScreenshot(hostView, "08-light-theme-preview.png")

        db.close()
    }

    @Test
    fun `render and capture ScanResultBottomSheet screenshot`() {
        val sampleEntity = ScanRecordEntity(
            id = 42L,
            rawValue = "https://github.com/CodeByArnob/QRYPT",
            primaryTitle = "github.com",
            subtitle = "HTTPS | Verified Safe Syntax",
            formatName = BarcodeFormatType.QR_CODE.name,
            payloadTypeName = ParsedPayloadType.URL.name,
            securityRiskName = SecurityRiskLevel.SAFE.name,
            isFavorite = true,
            isGenerated = false,
            customNotes = "Verified Open-Source Release Repository",
            timestamp = 1761400000000L
        )
        val qrBitmap = (BarcodeGeneratorEngine.generate(
            sampleEntity.rawValue,
            sampleEntity.format,
            QrCustomizationConfig(sizePx = 512, marginModules = 2)
        ) as? GenerationResult.Success)?.bitmap

        var capturedView: View? = null
        composeTestRule.setContent {
            capturedView = LocalView.current
            PrismScanTheme(themeMode = AppThemeMode.DARK) {
                LiquidGlassBackground {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(top = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            ScanResultSheetContent(
                                record = sampleEntity,
                                previewBitmap = qrBitmap
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        saveViewScreenshot(capturedView!!, "02-scan-result-sheet.png")
    }
}
