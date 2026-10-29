<div align="center">

<img src="docs/assets/qrypt-logo.svg" alt="QRYPT Animated Optical Logo" width="112" height="112" />

# QRYPT QR & Barcode Scanner

**Scan Smarter. Instantly.**

[![Android CI](https://github.com/CodeByArnob/QRYPT/actions/workflows/ci.yml/badge.svg)](https://github.com/CodeByArnob/QRYPT/actions/workflows/ci.yml)
[![Release APK](https://github.com/CodeByArnob/QRYPT/actions/workflows/release.yml/badge.svg)](https://github.com/CodeByArnob/QRYPT/releases)
[![GitHub Pages](https://github.com/CodeByArnob/QRYPT/actions/workflows/pages.yml/badge.svg)](https://codebyarnob.github.io/QRYPT/)
[![License: MIT](https://img.shields.io/badge/License-MIT-00E5FF.svg)](LICENSE)
[![Android API](https://img.shields.io/badge/Android-API%2024%20to%2036-3B82F6.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-00E5FF.svg)](https://developer.android.com/jetpack/compose)

<br />

<a href="https://codebyarnob.github.io/QRYPT/">
  <img src="docs/assets/qrypt-hero-motion.svg" alt="QRYPT Real-Time Optical Engine Motion Telemetry" width="100%" />
</a>

<br />

**QRYPT** is a high-precision, 100% offline Android QR and 13-format barcode scanner, batch inventory processor, anti-phishing URL inspector, local Room SQLite history vault, and 1024px to 1536px optical QR studio engineered by **ArnobRazz** with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, **Google ML Kit**, and **ZXing**.

[**Download for Android (v1.0.0 APK)**](https://github.com/CodeByArnob/QRYPT/releases/download/v1.0.0/QRYPT-v1.0.0-release.apk) • [**Live Product Page**](https://codebyarnob.github.io/QRYPT/) • [**Release Notes & SHA-256**](https://github.com/CodeByArnob/QRYPT/releases/tag/v1.0.0) • [**Privacy Policy**](PRIVACY_POLICY.md) • [**Security Policy**](SECURITY.md)

</div>

---

## Table of Contents

1. [Product Overview & Design System](#product-overview--design-system)
2. [Verified Features](#verified-features)
3. [Genuine Application Screenshots](#genuine-application-screenshots)
4. [Supported QR & Barcode Formats](#supported-qr--barcode-formats)
5. [Technical Architecture & Stack](#technical-architecture--stack)
6. [Privacy & Permissions Explained](#privacy--permissions-explained)
7. [Installation, Download & SHA-256 Verification](#installation-download--sha-256-verification)
8. [Building & Testing From Source](#building--testing-from-source)
9. [Troubleshooting & FAQ](#troubleshooting--faq)
10. [Contributing, Security & License](#contributing-security--license)

---

## Product Overview & Design System

QRYPT combines low-latency optical decoding with a refined **Liquid Glass** interface crafted for clarity, speed, and zero network telemetry:

- **Midnight Navy Dark Palette**: Built around `#070B14` primary backgrounds, `#0D1424` / `#121D30` elevated surfaces, `#00E5FF` vivid cyan optical accents, `#3B82F6` electric blue highlights, `#F5FAFF` high-contrast primary typography, and `#A9B7C9` readable cool gray copy.
- **Frosted Daylight Light Theme**: Adaptive high-contrast light palette (`#F4F7FB` canvas with `#0D1526` slate ink and `#0284C7` optical cyan) with a 1-tap header toggle and Android 12+ Material You Dynamic Color support.
- **100% Offline by Architecture**: QRYPT does **not** declare `android.permission.INTERNET` in `AndroidManifest.xml`. Every camera frame, heuristic safety check, batch export, and generated barcode runs strictly on-device.

---

## Verified Features

### 1. Real-Time CameraX + Google ML Kit Optical Scanner
- **Low-Latency Frame Analysis**: Uses `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` backpressure with bundled Google ML Kit Barcode Scanning (`17.3.0`) so frames are analyzed immediately without network downloads.
- **3 Viewfinder Target Modes**: Switch on the fly between **Precision Square** (QR & 2D matrix), **1D Laser Slit** (narrow horizontal window for dense retail/industrial barcodes), and **Full Sensor**.
- **Optical Controls**: Two-finger pinch-to-zoom, smooth zoom slider, tap-to-focus metering, hardware Torch toggle, Front/Back lens switch, and real-time Y-plane ambient luma calculation with automatic **Low-Light Exposure Boost** (< 45 lux).

### 2. Zero-Permission Photo Library & Built-In Self-Test
- **System Photo Picker (`PickVisualMedia`)**: Decode single or multiple barcodes from saved screenshots and gallery images without requesting `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE`.
- **Multi-Angle ZXing Recovery**: Automatically falls back across `HybridBinarizer`, `GlobalHistogramBinarizer`, rotated bitmaps (`90°`, `180°`, `270°`), and center-crop passes for dense or tilted images.
- **Optical Engine Self-Test**: Built-in verification menu that synthesizes real barcode bitmaps (Wi-Fi, Retail EAN-13, vCard, HTTPS URL, and Phishing Warning) and passes them through the optical decoder pipeline on-device.

### 3. Offline Anti-Phishing URL Shield & GS1 Checksum Engine
- **Local URL Heuristic Inspection (`PayloadParser.analyzeUrlSecurity`)**:
  - Flags unencrypted `http://` links (`WARNING`).
  - Detects Punycode (`xn--`) homograph spoofing, raw IPv4/IPv6 hostnames, embedded `@` authority credentials, high-risk top-level domains, and redirect shorteners (`HIGH_RISK` / `WARNING`) before any browser intent is launched.
- **GS1 & Modulo-10 Validation**: Computes Modulo-10 check digits and resolves GS1 country/region prefixes (including Bookland ISBN-13 / ISBN-10) for retail EAN/UPC barcodes.

### 4. High-Speed Batch & Continuous Inventory Mode
- **Multi-Scan Session Queue**: Capture dozens of barcodes in rapid succession with configurable duplicate cooldown suppression (`1 to 15s`).
- **Bulk Warehouse Tagging**: Attach batch notes/tags to an entire session and export RFC-4180 **CSV** or structured **JSON** files via Android’s `FileProvider` share sheet.

### 5. Local Optical Vault (Room SQLite)
- **Offline Database (`prism_scan_vault.db`)**: Full-text search across titles, raw payloads, and user annotations.
- **9 Category Filters**: Filter by `All`, `Favorites`, `URLs`, `Wi-Fi`, `Products`, `Contacts`, `Calendar`, `Geo/Comms`, or `Created in Studio`.
- **Privacy Controls**: Delete single entries, clear unfavorited history, or purge the entire Vault in one tap.

### 6. Optical QR & 13-Format Generator Studio
- **13 Barcode Symbologies + 10 Structured Templates**: Generate **URL**, **Wi-Fi Network**, **vCard 3.0 Contact**, **Plain Text**, **Retail GTIN (EAN/UPC)**, **Email**, **SMS**, **Phone**, **Geo Coordinates**, and **Calendar Event (`VEVENT`)** payloads.
- **Custom Styling & High-Res Export**: Select output resolution (`512px`, `1024px HD`, `1536px Print`), QR Error Correction (`L`, `M`, `Q`, `H`), rounded finder/module styling, and color presets, then share as a PNG or save to the Vault.

---

## Genuine Application Screenshots

All 8 screenshots below are **genuine renders** captured directly from QRYPT’s Jetpack Compose UI via Robolectric Native Graphics (`./scripts/capture-screenshots.sh`):

| 1. Live Optical Scanner HUD | 2. Smart Result & Security Shield |
| :---: | :---: |
| <img src="docs/screenshots/01-scanner-screen.png" alt="QRYPT Live Scanner Screen" width="340" /> | <img src="docs/screenshots/02-scan-result-sheet.png" alt="QRYPT Scan Result Sheet" width="340" /> |
| **3. Continuous & 1D Laser Slit Mode** | **4. Batch Inventory Mode** |
| <img src="docs/screenshots/03-image-scan-and-modes.png" alt="QRYPT Continuous and 1D Laser Slit Mode" width="340" /> | <img src="docs/screenshots/04-batch-scanner-screen.png" alt="QRYPT Batch Inventory Screen" width="340" /> |
| **5. Local Optical Vault** | **6. Optical Generator Studio** |
| <img src="docs/screenshots/05-history-vault-screen.png" alt="QRYPT History Vault Screen" width="340" /> | <img src="docs/screenshots/06-generator-studio-screen.png" alt="QRYPT Generator Studio Screen" width="340" /> |
| **7. Settings & Engine Control (Dark)** | **8. Frosted Daylight Theme Preview** |
| <img src="docs/screenshots/07-settings-screen-dark.png" alt="QRYPT Settings Dark Screen" width="340" /> | <img src="docs/screenshots/08-light-theme-preview.png" alt="QRYPT Light Theme Preview" width="340" /> |

---

## Supported QR & Barcode Formats

| 2D Matrix Formats (4) | 1D Linear Retail & Industrial Formats (9) | Structured Payload Schemas (12) |
| :--- | :--- | :--- |
| • **QR Code** (`QR_CODE`)<br>• **Aztec** (`AZTEC`)<br>• **Data Matrix** (`DATA_MATRIX`)<br>• **PDF417** (`PDF417`) | • **EAN-13** & **EAN-8**<br>• **UPC-A** & **UPC-E**<br>• **Code 128**, **Code 39**, **Code 93**<br>• **Codabar** & **ITF (Interleaved 2 of 5)** | • Website URLs (`http://`, `https://`)<br>• Wi-Fi (`WIFI:S:...;T:WPA;P:...;;`)<br>• vCard 3.0 (`BEGIN:VCARD`) & `MECARD:`<br>• Calendar (`BEGIN:VEVENT`)<br>• Email (`mailto:`, `MATMSG:`)<br>• SMS (`smsto:`) & Phone (`tel:`)<br>• Geo (`geo:lat,lng`)<br>• ISBN-13 / ISBN-10, Retail GTIN, JSON & Text |

---

## Technical Architecture & Stack

```text
app/src/main/java/com/example/
├── MainActivity.kt                        # Edge-to-edge Compose host, navigation & runtime permission flow
├── data/
│   ├── local/                             # Room SQLite (PrismScanDatabase, ScanRecordDao, ScanRecordEntity)
│   ├── preferences/                       # Jetpack DataStore (SettingsRepository, AppThemeMode, AccentColorPreset)
│   └── repository/                        # ScanRepository (duplicate cooldown guard, CSV & JSON export)
├── domain/
│   ├── model/                             # BarcodeModels.kt (BarcodeFormatType, ParsedPayload, UrlSecurityReport)
│   └── parser/                            # PayloadParser.kt (12 payload schemas, GS1 lookup, URL phishing analyzer)
├── engine/
│   ├── BarcodeGeneratorEngine.kt          # 13-format ZXing generator with rounded modules & color presets
│   ├── BarcodeScannerEngine.kt            # CameraX ImageAnalysis + ML Kit BarcodeScanning + multi-angle ZXing
│   └── FeedbackController.kt              # Tactile vibration & synthesized acoustic confirmation tones
├── ui/
│   ├── components/                        # LiquidGlassComponents.kt, ScanResultSheet.kt
│   ├── screens/                           # ScannerScreen, BatchScannerScreen, HistoryVaultScreen,
│   │                                      # GeneratorStudioScreen, SettingsScreen
│   ├── theme/                             # Color.kt (#070B14 Midnight Navy & Daylight tokens), Theme.kt, Type.kt
│   └── viewmodel/                         # PrismScanViewModel.kt (unidirectional StateFlow state holder)
└── util/
    └── ActionIntentHelper.kt              # Context-aware system intents & FileProvider CSV/JSON/PNG sharing
```

---

## Privacy & Permissions Explained

| Permission | Protection Level | Why It Is Needed |
| :--- | :--- | :--- |
| `android.permission.CAMERA` | Runtime (Optional) | Used strictly for real-time optical viewfinder scanning. If denied, users can still scan images from the Photo Library, run Self-Tests, browse the Vault, and generate codes in Studio. |
| `android.permission.VIBRATE` | Normal (Install-Time) | Provides tactile haptic confirmation when a barcode locks onto the reticle (can be disabled in Settings). |

- **No `android.permission.INTERNET`**: The app cannot connect to any remote server or transmit scan data.
- **No Storage Permissions**: Uses the zero-permission Android system Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- **No Location, Contacts, or Microphone Permissions**: Verified in [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml).

---

## Installation, Download & SHA-256 Verification

### Current Release (`v1.0.0`)
- **Release Page**: [https://github.com/CodeByArnob/QRYPT/releases/tag/v1.0.0](https://github.com/CodeByArnob/QRYPT/releases/tag/v1.0.0)
- **Direct APK Download**: [`QRYPT-v1.0.0-release.apk`](https://github.com/CodeByArnob/QRYPT/releases/download/v1.0.0/QRYPT-v1.0.0-release.apk) (`12.65 MB`)
- **Checksums File**: [`SHA256SUMS.txt`](https://github.com/CodeByArnob/QRYPT/releases/download/v1.0.0/SHA256SUMS.txt)

```bash
sha256sum QRYPT-v1.0.0-release.apk
# 071bc524d78637517b6ce14f6c471b2421497f79a5cfacfaaf2041bc048b9c23  QRYPT-v1.0.0-release.apk
```

---

## Building & Testing From Source

### Prerequisites
- **JDK**: Java 17 or Java 21
- **Android SDK**: `minSdk = 24` (Android 7.0), `targetSdk = 36`, `compileSdk = 36 (minorApiLevel = 1)`
- **Gradle**: Gradle `9.3.1`

### Commands
```bash
# 1. Run all JUnit & Robolectric tests (also renders genuine screenshots into docs/screenshots/)
gradle :app:testDebugUnitTest

# 2. Run Android Lint static analysis
gradle :app:lintDebug

# 3. Assemble Debug APK
gradle :app:assembleDebug

# 4. Regenerate genuine application screenshots via helper script
./scripts/capture-screenshots.sh
```

### Configuring Production Release Signing in GitHub Actions
The automated release workflow (`.github/workflows/release.yml`) supports persistent production signing via repository secrets:
- `KEYSTORE_BASE64`: Base64-encoded JKS keystore containing the `upload` key alias.
- `STORE_PASSWORD`: Keystore password.
- `KEY_PASSWORD`: Key password for the `upload` alias.

When these secrets are not yet configured in the repository settings, the workflow automatically generates a documented self-signed CI release certificate so tagged releases still build, verify via `apksigner`, and publish `SHA256SUMS.txt`.

---

## Troubleshooting & FAQ

- **Why does Google Play Protect show *"Play Protect hasn't seen an app from this developer before"* when sideloading?**  
  When installing an independent open-source APK downloaded directly from GitHub Releases rather than the Google Play Store, Play Protect displays an unrecognized-developer prompt on first install. Verify the `SHA-256` hash above, then tap **More details → Install anyway**.
- **Why does updating over an older build fail with "App not installed as package conflicts with an existing package"?**  
  Android requires upgrades to be signed with the exact same certificate as the currently installed APK. If you previously installed an older debug/test build with a different signature, uninstall the previous build once before installing `v1.0.0`.
- **Does QRYPT work without Google Play Services or internet access?**  
  Yes. QRYPT bundles the barcode scanning model inside the APK and uses ZXing Core for static and generated codes, working 100% offline.

---

## Contributing, Security & License

- **Contributing Guide**: See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
- **Changelog**: See [CHANGELOG.md](CHANGELOG.md).
- **Security Policy**: See [SECURITY.md](SECURITY.md) (report vulnerabilities privately to `arnobrazz.dev@gmail.com`).
- **Issues & Feature Requests**: [https://github.com/CodeByArnob/QRYPT/issues](https://github.com/CodeByArnob/QRYPT/issues)
- **Developer**: **ArnobRazz** ([@CodeByArnob](https://github.com/CodeByArnob))
- **License**: Released under the [MIT License](LICENSE) • **© 2026 ArnobRazz**.
