# Privacy Policy: QRYPT QR & Barcode Scanner

**Effective Date:** October 29, 2026  
**Developer:** Arnobrazz (`arnobrazz.dev@gmail.com`)  
**Application:** QRYPT (`com.arnobrazz.qrypt`)

---

## 1. 100% On-Device & Offline Guarantee

**QRYPT** is built from the ground up as a privacy-first utility. The application does **not** request `android.permission.INTERNET` and contains **zero** analytics SDKs, advertising trackers, crash telemetry uploaders, or cloud synchronization services.

- **No Personal Data Collection**: QRYPT never collects, transmits, sells, or shares your personal information, camera frames, location, contacts, or scan history with the developer or any third party.
- **No Account Required**: All features (including real-time scanning, batch mode, Optical Generator Studio, and the Local Optical Vault) work immediately without sign-in or registration.

---

## 2. Permissions & Hardware Access

QRYPT follows strict least-privilege permission principles and declares only two permissions in `AndroidManifest.xml`:

| Permission | Type | Purpose |
| :--- | :--- | :--- |
| `android.permission.CAMERA` | Runtime (Optional) | Used exclusively for real-time on-device optical barcode and QR code recognition via CameraX and Google ML Kit. Camera frames are analyzed in volatile memory and immediately discarded; no images or video are ever recorded or transmitted. |
| `android.permission.VIBRATE` | Install-time | Used to provide a brief tactile haptic pulse when a barcode is decoded (can be disabled anytime in **Settings**). |

### Photo Library Scanning (Zero Storage Permissions)
When you choose **Scan Image** / **Photo Library**, QRYPT uses the system **Android Photo Picker** (`ActivityResultContracts.PickVisualMedia`). This grants temporary read access only to the single image you explicitly select, without requiring `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE`.

---

## 3. Local Data Storage & User Control

- **Optical Vault Database**: Scanned and generated codes are stored locally on your device inside the application's private sandbox (`prism_scan_vault.db` via AndroidX Room SQLite).
- **Preferences**: Theme and scanner preferences are stored locally via AndroidX DataStore (`prism_scan_settings`).
- **Complete User Control**:
  - You can delete individual scan records at any time.
  - You can clear non-favorite history or permanently purge the entire local database in **Settings → Privacy & Local Vault Management → Purge Vault**.
  - Uninstalling the QRYPT application immediately and permanently deletes all local SQLite and DataStore files from your device.

---

## 4. Contact

If you have any questions about this Privacy Policy or QRYPT's security practices, please contact:
- **Developer**: Arnobrazz
- **Email**: `arnobrazz.dev@gmail.com`
- **GitHub Repository**: [https://github.com/CodeByArnob/QRYPT](https://github.com/CodeByArnob/QRYPT)
