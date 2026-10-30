# Changelog

All notable changes to **QRYPT QR & Barcode Scanner** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] | 2026-10-30

### Added
1. **Smart Scan Result Actions**:
   1. Added context-aware primary and secondary action buttons across the Scan Result Bottom Sheet, Scanner quick-result banner, Batch Scanner cards, and History Vault records.
   2. Added **Open Link** for website URLs (`http://` and `https://`), **View Product** for identified retail barcodes, **Call** for telephone numbers, **Send Email** for email addresses, **Send Message** for SMS payloads, **View Location** for geographic coordinates, **Add Contact** for vCard and MECARD payloads, **Add Event** for calendar events, **Copy**, **Share**, and **Search Online**.
   3. Added a mandatory **External Action Confirmation Dialog** before opening external websites, dialers, email clients, or map applications, ensuring external links never launch without explicit user interaction.
2. **Smart QR Content Detection & Unsafe URI Rejection**:
   1. Enhanced `PayloadParser` to detect plain email addresses and formatted international/local phone numbers in addition to `mailto:`, `MATMSG:`, `tel:`, `smsto:`, `geo:`, `WIFI:`, `BEGIN:VCARD`, `MECARD:`, and `BEGIN:VEVENT` schemas.
   2. Added strict URI validation in `PayloadParser` and `ActionIntentHelper` to block unsafe URI schemes (`javascript:`, `data:`, `file:`, `vbscript:`, `intent:`, `content:`, `about:`, `blob:`, `jar:`) and classify them as suspicious non-executable text.
3. **Product Identification from Barcodes (Open Food Facts API v3 + Room Cache)**:
   1. Added `BarcodeValidator` supporting `EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `ISBN-10`, `ISBN-13`, and `GTIN-14` with GS1 Modulo-10 checksum verification and country prefix lookup.
   2. Added `OpenFoodFactsService` querying `https://world.openfoodfacts.org/api/v3/product/{barcode}.json` over HTTPS with custom `User-Agent: QRYPT/1.1.0 (https://github.com/CodeByArnob/QRYPT)` and full timeout/error handling.
   3. Added `ProductCacheEntity` and `ProductCacheDao` in Room SQLite (`prism_scan_vault.db` v2 with non-destructive `MIGRATION_1_2`) so previously identified products load instantaneously even when offline.
   4. Added `ProductIdentificationSection` and `ProductDetailsModalDialog` displaying product photo (via Coil), product name, brand, quantity, categories, ingredients, allergens, packaging, Nutri-Score grade (`A` to `E`), NOVA food processing group (`1` to `4`), nutrition facts per 100g, Open Database License (ODbL) attribution, **View Product** modal, and **Open Product Page** external browser action.
   5. Added user toggle in Settings (**Automatic Product Barcode Lookup**) to enable or disable automatic HTTPS product lookup on scan.

## [1.0.0] | 2026-10-29

### Added
1. **Real-Time Dual-Engine Optical Scanner**:
   1. Hardware-accelerated CameraX `Preview` and `ImageAnalysis` pipeline powered by **Google ML Kit Barcode Scanning** (`17.3.0`) and **ZXing Core** (`3.5.3`).
   2. Support for all 13 major 1D and 2D barcode symbologies: QR Code, Aztec, Data Matrix, PDF417, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, Codabar, and ITF (Interleaved 2 of 5).
   3. Configurable viewfinder Regions of Interest (**Precision Square**, **1D Laser Slit**, and **Full Sensor**), pinch-to-zoom and slider zoom, tap-to-focus metering, hardware Torch toggle, Front/Back camera switching, and real-time Y-plane ambient luminance monitoring with automatic **Low-Light Exposure Boost**.
2. **Zero-Permission Photo Library Scanning & Optical Self-Test**:
   1. Decode single or multiple barcodes from static images using the modern Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) without requesting storage permissions.
   2. Built-in **Optical Engine Self-Test Presets** that render real barcode bitmaps and pass them through the optical decoder pipeline for instant end-to-end verification.
3. **Offline URL Anti-Phishing & Payload Parser**:
   1. Heuristic inspection flagging unencrypted `http://` links, Punycode (`xn--`) homograph attacks, raw IPv4/IPv6 hosts, embedded `@` authority spoofing, high-risk TLDs, and redirect shorteners.
4. **Batch & Continuous Scanning Mode**:
   1. Dedicated **Batch** workspace with session IDs, duplicate-scan cooldown guard, bulk warehouse/inventory note tagging, and one-tap **RFC-4180 CSV** and **JSON** export.
5. **Optical Generator Studio**:
   1. Real-time barcode and QR matrix generator supporting all 13 symbologies and 10 structured payload templates with `512px`, `1024px HD`, and `1536px Print` PNG export.
6. **Local Optical Vault (Room SQLite)**:
   1. Local history persistence (`prism_scan_vault.db`) with instant full-text search, 9 category/security filters, favorite bookmarking, custom annotations, and bulk CSV/JSON export.
7. **Midnight Navy & Vivid Cyan Liquid Glass UI**:
   1. Cohesive dark theme (`#070B14` midnight background, `#0D1424` / `#121D30` elevated deep navy surfaces, `#00E5FF` vivid cyan primary accent, `#3B82F6` electric blue secondary accent) and high-contrast **Frosted Daylight** light theme.
