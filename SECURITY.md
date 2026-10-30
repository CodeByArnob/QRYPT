# Security Policy

## Supported Versions

The following versions of **QRYPT** currently receive security updates and maintenance:

| Version | Supported |
| :--- | :--- |
| 1.1.x | Yes |
| 1.0.x | Yes |

---

## Security Architecture

QRYPT is engineered with a strict **zero-trust payload inspection** model:

1. **Mandatory User Confirmation for External Links**: QRYPT never automatically opens external links after scanning. Every external action (`Open Link`, `Call`, `Send Email`, `Send Message`, `View Location`, `Add Contact`) requires explicit user interaction and confirmation.
2. **Strict URI Scheme Allowlisting**: Only validated `http://` and `https://` URLs with valid hostnames can be opened in an external browser. Unsafe URI schemes (`javascript:`, `data:`, `file:`, `vbscript:`, `intent:`, `content:`, `about:`, `blob:`, `jar:`) are blocked by both `PayloadParser` and `ActionIntentHelper`.
3. **On-Device Anti-Phishing URL Heuristics**: Every decoded URL is analyzed locally by `PayloadParser.analyzeUrlSecurity()` before any browser intent is launched. Links exhibiting unencrypted `http://`, Punycode (`xn--`) homograph characters, embedded `@` authority credentials, raw IPv4/IPv6 addresses, high-risk TLDs, or link shorteners trigger a prominent warning dialog.
4. **Scoped HTTPS Product Lookup**: When a retail GTIN barcode (`EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `ISBN`) is scanned, only the validated numeric barcode digits are sent over HTTPS to `https://world.openfoodfacts.org/api/v3/product/{barcode}.json`. Camera images, gallery photos, QR code payloads, and scan history are never transmitted.
5. **Scoped File Sharing**: Exported CSV, JSON, and PNG files are shared exclusively via `androidx.core.content.FileProvider` (`android:exported="false"`) using temporary cache URIs with `FLAG_GRANT_READ_URI_PERMISSION`.
6. **Zero Broad Storage Permissions**: Image scanning uses the system-mediated Android Photo Picker (`ActivityResultContracts.PickVisualMedia`), preventing broad filesystem read access.

---

## Reporting a Vulnerability

If you discover a security vulnerability in QRYPT (such as a payload parser bypass, intent injection vector, or dependency vulnerability):

1. **Do not open a public GitHub issue** for unpatched security vulnerabilities.
2. Email a detailed report to **arnobrazz.dev@gmail.com** with the subject line `[SECURITY] QRYPT Vulnerability Report`.
3. Include steps to reproduce, sample barcode payload(s), and affected Android OS version.
4. You will receive an acknowledgment within **48 hours** and a remediation timeline once verified.
