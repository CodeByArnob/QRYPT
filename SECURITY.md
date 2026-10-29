# Security Policy

## Supported Versions

The following versions of **QRYPT** currently receive security updates and maintenance:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

---

## Security Architecture

QRYPT is engineered with a strict **offline-first, zero-trust payload inspection** model:

1. **No Network Attack Surface**: QRYPT does not declare `android.permission.INTERNET` in `AndroidManifest.xml`. No scanned payloads, camera frames, or local database records can ever be transmitted over the network by the application.
2. **Offline Anti-Phishing URL Heuristics**: Every decoded URL is analyzed locally by `PayloadParser.analyzeUrlSecurity()` before any browser intent is launched. Links exhibiting unencrypted `http://`, Punycode (`xn--`) homograph characters, embedded `@` authority credentials, raw IPv4/IPv6 addresses, high-risk TLDs, or link shorteners trigger a prominent warning dialog requiring explicit user confirmation.
3. **Scoped File Sharing**: Exported CSV, JSON, and PNG files are shared exclusively via `androidx.core.content.FileProvider` (`android:exported="false"`) using temporary cache URIs with `FLAG_GRANT_READ_URI_PERMISSION`.
4. **Zero Broad Storage Permissions**: Image scanning uses the system-mediated Android Photo Picker (`ActivityResultContracts.PickVisualMedia`), preventing broad filesystem read access.

---

## Reporting a Vulnerability

If you discover a security vulnerability in QRYPT (such as a payload parser bypass, intent injection vector, or dependency vulnerability):

1. **Do not open a public GitHub issue** for unpatched security vulnerabilities.
2. Email a detailed report to **arnobrazz.dev@gmail.com** with the subject line `[SECURITY] QRYPT Vulnerability Report`.
3. Include steps to reproduce, sample barcode payload(s), and affected Android OS version.
4. You will receive an acknowledgment within **48 hours** and a remediation timeline once verified.
