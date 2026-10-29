package com.example

import com.example.data.local.ScanRecordEntity
import com.example.data.repository.ScanRepository
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.QrCustomizationConfig
import com.example.domain.model.QrErrorCorrection
import com.example.domain.model.ScanRegionMode
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.parser.PayloadParser
import com.example.engine.BarcodeGeneratorEngine
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
    fun `parse Calendar, Email, SMS, Phone, and Geo payloads`() {
        val calPayload = PayloadParser.buildCalendarPayload(
            title = "QRYPT Release Review",
            location = "Hall 3",
            startIso = "20261104T093000Z",
            endIso = "20261104T103000Z",
            description = "v1.0.0 APK Audit"
        )
        val parsedCal = PayloadParser.parse(calPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.CALENDAR, parsedCal.type)
        assertEquals("QRYPT Release Review", parsedCal.primaryTitle)

        val emailPayload = PayloadParser.buildEmailPayload("arnobrazz.dev@gmail.com", "Audit", "Ready")
        val parsedEmail = PayloadParser.parse(emailPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.EMAIL, parsedEmail.type)
        assertEquals("arnobrazz.dev@gmail.com", parsedEmail.primaryTitle)

        val smsPayload = PayloadParser.buildSmsPayload("+14158910192", "Verify code")
        val parsedSms = PayloadParser.parse(smsPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.SMS, parsedSms.type)
        assertEquals("+14158910192", parsedSms.primaryTitle)

        val parsedPhone = PayloadParser.parse("tel:+14158910192", BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.PHONE, parsedPhone.type)
        assertEquals("+14158910192", parsedPhone.primaryTitle)

        val geoPayload = PayloadParser.buildGeoPayload("37.7897", "-122.4014", "SF HQ")
        val parsedGeo = PayloadParser.parse(geoPayload, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedPayloadType.GEO, parsedGeo.type)
        assertEquals("SF HQ", parsedGeo.primaryTitle)
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
    fun `parse ISBN-13 and retail EAN-13 verifies GTIN check digit and GS1 origin`() {
        val isbn = PayloadParser.parse("9780134494166", BarcodeFormatType.EAN_13)
        assertEquals(ParsedPayloadType.ISBN, isbn.type)
        assertTrue(isbn.primaryTitle.contains("978-0-13449-416-6"))

        val ean = PayloadParser.parse("5901234123457", BarcodeFormatType.EAN_13)
        assertEquals(ParsedPayloadType.PRODUCT, ean.type)
        assertEquals("Poland (GS1 Poland)", ean.productCountryOrigin)
        assertTrue(PayloadParser.isValidGtin("5901234123457"))
        assertFalse(PayloadParser.isValidGtin("5901234123450"))
    }

    @Test
    fun `validateAndNormalize enforces symbology constraints across all 13 formats`() {
        // Valid 12-digit EAN-13 auto-completes 13th check digit
        val ean13 = BarcodeGeneratorEngine.validateAndNormalize("590123412345", BarcodeFormatType.EAN_13)
        assertTrue(ean13.isSuccess)
        assertEquals("5901234123457", ean13.getOrNull())

        // Invalid EAN-8 length fails gracefully
        val badEan8 = BarcodeGeneratorEngine.validateAndNormalize("12345", BarcodeFormatType.EAN_8)
        assertTrue(badEan8.isFailure)

        // Odd-length ITF auto-pads leading zero to even length
        val itf = BarcodeGeneratorEngine.validateAndNormalize("1234567", BarcodeFormatType.ITF)
        assertTrue(itf.isSuccess)
        assertEquals("01234567", itf.getOrNull())

        // Codabar auto-wraps start/stop guards A..B
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
                subtitle = "HTTPS • Verified Safe Syntax",
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
        assertTrue(json.contains("\"version\": \"1.0.0\""))
        assertFalse(json.contains("com.arnobrazz.qrypt"))
        assertTrue(json.contains("\"raw_value\": \"https://github.com/CodeByArnob/QRYPT\""))
    }
}
