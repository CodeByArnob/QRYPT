# Contributing to QRYPT

Thank you for your interest in contributing to **QRYPT: Scan Smarter. Instantly.** This document provides guidelines and instructions for building, testing, and submitting improvements to the project.

---

## 1. Development Environment Prerequisites

1. **JDK**: Java 17 or Java 21 (configured to compile with Java 11 bytecode target compatibility).
2. **Android SDK**:
   1. `compileSdk`: **36** (`minorApiLevel = 1`)
   2. `targetSdk`: **36**
   3. `minSdk`: **24** (Android 7.0+)
3. **Build System**: Gradle with Kotlin DSL (`.gradle.kts`) and Version Catalog (`gradle/libs.versions.toml`).
4. **IDE**: Android Studio Ladybug / Meerkat (or newer) or command-line Gradle.

---

## 2. Project Architecture

QRYPT follows a modular, unidirectional **Clean Architecture + MVVM** structure inside `app/src/main/java/com/example/`:

1. **`domain/model/`**: Immutable domain models (`BarcodeFormatType`, `ParsedPayload`, `ParsedPayloadType`, `UrlSecurityReport`, `SecurityRiskLevel`, `ScanRegionMode`, `DetectedBarcodeFrame`, `QrCustomizationConfig`, `ProductInfo`, `ProductNutritionInfo`, `ProductLookupState`, `BarcodeValidator`).
2. **`domain/parser/`**: `PayloadParser`: Zero-dependency on-device parser, GTIN Modulo-10 checksum calculator, GS1 country prefix lookup, unsafe URI scheme blocker, and URL anti-phishing heuristic analyzer.
3. **`engine/`**:
   1. `BarcodeScannerEngine`: CameraX `ImageAnalysis.Analyzer` powered by Google ML Kit Barcode Scanning + ZXing Multi-Format Reader for static bitmaps.
   2. `BarcodeGeneratorEngine`: 13-format barcode and QR matrix generator with custom styling.
   3. `FeedbackController`: Tactile vibration and acoustic beep confirmation.
4. **`data/`**:
   1. `local/`: Room SQLite database (`PrismScanDatabase`, `ScanRecordDao`, `ScanRecordEntity`, `ProductCacheDao`, `ProductCacheEntity`).
   2. `remote/`: `OpenFoodFactsService` and `ProductLookupRemoteDataSource` for HTTPS product barcode identification.
   3. `repository/`: `ScanRepository` (duplicate cooldown suppression and RFC-4180 CSV/JSON export) and `ProductLookupRepository` (barcode validation, Room cache, offline detection, and remote lookup).
   4. `preferences/`: `SettingsRepository` backed by Jetpack DataStore Preferences.
5. **`ui/`**:
   1. `theme/`: `PrismScanTheme`, `Color.kt`, `Type.kt` (locally bundled `Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`).
   2. `components/`: `LiquidGlassComponents.kt`, `ScanResultSheet.kt` (smart action sheet, external link confirmation dialog, and product details modal).
   3. `screens/`: `ScannerScreen.kt`, `BatchScannerScreen.kt`, `GeneratorStudioScreen.kt`, `HistoryVaultScreen.kt`, `SettingsScreen.kt`.
   4. `viewmodel/`: `PrismScanViewModel.kt`.

---

## 3. Running Builds & Automated Tests

### Run Unit & Robolectric UI Tests
```bash
gradle :app:testDebugUnitTest
```

### Build Debug APK
```bash
gradle :app:assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Regenerate Application Screenshots
```bash
./scripts/capture-screenshots.sh
```
This script captures genuine screenshots into `docs/screenshots/` using either a connected ADB device or the deterministic Robolectric Native-Graphics test suite.

---

## 4. Design & Privacy Standards

1. **Privacy-First Network Scope**: Camera frames, gallery images, QR payloads, and scan history must never be transmitted over the network. Network calls are restricted strictly to optional HTTPS product barcode lookups (`OpenFoodFactsService`) using validated numeric GTIN barcodes.
2. **Safe External Intents**: External URLs must never open automatically without user interaction. Always validate schemes (`http` / `https` only) and require user confirmation.
3. **Visual Consistency**: Maintain the QRYPT Midnight Navy (`#070B14`, `#0D1424`, `#121D30`), Vivid Cyan (`#00E5FF`), and Electric Blue (`#3B82F6`) palette alongside high-contrast Frosted Daylight light mode support.
4. **Accessibility & Testing**: Every interactive Jetpack Compose control must meet a minimum `48.dp` touch target, provide a meaningful `contentDescription`, and include a `Modifier.testTag(...)` where appropriate.

---

## 5. Submitting a Pull Request

1. Fork the repository (`https://github.com/CodeByArnob/QRYPT`) and create a feature branch from `main`.
2. Add or update unit/Robolectric tests in `app/src/test/java/com/example/` covering your changes.
3. Verify that `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` pass with zero errors.
4. Open a Pull Request with a clear summary of the problem and solution.
