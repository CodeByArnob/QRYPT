<div align="center">

<img src="docs/assets/qrypt-logo.svg" alt="QRYPT Logo" width="108" height="108" />

# QRYPT — QR & Barcode Scanner

**Scan Smarter. Instantly.**

[![Android CI](https://github.com/CodeByArnob/QRYPT/actions/workflows/ci.yml/badge.svg)](https://github.com/CodeByArnob/QRYPT/actions/workflows/ci.yml)
[![Release APK](https://github.com/CodeByArnob/QRYPT/actions/workflows/release.yml/badge.svg)](https://github.com/CodeByArnob/QRYPT/releases)
[![GitHub Pages](https://github.com/CodeByArnob/QRYPT/actions/workflows/pages.yml/badge.svg)](https://codebyarnob.github.io/QRYPT/)
[![License: MIT](https://img.shields.io/badge/License-MIT-00E5FF.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-3B82F6.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-00E5FF.svg)](https://developer.android.com/jetpack/compose)

**QRYPT** is a high-precision, offline-first Android QR and barcode scanner, batch inventory processor, encrypted local history vault, and custom optical QR studio engineered with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, **Google ML Kit**, **ZXing**, and **Room SQLite**.

[Download Latest APK](https://github.com/CodeByArnob/QRYPT/releases/latest) • [Live Project Page](https://codebyarnob.github.io/QRYPT/) • [Security Policy](SECURITY.md) • [Privacy Policy](PRIVACY_POLICY.md)

</div>

---

## Table of Contents

1. [Overview & Visual Direction](#overview--visual-direction)
2. [Key Features](#key-features)
3. [Application Screenshots](#application-screenshots)
4. [Supported Barcode Symbologies](#supported-barcode-symbologies)
5. [Architecture & Tech Stack](#architecture--tech-stack)
6. [Privacy & Permissions](#privacy--permissions)
7. [Installation & Sideloading Guide](#installation--sideloading-guide)
8. [Building & Testing Locally](#building--testing-locally)
9. [Automated GitHub Actions CI/CD](#automated-github-actions-cicd)
10. [Contributing & License](#contributing--license)

---

## Overview & Visual Direction

QRYPT combines low-latency optical decoding with a refined **Liquid Glass** interface crafted for clarity, speed, and zero telemetry:

- **Midnight Navy Dark Palette**: Engineered around `#070B14` primary backgrounds, `#0D1424` / `#121D30` elevated surfaces, `#00E5FF` vivid cyan optical accents, `#3B82F6` electric blue secondary highlights, `#F5FAFF` high-contrast primary typography, and `#A9B7C9` readable secondary copy.
- **Frosted Daylight Light Mode**: Adaptive high-contrast light palette (`#F4F7FB` canvas with `#0D1526` slate ink and `#0284C7` optical cyan) alongside Android 12+ Dynamic Material You color support.
- **100% Offline & Private**: QRYPT requests **no `INTERNET` permission**. Every scan, heuristic safety check, batch export, and generated barcode runs strictly on-device.

---

## Key Features

### 1. Real-Time CameraX + ML Kit Scanner
- **60fps Low-Latency Analysis**: Uses `STRATEGY_KEEP_ONLY_LATEST` backpressure to analyze frames immediately without frame queue lag.
- **Pinch-to-Zoom & Smooth Slider**: Supports both two-finger pinch gestures on the live preview and an interactive `1.0x – 5.0x` zoom slider.
- **Optical Reticle & Bounding Boxes**: Highlights live barcode coordinates and renders an animated laser scanline with auto-torch indicators.
- **Continuous Scan Mode**: Optional rapid-fire scanning mode that logs items directly into the Vault with an instant preview banner.
- **Zero-Permission Photo Library Scanning**: Scans saved screenshots and photos using the Android system `ActivityResultContracts.PickVisualMedia` picker without requesting storage permissions, with graceful error feedback when an image contains no valid code.
- **Built-In Demo / Self-Test**: Instant verification workflow containing sample Wi-Fi, Product EAN-13, vCard, URL, and Phishing Alert payloads for testing without a physical barcode in front of the lens.

### 2. Offline Anti-Phishing & URL Safety Heuristics
Every scanned link is inspected before opening in the browser (`SecurityAnalyzer.kt`):
- Detects insecure `http://` Cleartext protocols (`WARNING`).
- Flags raw IPv4 hostnames (`http://192.168...`), `@` credential-spoofing prefixes, punycode (`xn--`) homograph attacks, suspicious URL shorteners, excessive subdomain chaining, and brand-impersonation keywords (`DANGER`).

### 3. Batch Inventory Scanner
- **Rapid Multi-Item Queue**: Continuously captures barcodes into an in-session inventory list.
- **Duplicate Prevention**: Optional **Unique Only** filter prevents scanning the same barcode twice, while duplicate mode automatically increments item quantities (`×2`, `×3`, etc.).
- **One-Tap CSV & JSON Export**: Exports structured batch sessions (`qrypt_batch_*.csv` or `qrypt_batch_*.json`) via Android's `FileProvider` share sheet.

### 4. Encrypted-Feel Local History Vault
- **Room SQLite Persistence**: Stores both scanned and studio-generated codes locally with full-text search across display values, raw payloads, and custom notes.
- **Category & Security Filtering**: Filter instantly by `Favorites`, `Created`, `Security Alerts`, `URL`, `Wi-Fi`, `Product`, `Contact`, `Text`, and more.
- **Custom Annotations**: Attach searchable user notes to any scan record or export the entire Vault to CSV/JSON.

### 5. Optical QR & Barcode Studio
- **Multi-Format Creation**: Generate **URL**, **Wi-Fi Network**, **vCard Contact**, **Email**, **SMS**, **Phone**, **Geo Location**, **Calendar Event**, **Plain Text**, **Code 128**, **EAN-13 (with GS1 check-digit validation)**, and **PDF417**.
- **Custom Vector Aesthetics**: Customize module shapes (**Square**, **Rounded**, **Dots**), finder eyes, gradient palettes (**QRYPT Dark**, **Cyber Cyan**, **Electric Indigo**, **Emerald Matrix**, **Classic Ink**), and center badge overlays (**None**, **QRYPT**, **Wi-Fi**, **Link**, **Check**).
- **High-Res PNG Export**: Renders crisp 1024×1024 PNG images ready for sharing or printing.

---

## Application Screenshots

All screenshots below are **genuine renders** captured directly from the QRYPT Jetpack Compose UI via Robolectric native graphics (`./scripts/capture-screenshots.sh`):

| 1. Live Optical Scanner HUD | 2. Smart Result & Security Shield |
| :---: | :---: |
| <img src="docs/screenshots/01-scanner-screen.png" alt="QRYPT Live Scanner Screen" width="340" /> | <img src="docs/screenshots/02-scan-result-sheet.png" alt="QRYPT Scan Result Sheet" width="340" /> |
| **3. Continuous & 1D Laser Slit Mode** | **4. Batch Inventory Mode** |
| <img src="docs/screenshots/03-image-scan-and-modes.png" alt="QRYPT Continuous and 1D Laser Slit Mode" width="340" /> | <img src="docs/screenshots/04-batch-scanner-screen.png" alt="QRYPT Batch Inventory Screen" width="340" /> |
| **5. History Vault & Category Filters** | **6. Optical QR Studio (Dark)** |
| <img src="docs/screenshots/05-history-vault-screen.png" alt="QRYPT History Vault Screen" width="340" /> | <img src="docs/screenshots/06-generator-studio-screen.png" alt="QRYPT Generator Studio Screen" width="340" /> |
| **7. Settings & Engine Control (Dark)** | **8. Frosted Daylight Theme Preview** |
| <img src="docs/screenshots/07-settings-screen-dark.png" alt="QRYPT Settings Dark Screen" width="340" /> | <img src="docs/screenshots/08-light-theme-preview.png" alt="QRYPT Light Theme Preview" width="340" /> |

---

## Supported Barcode Symbologies

QRYPT decodes **13 1D and 2D barcode formats** out of the box and parses **12 structured payload schemas**:

| 2D Matrix Formats | 1D Linear Industrial & Retail Formats | Structured Payload Parsers |
| :--- | :--- | :--- |
| • **QR Code**<br>• **Aztec**<br>• **Data Matrix**<br>• **PDF417** | • **EAN-13** & **EAN-8**<br>• **UPC-A** & **UPC-E**<br>• **Code 128**, **Code 39**, **Code 93**<br>• **Codabar** & **ITF (Interleaved 2 of 5)** | • Website URLs (`http` / `https`)<br>• Wi-Fi (`WIFI:S:...;T:WPA;P:...;;`)<br>• vCard (`BEGIN:VCARD`) & MECARD<br>• Calendar (`BEGIN:VEVENT`)<br>• Email (`mailto:`, `MATMSG:`)<br>• SMS (`smsto:`) & Phone (`tel:`)<br>• Geo Coordinates (`geo:lat,lng`)<br>• Cryptographic Hashes & Plain Text |

---

## Architecture & Tech Stack

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Edge-to-edge host, bottom navigation & permissions flow
├── data/
│   ├── db/                          # Room Database (ScanRecordEntity, ScanDao, PrismScanDatabase)
│   └── preferences/                 # Jetpack DataStore (SettingsRepository, ThemeMode, AccentPreset)
├── domain/
│   ├── BarcodeModels.kt             # Domain models, ScanAction, SecurityAssessment, StudioStyleConfig
│   ├── BarcodePayloadParser.kt      # Structured parser for Wi-Fi, vCard, Geo, Calendar, GS1 EAN/UPC
│   └── SecurityAnalyzer.kt          # Offline URL anti-phishing & homograph heuristic analyzer
├── engine/
│   ├── BarcodeScannerEngine.kt      # CameraX ImageAnalysis + ML Kit BarcodeScanning + luminosity gauge
│   └── QrBitmapGenerator.kt         # Custom ZXing matrix renderer with vector eye/module styling
├── ui/
│   ├── components/                  # LiquidGlassCard, SegmentedPillRow, ScanResultSheet
│   ├── screens/                     # ScannerScreen, BatchScannerScreen, HistoryVaultScreen,
│   │                                # GeneratorStudioScreen, SettingsScreen
│   ├── theme/                       # Color.kt (#070B14 Midnight Navy & Daylight tokens), Theme.kt, Type.kt
│   └── viewmodel/                   # PrismScanViewModel (StateFlow reactive state management)
└── util/
    └── ExportHelper.kt              # FileProvider CSV, JSON, and 1024px PNG export helpers
```

---

## Privacy & Permissions

QRYPT follows strict least-privilege Android security practices:

| Permission | Type | Purpose |
| :--- | :--- | :--- |
| `android.permission.CAMERA` | Runtime (Optional) | Live optical camera scanning. Users who deny camera access can still scan from photos, browse the Vault, and generate QR codes in Studio. |
| `android.permission.VIBRATE` | Normal (Install-time) | Tactile haptic confirmation when a barcode locks onto the reticle. |

- **No `INTERNET` permission** — impossible for the app to transmit scans off-device.
- **No `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES` permissions** — uses the zero-permission Android system Photo Picker (`PickVisualMedia`).

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and [SECURITY.md](SECURITY.md) for full details.

---

## Installation & Sideloading Guide

### Download from GitHub Releases
1. Visit the [**Releases**](https://github.com/CodeByArnob/QRYPT/releases) page.
2. Download **`QRYPT-vX.Y.Z-release.apk`** (or the latest release asset) and optionally verify its **`SHA-256`** checksum against the `.sha256` file attached to the release.
3. Open the downloaded `.apk` file on your Android device (Android 8.0 / API 26 or newer).

> **Note on Google Play Protect when Sideloading:**
> When installing an independent open-source APK downloaded outside the Google Play Store using a self-managed signing certificate, Google Play Protect may display a prompt stating *"Play Protect hasn't seen an app from this developer before."*
> To complete installation: tap **More details** → **Install anyway**.
> For Updating Existing Installations: always use the same persistent release keystore configured in GitHub Actions secrets (`ANDROID_KEYSTORE_BASE64`) so Android verifies matching certificate signatures across updates without requiring an uninstall first.

---

## Building & Testing Locally

### Prerequisites
- **JDK 17**
- **Android SDK** (Compile SDK 36, Min SDK 26)

### 1. Assemble Debug & Release APKs
```bash
gradle :app:assembleDebug :app:assembleRelease
```

### 2. Run Unit & Robolectric Tests
Executes 14 unit and Robolectric tests covering payload parsing, URL phishing heuristics, EAN-13 GS1 checksum calculation, and Compose screen rendering:
```bash
gradle :app:testDebugUnitTest
```

### 3. Regenerate Genuine Application Screenshots
Renders all 7 application screens via Robolectric native graphics into `docs/screenshots/`:
```bash
./scripts/capture-screenshots.sh
```

---

## Automated GitHub Actions CI/CD

- **Continuous Integration (`.github/workflows/ci.yml`)**: Runs on every push and pull request to `main`. Executes unit & Robolectric tests, verifies genuine screenshots, runs Android Lint, builds Debug and Release APKs, and uploads build artifacts.
- **Automated Versioned Releases (`.github/workflows/release.yml`)**: Triggered on any `v*.*.*` tag (e.g., `v1.0.0`) or manual workflow dispatch. Signs the release APK using repository secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) with an automatic ephemeral CI keystore fallback when secrets are not yet configured, verifies the APK signature via `apksigner`, generates `.sha256` checksums, and publishes the GitHub Release.
- **GitHub Pages Landing Page (`.github/workflows/pages.yml`)**: Deploys the static showcase in `docs/` (`docs/index.html`, `docs/screenshots/`, `docs/assets/`) to GitHub Pages.

---

## Contributing & License

Contributions, bug reports, and feature requests are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md) before submitting a pull request.

- **Developer**: Arnobrazz (`arnobrazz.dev@gmail.com`)
- **Repository**: [https://github.com/CodeByArnob/QRYPT](https://github.com/CodeByArnob/QRYPT)
- **License**: Released under the [MIT License](LICENSE) — `Copyright (c) 2026 ArnobRazz`.
