package com.example

import com.example.data.local.ScanRecordEntity
import com.example.data.repository.ScanRepository
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.BarcodeValidator
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.parser.PayloadParser
import com.example.engine.BarcodeGeneratorEngine
import com.example.util.ActionIntentHelper
import com.example.util.UrlValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `parse Wi-Fi payload extracts SSID, encryption, and password`() {
        val raw = "WIFI:T:WPA;S:QRYPT-Optics-5G;P:Qrypt#2026!;H:false;;"
        val parsed = PayloadParser.parse(raw, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.WIFI, parsed.type)
        assertEquals("QRYPT-Optics-5G", parsed.primaryTitle)
        val map = parsed.structuredFields.toMap()
        assertEquals("WPA", map["Security Type"])
        assertEquals("Qrypt#2026!", map["Password"])
    }

    @Test
    fun `parse vCard and MECARD payloads extract contact fields accurately`() {
        val vcard = PayloadParser.buildVCardPayload(
            fullName = "Arnob Razz",
            organization = "QRYPT Optical Systems",
            jobTitle = "Principal Android Engineer",
            phone = "+14158904412",
            email = "arnobrazz.dev@gmail.com",
            website = "https://github.com/CodeByArnob/QRYPT",
            address = "450 Mission St"
        )
        val parsed = PayloadParser.parse(vcard, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.CONTACT_VCARD, parsed.type)
        assertEquals("Arnob Razz", parsed.primaryTitle)
        val map = parsed.structuredFields.toMap()
        assertEquals("QRYPT Optical Systems", map["Organization"])
        assertEquals("Principal Android Engineer", map["Role / Title"])

        val mecard = "MECARD:N:Vance,Elena;ORG:Aether Lab;TEL:+14155550199;EMAIL:elena@aether.io;;"
        val parsedMecard = PayloadParser.parse(mecard, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.CONTACT_VCARD, parsedMecard.type)
        assertTrue(parsedMecard.primaryTitle.contains("Vance"))
    }

    @Test
    fun `parse Calendar, Email, SMS, Phone, Geo, and Plain Text payloads`() {
        val calPayload = PayloadParser.buildCalendarPayload(
            title = "QRYPT Release Review",
            location = "Hall 3",
            startIso = "20261104T093000Z",
            endIso = "20261104T103000Z",
            description = "v1.1.0 APK Audit"
        )
        val parsedCal = PayloadParser.parse(calPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.CALENDAR, parsedCal.type)
        assertEquals("QRYPT Release Review", parsedCal.primaryTitle)

        val emailPayload = PayloadParser.buildEmailPayload("arnobrazz.dev@gmail.com", "Audit", "Ready")
        val parsedEmail = PayloadParser.parse(emailPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.EMAIL, parsedEmail.type)
        assertEquals("arnobrazz.dev@gmail.com", parsedEmail.primaryTitle)

        // Plain email QR detection
        val plainEmail = PayloadParser.parse("contact@qrypt.dev", BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.EMAIL, plainEmail.type)
        assertEquals("contact@qrypt.dev", plainEmail.primaryTitle)

        val smsPayload = PayloadParser.buildSmsPayload("+14158910192", "Verify code")
        val parsedSms = PayloadParser.parse(smsPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.SMS, parsedSms.type)
        assertEquals("+14158910192", parsedSms.primaryTitle)

        val parsedPhone = PayloadParser.parse("tel:+14158910192", BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.PHONE, parsedPhone.type)
        assertEquals("+14158910192", parsedPhone.primaryTitle)

        // Plain international phone QR detection
        val plainPhone = PayloadParser.parse("+1 (415) 890-4412", BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.PHONE, plainPhone.type)

        val geoPayload = PayloadParser.buildGeoPayload("37.7897", "-122.4014", "SF HQ")
        val parsedGeo = PayloadParser.parse(geoPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.GEO, parsedGeo.type)
        assertEquals("SF HQ", parsedGeo.primaryTitle)

        // Plain text QR result
        val plainText = PayloadParser.parse("Warehouse Rack B-14 Serial 99412", BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.PLAIN_TEXT, plainText.type)
        assertEquals("Warehouse Rack B-14 Serial 99412", plainText.primaryTitle)
    }

    @Test
    fun `reject unsafe URI schemes and validate safe HTTP and HTTPS URLs`() {
        val unsafePayloads = listOf(
            "javascript:alert(document.cookie)",
            "data:text/html,<script>alert(1)</script>",
            "file:///data/data/com.arnobrazz.qrypt/shared_prefs",
            "intent://scan/#Intent;scheme=zxing;end",
            "vbscript:msgbox(1)",
            "content://com.android.contacts/contacts"
        )

        for (unsafe in unsafePayloads) {
            assertTrue(PayloadParser.hasUnsafeUriScheme(unsafe))
            val validation = ActionIntentHelper.validateExternalHttpUrl(unsafe)
            assertTrue("Expected Rejected for $unsafe", validation is UrlValidationResult.Rejected)
            val parsed = PayloadParser.parse(unsafe, BarcodeFormatType.QR_CODE)
            assertEquals("Unsafe payload $unsafe must not be classified as URL", ParsedPayloadType.PLAIN_TEXT, parsed.type)
            assertEquals(SecurityRiskLevel.SUSPICIOUS, parsed.urlSecurityReport?.riskLevel)
        }

        val validHttps = ActionIntentHelper.validateExternalHttpUrl("https://github.com/CodeByArnob/QRYPT")
        assertTrue(validHttps is UrlValidationResult.Valid)
        assertEquals("https://github.com/CodeByArnob/QRYPT", (validHttps as UrlValidationResult.Valid).normalizedUrl)

        val bareDomain = ActionIntentHelper.validateExternalHttpUrl("github.com/CodeByArnob/QRYPT")
        assertTrue(bareDomain is UrlValidationResult.Valid)
        assertEquals("https://github.com/CodeByArnob/QRYPT", (bareDomain as UrlValidationResult.Valid).normalizedUrl)
    }

    @Test
    fun `analyzeUrlSecurity flags punycode, raw IP, shorteners, and unencrypted HTTP`() {
        val safe = PayloadParser.analyzeUrlSecurity("https://github.com/CodeByArnob/QRYPT")
        assertEquals(SecurityRiskLevel.SAFE, safe.riskLevel)
        assertTrue(safe.warnings.isEmpty())

        val suspicious = PayloadParser.analyzeUrlSecurity("http://admin@xn--pple-43d.top/login", rawInputWasHttp = true)
        assertEquals(SecurityRiskLevel.SUSPICIOUS, suspicious.riskLevel)
        assertTrue(suspicious.warnings.size >= 3)

        val ipBased = PayloadParser.analyzeUrlSecurity("http://192.168.1.250/router")
        assertEquals(SecurityRiskLevel.SUSPICIOUS, ipBased.riskLevel)

        val shortener = PayloadParser.analyzeUrlSecurity("https://bit.ly/3xyz99")
        assertEquals(SecurityRiskLevel.CAUTION, shortener.riskLevel)
    }

    @Test
    fun `EAN, UPC, and ISBN product barcode identification and validation`() {
        val isbn = PayloadParser.parse("9780134494166", BarcodeFormatType.EAN_13)
        assertEquals(ParsedPayloadType.ISBN, isbn.type)
        assertTrue(isbn.primaryTitle.contains("978-0-13449-416-6"))

        val ean = PayloadParser.parse("3017620422003", BarcodeFormatType.EAN_13)
        assertEquals(ParsedPayloadType.PRODUCT, ean.type)
        assertEquals("France & Monaco (GS1 France)", ean.productCountryOrigin)
        assertTrue(PayloadParser.isValidGtin("3017620422003"))
        assertFalse(PayloadParser.isValidGtin("3017620422000"))

        val eanCheck = BarcodeValidator.validateProductBarcode("3017620422003", BarcodeFormatType.EAN_13)
        assertTrue(eanCheck.isValid)
        assertEquals("3017620422003", eanCheck.normalizedBarcode)

        val upcCheck = BarcodeValidator.validateProductBarcode("036000291452", BarcodeFormatType.UPC_A)
        assertTrue(upcCheck.isValid)

        val ean8Check = BarcodeValidator.validateProductBarcode("96385074", BarcodeFormatType.EAN_8)
        assertTrue(ean8Check.isValid)

        val invalidLength = BarcodeValidator.validateProductBarcode("12345", BarcodeFormatType.EAN_13)
        assertFalse(invalidLength.isValid)

        val invalidChecksum = BarcodeValidator.validateProductBarcode("3017620422009", BarcodeFormatType.EAN_13)
        assertFalse(invalidChecksum.isValid)
    }

    @Test
    fun `validateAndNormalize enforces symbology constraints across all 13 formats`() {
        val ean13 = BarcodeGeneratorEngine.validateAndNormalize("590123412345", BarcodeFormatType.EAN_13)
        assertTrue(ean13.isSuccess)
        assertEquals("5901234123457", ean13.getOrNull())

        val badEan8 = BarcodeGeneratorEngine.validateAndNormalize("12345", BarcodeFormatType.EAN_8)
        assertTrue(badEan8.isFailure)

        val itf = BarcodeGeneratorEngine.validateAndNormalize("1234567", BarcodeFormatType.ITF)
        assertTrue(itf.isSuccess)
        assertEquals("01234567", itf.getOrNull())

        val codabar = BarcodeGeneratorEngine.validateAndNormalize("40156789", BarcodeFormatType.CODABAR)
        assertTrue(codabar.isSuccess)
        assertEquals("A40156789B", codabar.getOrNull())
    }

    @Test
    fun `exportToCsv and exportToJson produce valid structured output without package leakage`() {
        val sampleRecords = listOf(
            ScanRecordEntity(
                id = 1L,
                rawValue = "https://github.com/CodeByArnob/QRYPT",
                primaryTitle = "github.com",
                subtitle = "HTTPS | Verified Safe Syntax",
                formatName = BarcodeFormatType.QR_CODE.name,
                payloadTypeName = ParsedPayloadType.URL.name,
                securityRiskName = SecurityRiskLevel.SAFE.name,
                isFavorite = true,
                isGenerated = false,
                customNotes = "Verified, QRYPT repo"
            )
        )
        val csv = ScanRepository.exportToCsv(sampleRecords)
        assertTrue(csv.contains("id,timestamp_utc,format,payload_type"))
        assertTrue(csv.contains("\"Verified, QRYPT repo\""))

        val json = ScanRepository.exportToJson(sampleRecords)
        assertNotNull(json)
        assertTrue(json.contains("\"application\": \"QRYPT\""))
        assertTrue(json.contains("\"developer\": \"Arnobrazz\""))
        assertTrue(json.contains("\"version\": \"1.1.0\""))
        assertFalse(json.contains("com.arnobrazz.qrypt"))
        assertTrue(json.contains("\"raw_value\": \"https://github.com/CodeByArnob/QRYPT\""))
    }
}
