# Privacy Policy: QRYPT QR & Barcode Scanner

**Effective Date:** October 30, 2026  
**Developer:** ArnobRazz (`arnobrazz.dev@gmail.com`)  
**Application:** QRYPT (`com.arnobrazz.qrypt`)  
**Current Version:** v1.1.0

---

## 1. On-Device Scanning & Privacy-First Architecture

**QRYPT** is engineered from the ground up as a privacy-first optical scanner and barcode utility.

1. **On-Device Optical Decoding**: All live camera frames and gallery photos are decoded 100% locally on your device using bundled Google ML Kit Barcode Scanning and ZXing Core. Camera frames and photos are never uploaded to any server.
2. **Zero Tracking or Analytics**: QRYPT contains zero analytics SDKs, advertising trackers, crash telemetry uploaders, or cloud account services.
3. **Optional Product Barcode Lookup**: When you scan a retail product barcode (`EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `ISBN`, or `GTIN-14`) and **Automatic Product Barcode Lookup** is enabled in Settings, QRYPT sends only the numeric barcode digits over an encrypted `HTTPS` request to the public [Open Food Facts API](https://world.openfoodfacts.org/) (`https://world.openfoodfacts.org/api/v3/product/{barcode}.json`) to retrieve public product metadata (product name, brand, image, ingredients, and nutrition facts). QR codes, website links, Wi-Fi credentials, contacts, and plain text codes are never sent to any external service. You can disable automatic product lookup at any time in **Settings**.

---

## 2. Permissions & Hardware Access

QRYPT follows strict least-privilege permission principles and declares only the following permissions in `AndroidManifest.xml`:

| Permission | Type | Purpose |
| :--- | :--- | :--- |
| `android.permission.CAMERA` | Runtime (Optional) | Used exclusively for real-time on-device optical barcode and QR code recognition via CameraX and Google ML Kit. Camera frames are analyzed in volatile memory and immediately discarded. |
| `android.permission.INTERNET` | Install-time | Used strictly to query public product information and product images from Open Food Facts over HTTPS when a retail product barcode is scanned. |
| `android.permission.ACCESS_NETWORK_STATE` | Install-time | Used to check whether the device is online or offline before attempting a product barcode lookup. |
| `android.permission.VIBRATE` | Install-time | Used to provide a brief tactile haptic pulse when a barcode is decoded (can be disabled anytime in **Settings**). |

### Photo Library Scanning (Zero Storage Permissions)
When you choose **Photo Library**, QRYPT uses the system **Android Photo Picker** (`ActivityResultContracts.PickVisualMedia`). This grants temporary read access only to the single image you explicitly select, without requiring `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE`.

---

## 3. Local Data Storage & User Control

1. **Optical Vault Database**: Scanned and generated codes, along with cached product lookup metadata, are stored locally on your device inside the application's private sandbox (`prism_scan_vault.db` via AndroidX Room SQLite).
2. **Preferences**: Theme and scanner preferences are stored locally via AndroidX DataStore (`prism_scan_settings`).
3. **Complete User Control**:
   1. You can delete individual scan records at any time.
   2. You can clear non-favorite history or permanently purge the entire local database and product cache in **Settings > Privacy & Local Vault Management > Purge Vault**.
   3. Uninstalling the QRYPT application immediately and permanently deletes all local SQLite and DataStore files from your device.

---

## 4. Contact

If you have any questions about this Privacy Policy or QRYPT's security practices, please contact:

1. **Developer**: ArnobRazz
2. **Email**: `arnobrazz.dev@gmail.com`
3. **GitHub Repository**: [https://github.com/CodeByArnob/QRYPT](https://github.com/CodeByArnob/QRYPT)
