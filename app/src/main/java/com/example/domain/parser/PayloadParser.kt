package com.example.domain.parser

import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.ParsedPayload
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.model.UrlSecurityReport
import java.net.URI
import java.util.Locale

object PayloadParser {

    private val SHORTENER_HOSTS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
        "buff.ly", "cutt.ly", "rb.gy", "shorturl.at", "tiny.cc"
    )

    private val HIGH_RISK_TLDS = setOf(
        ".zip", ".mov", ".top", ".xyz", ".tk", ".ml", ".ga", ".cf", ".gq", ".icu"
    )

    private val IPV4_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun parse(rawValue: String, format: BarcodeFormatType = BarcodeFormatType.QR_CODE): ParsedPayload {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) {
            return ParsedPayload(
                type = ParsedPayloadType.PLAIN_TEXT,
                rawValue = rawValue,
                primaryTitle = "Empty Code",
                subtitle = "No payload data"
            )
        }

        val upper = trimmed.uppercase(Locale.US)

        // 1. Wi-Fi Network
        if (upper.startsWith("WIFI:")) {
            return parseWifi(trimmed)
        }

        // 2. Contact Card (vCard or MECARD)
        if (upper.startsWith("BEGIN:VCARD") || upper.startsWith("MECARD:")) {
            return parseContact(trimmed)
        }

        // 3. Calendar Event (iCalendar VEVENT)
        if (upper.contains("BEGIN:VEVENT")) {
            return parseCalendar(trimmed)
        }

        // 4. Email (mailto: or MATMSG:)
        if (upper.startsWith("MAILTO:") || upper.startsWith("MATMSG:")) {
            return parseEmail(trimmed)
        }

        // 5. SMS (smsto: or sms:)
        if (upper.startsWith("SMSTO:") || upper.startsWith("SMS:")) {
            return parseSms(trimmed)
        }

        // 6. Phone (tel:)
        if (upper.startsWith("TEL:")) {
            val number = trimmed.substringAfter(":", "").trim()
            return ParsedPayload(
                type = ParsedPayloadType.PHONE,
                rawValue = trimmed,
                primaryTitle = number.ifEmpty { trimmed },
                subtitle = "Telephone Number",
                structuredFields = listOf("Phone" to number)
            )
        }

        // 7. Geographic Coordinates (geo:)
        if (upper.startsWith("GEO:")) {
            return parseGeo(trimmed)
        }

        // 8. Website URL
        if (upper.startsWith("HTTP://") || upper.startsWith("HTTPS://") || isLikelyBareUrl(trimmed)) {
            val normalizedUrl = if (upper.startsWith("HTTP://") || upper.startsWith("HTTPS://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
            val security = analyzeUrlSecurity(normalizedUrl, rawInputWasHttp = upper.startsWith("HTTP://"))
            return ParsedPayload(
                type = ParsedPayloadType.URL,
                rawValue = trimmed,
                primaryTitle = security.host.ifEmpty { trimmed },
                subtitle = "${security.scheme.uppercase(Locale.US)} • ${security.riskLevel.label}",
                structuredFields = buildList {
                    add("URL" to normalizedUrl)
                    add("Host" to security.host)
                    add("Protocol" to security.scheme.uppercase(Locale.US))
                    add("Security Status" to security.riskLevel.label)
                    if (security.warnings.isNotEmpty()) {
                        add("Risk Details" to security.warnings.joinToString(" • "))
                    }
                },
                urlSecurityReport = security
            )
        }

        // 9. ISBN / Retail Product (EAN-13, EAN-8, UPC-A, UPC-E, or 12/13-digit numeric)
        val digitsOnly = trimmed.all { it.isDigit() }
        if (digitsOnly && (format in listOf(
                BarcodeFormatType.EAN_13,
                BarcodeFormatType.EAN_8,
                BarcodeFormatType.UPC_A,
                BarcodeFormatType.UPC_E
            ) || trimmed.length in listOf(8, 12, 13))
        ) {
            if (trimmed.length == 13 && (trimmed.startsWith("978") || trimmed.startsWith("979"))) {
                val isbnValid = isValidGtin(trimmed)
                val isbn10 = convertIsbn13ToIsbn10(trimmed)
                return ParsedPayload(
                    type = ParsedPayloadType.ISBN,
                    rawValue = trimmed,
                    primaryTitle = "ISBN ${formatIsbn(trimmed)}",
                    subtitle = "Bookland Publication • ${if (isbnValid) "Valid Checksum" else "Unverified Checksum"}",
                    structuredFields = buildList {
                        add("ISBN-13" to formatIsbn(trimmed))
                        if (isbn10 != null) add("ISBN-10" to isbn10)
                        add("GS1 Prefix" to trimmed.take(3) + " (Bookland)")
                        add("Checksum" to if (isbnValid) "Verified (Modulo-10)" else "Invalid Check Digit")
                    },
                    productCountryOrigin = "International Bookland"
                )
            }

            val origin = lookupGs1Country(trimmed)
            val validCheck = isValidGtin(trimmed)
            return ParsedPayload(
                type = ParsedPayloadType.PRODUCT,
                rawValue = trimmed,
                primaryTitle = "GTIN $trimmed",
                subtitle = "${format.displayName} • $origin",
                structuredFields = listOf(
                    "Product Identifier" to trimmed,
                    "Symbology" to format.displayName,
                    "GS1 Registration Origin" to origin,
                    "GTIN Check Digit" to if (validCheck) "Verified (Modulo-10)" else "Custom / Non-standard"
                ),
                productCountryOrigin = origin
            )
        }

        // 10. Plain Text fallback
        return ParsedPayload(
            type = ParsedPayloadType.PLAIN_TEXT,
            rawValue = trimmed,
            primaryTitle = trimmed.lineSequence().firstOrNull()?.take(48) ?: trimmed.take(48),
            subtitle = "${format.displayName} • ${trimmed.length} chars",
            structuredFields = listOf(
                "Format" to format.displayName,
                "Character Count" to "${trimmed.length} characters",
                "Content" to trimmed
            )
        )
    }

    fun analyzeUrlSecurity(urlString: String, rawInputWasHttp: Boolean = false): UrlSecurityReport {
        val warnings = mutableListOf<String>()
        return try {
            val uri = URI(urlString.trim())
            val scheme = (uri.scheme ?: if (rawInputWasHttp) "http" else "https").lowercase(Locale.US)
            val host = (uri.host ?: urlString.substringAfter("://").substringBefore("/").substringBefore("?")).lowercase(Locale.US)
            val rawAuthority = uri.rawAuthority ?: ""

            if (scheme == "http" || rawInputWasHttp) {
                warnings.add("Unencrypted HTTP connection (traffic can be intercepted)")
            }
            if (rawAuthority.contains("@")) {
                warnings.add("Contains embedded '@' authority credentials (common domain spoofing technique)")
            }
            if (IPV4_REGEX.matches(host) || host.startsWith("[")) {
                warnings.add("Uses raw IP address instead of a verified domain name")
            }
            if (host.contains("xn--")) {
                warnings.add("Contains Punycode (xn--) internationalized characters; verify against homograph spoofing")
            }
            if (SHORTENER_HOSTS.contains(host)) {
                warnings.add("Shortened redirect link masks final destination URL")
            }
            if (HIGH_RISK_TLDS.any { host.endsWith(it) }) {
                warnings.add("Uses top-level domain frequently associated with deceptive downloads")
            }
            if (host.count { it == '.' } >= 4) {
                warnings.add("Unusually deep subdomain chain (${host.count { it == '.' } + 1} levels)")
            }

            val riskLevel = when {
                rawAuthority.contains("@") ||
                    IPV4_REGEX.matches(host) ||
                    host.contains("xn--") ||
                    HIGH_RISK_TLDS.any { host.endsWith(it) } ||
                    warnings.size >= 2 -> SecurityRiskLevel.SUSPICIOUS

                warnings.isNotEmpty() -> SecurityRiskLevel.CAUTION
                else -> SecurityRiskLevel.SAFE
            }

            UrlSecurityReport(
                riskLevel = riskLevel,
                host = host.ifEmpty { urlString },
                scheme = scheme,
                warnings = warnings
            )
        } catch (_: Exception) {
            UrlSecurityReport(
                riskLevel = SecurityRiskLevel.SUSPICIOUS,
                host = urlString.take(40),
                scheme = "unknown",
                warnings = listOf("Malformed URI syntax — inspect carefully before opening")
            )
        }
    }

    private fun parseWifi(raw: String): ParsedPayload {
        val body = raw.substringAfter(":", "")
        val tokens = parseSemicolonTokens(body)
        val ssid = tokens["S"] ?: "Hidden Network"
        val auth = (tokens["T"] ?: "NOPASS").uppercase(Locale.US)
        val password = tokens["P"] ?: ""
        val hidden = tokens["H"]?.equals("true", ignoreCase = true) == true

        val fields = buildList {
            add("Network Name (SSID)" to ssid)
            add("Security Type" to if (auth == "NOPASS" || auth.isEmpty()) "Open (No Password)" else auth)
            if (password.isNotEmpty()) add("Password" to password)
            if (hidden) add("Hidden SSID" to "Yes")
        }

        return ParsedPayload(
            type = ParsedPayloadType.WIFI,
            rawValue = raw,
            primaryTitle = ssid,
            subtitle = "Wi-Fi • ${if (auth == "NOPASS") "Open" else auth}",
            structuredFields = fields
        )
    }

    private fun parseContact(raw: String): ParsedPayload {
        if (raw.uppercase(Locale.US).startsWith("MECARD:")) {
            val tokens = parseSemicolonTokens(raw.substringAfter(":"))
            val name = tokens["N"]?.replace(",", " ")?.trim().orEmpty().ifEmpty { "Contact" }
            val tel = tokens["TEL"].orEmpty()
            val email = tokens["EMAIL"].orEmpty()
            val org = tokens["ORG"].orEmpty()
            val adr = tokens["ADR"].orEmpty()
            val url = tokens["URL"].orEmpty()
            val note = tokens["NOTE"].orEmpty()

            return ParsedPayload(
                type = ParsedPayloadType.CONTACT_VCARD,
                rawValue = raw,
                primaryTitle = name,
                subtitle = listOfNotNull(org.takeIf { it.isNotEmpty() }, tel.takeIf { it.isNotEmpty() })
                    .joinToString(" • ").ifEmpty { "Business Card (MECARD)" },
                structuredFields = buildList {
                    add("Full Name" to name)
                    if (org.isNotEmpty()) add("Organization" to org)
                    if (tel.isNotEmpty()) add("Phone" to tel)
                    if (email.isNotEmpty()) add("Email" to email)
                    if (adr.isNotEmpty()) add("Address" to adr)
                    if (url.isNotEmpty()) add("Website" to url)
                    if (note.isNotEmpty()) add("Note" to note)
                }
            )
        }

        var fullName = ""
        var org = ""
        var title = ""
        var phone = ""
        var email = ""
        var url = ""
        var address = ""

        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val key = trimmed.substringBefore(":").substringBefore(";").uppercase(Locale.US)
            val value = trimmed.substringAfter(":", "").trim()
            when (key) {
                "FN" -> fullName = value
                "N" -> if (fullName.isEmpty()) {
                    fullName = value.split(";").filter { it.isNotBlank() }.reversed().joinToString(" ")
                }
                "ORG" -> org = value.replace(";", " ").trim()
                "TITLE" -> title = value
                "TEL" -> if (phone.isEmpty()) phone = value
                "EMAIL" -> if (email.isEmpty()) email = value
                "URL" -> if (url.isEmpty()) url = value
                "ADR" -> if (address.isEmpty()) {
                    address = value.split(";").filter { it.isNotBlank() }.joinToString(", ")
                }
            }
        }

        val display = fullName.ifEmpty { org.ifEmpty { "Contact vCard" } }
        return ParsedPayload(
            type = ParsedPayloadType.CONTACT_VCARD,
            rawValue = raw,
            primaryTitle = display,
            subtitle = listOfNotNull(title.takeIf { it.isNotEmpty() }, org.takeIf { it.isNotEmpty() }, phone.takeIf { it.isNotEmpty() })
                .joinToString(" • ").ifEmpty { "vCard Contact" },
            structuredFields = buildList {
                if (fullName.isNotEmpty()) add("Name" to fullName)
                if (title.isNotEmpty()) add("Role / Title" to title)
                if (org.isNotEmpty()) add("Organization" to org)
                if (phone.isNotEmpty()) add("Phone" to phone)
                if (email.isNotEmpty()) add("Email" to email)
                if (address.isNotEmpty()) add("Address" to address)
                if (url.isNotEmpty()) add("Website" to url)
            }
        )
    }

    private fun parseCalendar(raw: String): ParsedPayload {
        var summary = ""
        var location = ""
        var dtStart = ""
        var dtEnd = ""
        var description = ""

        raw.lineSequence().forEach { line ->
            val key = line.substringBefore(":").substringBefore(";").trim().uppercase(Locale.US)
            val value = line.substringAfter(":", "").trim()
            when (key) {
                "SUMMARY" -> summary = value
                "LOCATION" -> location = value
                "DTSTART" -> dtStart = value
                "DTEND" -> dtEnd = value
                "DESCRIPTION" -> description = value
            }
        }

        val displayTitle = summary.ifEmpty { "Calendar Event" }
        return ParsedPayload(
            type = ParsedPayloadType.CALENDAR,
            rawValue = raw,
            primaryTitle = displayTitle,
            subtitle = location.ifEmpty { "iCalendar Event" },
            structuredFields = buildList {
                add("Event Title" to displayTitle)
                if (location.isNotEmpty()) add("Location" to location)
                if (dtStart.isNotEmpty()) add("Starts" to dtStart)
                if (dtEnd.isNotEmpty()) add("Ends" to dtEnd)
                if (description.isNotEmpty()) add("Details" to description)
            }
        )
    }

    private fun parseEmail(raw: String): ParsedPayload {
        if (raw.uppercase(Locale.US).startsWith("MATMSG:")) {
            val tokens = parseSemicolonTokens(raw.substringAfter(":"))
            val to = tokens["TO"].orEmpty()
            val sub = tokens["SUB"].orEmpty()
            val body = tokens["BODY"].orEmpty()
            return ParsedPayload(
                type = ParsedPayloadType.EMAIL,
                rawValue = raw,
                primaryTitle = to.ifEmpty { "Email Message" },
                subtitle = sub.ifEmpty { "MATMSG Email" },
                structuredFields = buildList {
                    if (to.isNotEmpty()) add("Recipient" to to)
                    if (sub.isNotEmpty()) add("Subject" to sub)
                    if (body.isNotEmpty()) add("Message" to body)
                }
            )
        }

        val withoutScheme = raw.substringAfter(":")
        val address = withoutScheme.substringBefore("?")
        val query = withoutScheme.substringAfter("?", "")
        val params = parseQueryParams(query)
        val subject = params["subject"].orEmpty()
        val body = params["body"].orEmpty()

        return ParsedPayload(
            type = ParsedPayloadType.EMAIL,
            rawValue = raw,
            primaryTitle = address.ifEmpty { "Email Recipient" },
            subtitle = subject.ifEmpty { "Direct Email Link" },
            structuredFields = buildList {
                add("Recipient" to address)
                if (subject.isNotEmpty()) add("Subject" to subject)
                if (body.isNotEmpty()) add("Body" to body)
            }
        )
    }

    private fun parseSms(raw: String): ParsedPayload {
        val content = raw.substringAfter(":")
        val number = content.substringBefore(":").substringBefore("?")
        val message = when {
            content.contains(":") -> content.substringAfter(":")
            content.contains("?body=") -> content.substringAfter("?body=")
            else -> ""
        }
        return ParsedPayload(
            type = ParsedPayloadType.SMS,
            rawValue = raw,
            primaryTitle = number.ifEmpty { "SMS Message" },
            subtitle = message.ifEmpty { "SMS Recipient" },
            structuredFields = buildList {
                add("Phone Number" to number)
                if (message.isNotEmpty()) add("Message Body" to message)
            }
        )
    }

    private fun parseGeo(raw: String): ParsedPayload {
        val body = raw.substringAfter(":")
        val coordsPart = body.substringBefore("?")
        val queryPart = body.substringAfter("?", "")
        val coords = coordsPart.split(",")
        val lat = coords.getOrNull(0)?.trim().orEmpty()
        val lng = coords.getOrNull(1)?.trim().orEmpty()
        val label = parseQueryParams(queryPart)["q"].orEmpty()

        return ParsedPayload(
            type = ParsedPayloadType.GEO,
            rawValue = raw,
            primaryTitle = if (label.isNotEmpty()) label else "$lat, $lng",
            subtitle = "Geographic Coordinates",
            structuredFields = buildList {
                if (lat.isNotEmpty()) add("Latitude" to lat)
                if (lng.isNotEmpty()) add("Longitude" to lng)
                if (label.isNotEmpty()) add("Query / Label" to label)
            }
        )
    }

    private fun isLikelyBareUrl(text: String): Boolean {
        if (text.contains(" ") || text.contains("\n")) return false
        val lower = text.lowercase(Locale.US)
        return lower.startsWith("www.") && lower.count { it == '.' } >= 2
    }

    private fun parseSemicolonTokens(input: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val parts = input.split(";")
        for (part in parts) {
            if (part.contains(":")) {
                val key = part.substringBefore(":").trim().uppercase(Locale.US)
                val value = part.substringAfter(":").trim()
                if (key.isNotEmpty() && !result.containsKey(key)) {
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = pair.substring(0, idx).lowercase(Locale.US)
                val v = pair.substring(idx + 1).replace("+", " ")
                k to v
            } else null
        }.toMap()
    }

    fun isValidGtin(digits: String): Boolean {
        if (digits.length !in listOf(8, 12, 13, 14) || !digits.all { it.isDigit() }) return false
        val expectedCheck = calculateGtinCheckDigit(digits.dropLast(1))
        return digits.last().digitToInt() == expectedCheck
    }

    fun calculateGtinCheckDigit(payloadWithoutCheck: String): Int {
        var sum = 0
        val reversed = payloadWithoutCheck.reversed()
        for ((index, ch) in reversed.withIndex()) {
            val digit = ch.digitToInt()
            val weight = if (index % 2 == 0) 3 else 1
            sum += digit * weight
        }
        return (10 - (sum % 10)) % 10
    }

    fun normalizeGtinForFormat(input: String, format: BarcodeFormatType): String {
        val digits = input.filter { it.isDigit() }
        return when (format) {
            BarcodeFormatType.EAN_13 -> {
                val base12 = digits.padEnd(12, '0').take(12)
                base12 + calculateGtinCheckDigit(base12)
            }
            BarcodeFormatType.EAN_8 -> {
                val base7 = digits.padEnd(7, '0').take(7)
                base7 + calculateGtinCheckDigit(base7)
            }
            BarcodeFormatType.UPC_A -> {
                val base11 = digits.padEnd(11, '0').take(11)
                base11 + calculateGtinCheckDigit(base11)
            }
            BarcodeFormatType.UPC_E -> {
                val base7 = ("0" + digits.dropWhile { it == '0' }).padEnd(7, '0').take(7)
                base7 + calculateGtinCheckDigit(base7)
            }
            else -> input
        }
    }

    private fun formatIsbn(isbn13: String): String {
        if (isbn13.length != 13) return isbn13
        return "${isbn13.substring(0, 3)}-${isbn13[3]}-${isbn13.substring(4, 9)}-${isbn13.substring(9, 12)}-${isbn13[12]}"
    }

    private fun convertIsbn13ToIsbn10(isbn13: String): String? {
        if (isbn13.length != 13 || !isbn13.startsWith("978")) return null
        val core = isbn13.substring(3, 12)
        var sum = 0
        for (i in 0 until 9) {
            sum += (10 - i) * core[i].digitToInt()
        }
        val rem = (11 - (sum % 11)) % 11
        val check = if (rem == 10) "X" else rem.toString()
        return "$core$check"
    }

    fun lookupGs1Country(gtin: String): String {
        val normalized = if (gtin.length == 12) "0$gtin" else gtin
        if (normalized.length < 3) return "GS1 Standard Retail"
        val prefix = normalized.take(3).toIntOrNull() ?: return "GS1 Standard Retail"
        return when (prefix) {
            in 0..19 -> "United States & Canada (GS1 US)"
            in 20..29 -> "In-Store Restricted / Variable Weight"
            in 30..39 -> "United States Drugs / National Drug Code"
            in 40..49 -> "Restricted Circulation / Company Internal"
            in 50..59 -> "GS1 US Coupons"
            in 60..139 -> "United States & Canada"
            in 300..379 -> "France & Monaco (GS1 France)"
            in 400..440 -> "Germany (GS1 Germany)"
            in 450..459, in 490..499 -> "Japan (GS1 Japan)"
            in 460..469 -> "Russia & CIS"
            471 -> "Taiwan"
            480 -> "Philippines"
            489 -> "Hong Kong"
            in 500..509 -> "United Kingdom (GS1 UK)"
            520, 521 -> "Greece"
            539 -> "Ireland"
            in 540..549 -> "Belgium & Luxembourg"
            560 -> "Portugal"
            569 -> "Iceland"
            in 570..579 -> "Denmark & Faroe Islands"
            590 -> "Poland (GS1 Poland)"
            599 -> "Hungary"
            in 600..601 -> "South Africa"
            in 640..649 -> "Finland"
            in 690..699 -> "China"
            in 700..709 -> "Norway"
            729 -> "Israel"
            in 730..739 -> "Sweden"
            750 -> "Mexico"
            in 754..755 -> "Canada"
            in 760..769 -> "Switzerland & Liechtenstein"
            in 789..790 -> "Brazil"
            in 800..839 -> "Italy & San Marino"
            in 840..849 -> "Spain & Andorra"
            869 -> "Turkey"
            in 870..879 -> "Netherlands"
            880 -> "South Korea"
            885 -> "Thailand"
            888 -> "Singapore"
            890 -> "India (GS1 India)"
            893 -> "Vietnam"
            899 -> "Indonesia"
            in 900..919 -> "Austria"
            in 930..939 -> "Australia"
            in 940..949 -> "New Zealand"
            955 -> "Malaysia"
            977 -> "Serial Publications (ISSN)"
            978, 979 -> "Bookland (ISBN)"
            else -> "GS1 International (Prefix $prefix)"
        }
    }

    // Builders for Generator Studio
    fun buildWifiPayload(ssid: String, password: String, encryption: String, hidden: Boolean): String {
        val cleanSsid = ssid.replace(";", "\\;").replace(":", "\\:")
        val cleanPass = password.replace(";", "\\;").replace(":", "\\:")
        val auth = encryption.uppercase(Locale.US)
        return "WIFI:T:$auth;S:$cleanSsid;P:$cleanPass;H:${if (hidden) "true" else "false"};;"
    }

    fun buildVCardPayload(
        fullName: String,
        organization: String,
        jobTitle: String,
        phone: String,
        email: String,
        website: String,
        address: String
    ): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${fullName.trim()}")
        if (organization.isNotBlank()) appendLine("ORG:${organization.trim()}")
        if (jobTitle.isNotBlank()) appendLine("TITLE:${jobTitle.trim()}")
        if (phone.isNotBlank()) appendLine("TEL;TYPE=CELL:${phone.trim()}")
        if (email.isNotBlank()) appendLine("EMAIL:${email.trim()}")
        if (website.isNotBlank()) appendLine("URL:${website.trim()}")
        if (address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${address.trim()};;;;")
        append("END:VCARD")
    }

    fun buildEmailPayload(to: String, subject: String, body: String): String {
        return "MATMSG:TO:${to.trim()};SUB:${subject.trim()};BODY:${body.trim()};;"
    }

    fun buildSmsPayload(phone: String, message: String): String {
        return "SMSTO:${phone.trim()}:${message.trim()}"
    }

    fun buildGeoPayload(latitude: String, longitude: String, label: String): String {
        val lat = latitude.trim().ifEmpty { "37.4220" }
        val lng = longitude.trim().ifEmpty { "-122.0841" }
        return if (label.isNotBlank()) {
            "geo:$lat,$lng?q=${label.trim()}"
        } else {
            "geo:$lat,$lng"
        }
    }

    fun buildCalendarPayload(
        title: String,
        location: String,
        startIso: String,
        endIso: String,
        description: String
    ): String = buildString {
        appendLine("BEGIN:VEVENT")
        appendLine("SUMMARY:${title.trim()}")
        if (location.isNotBlank()) appendLine("LOCATION:${location.trim()}")
        if (startIso.isNotBlank()) appendLine("DTSTART:${startIso.trim()}")
        if (endIso.isNotBlank()) appendLine("DTEND:${endIso.trim()}")
        if (description.isNotBlank()) appendLine("DESCRIPTION:${description.trim()}")
        append("END:VEVENT")
    }
}
