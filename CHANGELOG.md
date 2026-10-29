# Changelog

All notable changes to **QRYPT QR & Barcode Scanner** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-29

### Added
- **Real-Time Dual-Engine Optical Scanner**:
  - Hardware-accelerated CameraX `Preview` and `ImageAnalysis` pipeline powered by **Google ML Kit Barcode Scanning** (`17.3.0`) and **ZXing Core** (`3.5.3`).
  - Support for all **13 major 1D & 2D barcode symbologies**: QR Code, Aztec, Data Matrix, PDF417, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, Codabar, and ITF (Interleaved 2 of 5).
  - Configurable viewfinder Regions of Interest (**Precision Square**, **1D Laser Slit**, and **Full Sensor**), pinch-to-zoom & slider zoom, tap-to-focus metering, hardware Torch toggle, Front/Back camera switching, and real-time Y-plane ambient luminance monitoring with automatic **Low-Light Exposure Boost**.
- **Zero-Permission Photo Library Scanning & Optical Self-Test**:
  - Decode single or multiple barcodes from static images using the modern Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) without requesting storage permissions.
  - Built-in **Optical Engine Self-Test Presets** that render real barcode bitmaps and pass them through the optical decoder pipeline for instant end-to-end verification.
- **Offline URL Anti-Phishing & Payload Parser**:
  - Zero-network heuristic inspection flagging unencrypted `http://` links, Punycode (`xn--`) homograph attacks, raw IPv4/IPv6 hosts, embedded `@` authority spoofing, high-risk TLDs, and redirect shorteners.
  - Automatic structured parsing and context-aware smart actions for Website URLs, Wi-Fi networks (`WIFI:`), vCard 3.0 & MECARD contacts, iCalendar events (`VEVENT`), Email (`mailto:` / `MATMSG:`), SMS (`smsto:`), Phone numbers (`tel:`), Geographic coordinates (`geo:`), Bookland ISBN-13/ISBN-10, and Retail GTIN products with **GS1 Country/Region Prefix Lookup** and Modulo-10 checksum verification.
- **Batch & Continuous Scanning Mode**:
  - Dedicated **Batch** workspace with session IDs, duplicate-scan cooldown guard, bulk warehouse/inventory note tagging, and one-tap **RFC-4180 CSV** and **JSON** export.
- **Optical Generator Studio**:
  - Real-time barcode & QR matrix generator supporting all 13 symbologies and 10 structured payload templates (URL, Wi-Fi, vCard, Plain Text, Retail GTIN, Email, SMS, Phone, Geo, Calendar).
  - Customizable resolution (`512px`, `1024px HD`, `1536px Print`), QR Error Correction (`L`, `M`, `Q`, `H`), rounded matrix module rendering, color pair presets, PNG image sharing, and direct Vault saving.
- **Local Optical Vault (Room SQLite)**:
  - 100% offline history persistence (`prism_scan_vault.db`) with instant full-text search, 9 category/security filters, favorite bookmarking, custom annotations, and bulk CSV/JSON export.
- **Midnight Navy & Vivid Cyan Liquid Glass UI**:
  - Cohesive dark theme (`#070B14` midnight background, `#0D1424` / `#121D30` elevated deep navy surfaces, `#00E5FF` vivid cyan primary accent, `#3B82F6` electric blue secondary accent, `#F5FAFF` primary text, `#A9B7C9` cool gray secondary text) and high-contrast **Frosted Daylight** light theme.
  - Instant 1-tap Dark/Light mode switcher in the top header bar, Android 12+ Material You Dynamic Color toggle, and 5 selectable accent wavelength presets.
- **CI/CD & Open-Source Release Automation**:
  - Automated GitHub Actions CI workflow (`.github/workflows/ci.yml`), versioned APK Release workflow with SHA-256 checksums (`.github/workflows/release.yml`), and GitHub Pages documentation site (`.github/workflows/pages.yml`).
