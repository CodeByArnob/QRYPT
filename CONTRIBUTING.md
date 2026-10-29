# Contributing to QRYPT

Thank you for your interest in contributing to **QRYPT — Scan Smarter. Instantly.**! This document provides guidelines and instructions for building, testing, and submitting improvements to the project.

---

## 1. Development Environment Prerequisites

- **JDK**: Java 17 or Java 21 (configured to compile with Java 11 bytecode target compatibility).
- **Android SDK**:
  - `compileSdk`: **36** (`minorApiLevel = 1`)
  - `targetSdk`: **36**
  - `minSdk`: **24** (Android 7.0+)
- **Build System**: Gradle with Kotlin DSL (`.gradle.kts`) and Version Catalog (`gradle/libs.versions.toml`).
- **IDE**: Android Studio Ladybug / Meerkat (or newer) or command-line Gradle.

---

## 2. Project Architecture

QRYPT follows a modular, unidirectional **Clean Architecture + MVVM** structure inside `app/src/main/java/com/example/`:

- **`domain/model/`** — Immutable domain models (`BarcodeFormatType`, `ParsedPayload`, `ParsedPayloadType`, `UrlSecurityReport`, `SecurityRiskLevel`, `ScanRegionMode`, `DetectedBarcodeFrame`, `QrCustomizationConfig`).
- **`domain/parser/`** — `PayloadParser`: Zero-dependency offline parser, GTIN Modulo-10 checksum calculator, GS1 country prefix lookup, and URL anti-phishing heuristic analyzer.
- **`engine/`** —
  - `BarcodeScannerEngine`: CameraX `ImageAnalysis.Analyzer` powered by Google ML Kit Barcode Scanning + ZXing Multi-Format Reader for static bitmaps.
  - `BarcodeGeneratorEngine`: 13-format barcode & QR matrix generator with custom styling.
  - `FeedbackController`: Tactile vibration and acoustic beep confirmation.
- **`data/`** —
  - `local/`: Room SQLite database (`PrismScanDatabase`, `ScanRecordDao`, `ScanRecordEntity`).
  - `repository/`: `ScanRepository` with duplicate cooldown suppression and RFC-4180 CSV/JSON export.
  - `preferences/`: `SettingsRepository` backed by Jetpack DataStore Preferences.
- **`ui/`** —
  - `theme/`: `PrismScanTheme`, `Color.kt`, `Type.kt` (locally bundled `Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`).
  - `components/`: `LiquidGlassComponents.kt`, `ScanResultSheet.kt`.
  - `screens/`: `ScannerScreen.kt`, `BatchScannerScreen.kt`, `GeneratorStudioScreen.kt`, `HistoryVaultScreen.kt`, `SettingsScreen.kt`.
  - `viewmodel/`: `PrismScanViewModel.kt`.

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

1. **Zero Network / 100% Offline**: Never add `android.permission.INTERNET`, analytics SDKs, or external telemetry calls. All barcode decoding, generation, and security inspection must execute strictly on-device.
2. **Least-Privilege Permissions**: Only `CAMERA` and `VIBRATE` are permitted in `AndroidManifest.xml`. Media selection must use the zero-permission Android Photo Picker (`PickVisualMedia`).
3. **Visual Consistency**: Maintain the QRYPT Midnight Navy (`#070B14`, `#0D1424`, `#121D30`), Vivid Cyan (`#00E5FF`), and Electric Blue (`#3B82F6`) palette alongside high-contrast Frosted Daylight light mode support.
4. **Accessibility & Testing**: Every interactive Jetpack Compose control must meet a minimum `48.dp` touch target, provide a meaningful `contentDescription`, and include a `Modifier.testTag(...)` where appropriate.

---

## 5. Submitting a Pull Request

1. Fork the repository (`https://github.com/CodeByArnob/QRYPT`) and create a feature branch from `main`.
2. Add or update unit/Robolectric tests in `app/src/test/java/com/example/` covering your changes.
3. Verify that `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` pass with zero errors.
4. Open a Pull Request with a clear summary of the problem and solution.
