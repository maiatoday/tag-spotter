package net.maiatoday.tagspotter.core.photo

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExifMetadataParserTest {

    @Test
    fun testExtractMetadataWithEmptyBytesReturnsNull() {
        val result = ExifMetadataParser.extractMetadata(byteArrayOf())
        assertNull(result)
    }

    @Test
    fun testExtractMetadataReadsGpsCoordinatesFromJpegExif() {
        val bytes = buildJpegWithExifGps(
            latDeg = 45, latMin = 27, latSecMillis = 51120, latRef = 'N',
            lngDeg = 9, lngMin = 11, lngSecMillis = 24000, lngRef = 'E'
        )
        val result = ExifMetadataParser.extractMetadata(bytes)
        assertNotNull(result)
        val lat = result.latitude
        val lng = result.longitude
        assertNotNull(lat)
        assertNotNull(lng)
        assertTrue(abs(lat - 45.4642) < 0.001, "Expected lat ~45.4642, got $lat")
        assertTrue(abs(lng - 9.1900) < 0.001, "Expected lng ~9.1900, got $lng")
    }

    @Test
    fun testFakePhotoProcessorExceptionSimulations() {
        val fake = FakePhotoProcessor()
        fake.tempCameraFileException = RuntimeException("Camera disconnected")
        
        try {
            fake.createTempCameraFile()
            assertTrue(false, "Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e is RuntimeException)
            assertEquals("Camera disconnected", e.message)
        }
    }
}

internal fun buildJpegWithExifGps(
    latDeg: Int,
    latMin: Int,
    latSecMillis: Int,
    latRef: Char,
    lngDeg: Int,
    lngMin: Int,
    lngSecMillis: Int,
    lngRef: Char,
    dateTime: String = "2024:05:15 14:30:00"
): ByteArray {
    val tiff = mutableListOf<Byte>()
    fun writeU16(v: Int) {
        tiff.add(((v shr 8) and 0xFF).toByte())
        tiff.add((v and 0xFF).toByte())
    }
    fun writeU32(v: Int) {
        tiff.add(((v shr 24) and 0xFF).toByte())
        tiff.add(((v shr 16) and 0xFF).toByte())
        tiff.add(((v shr 8) and 0xFF).toByte())
        tiff.add((v and 0xFF).toByte())
    }
    fun writeIfdEntry(tag: Int, type: Int, count: Int, valueOrOffset: Int) {
        writeU16(tag)
        writeU16(type)
        writeU32(count)
        writeU32(valueOrOffset)
    }

    // TIFF header (8 bytes): Big-Endian "MM", magic 42, IFD0 offset = 8
    tiff.add(0x4D); tiff.add(0x4D)
    writeU16(42)
    writeU32(8)

    val dateTimeOffset = 38
    val gpsIfdOffset = 58
    val latRationalsOffset = 112
    val lngRationalsOffset = 136

    // IFD0 at offset 8 (2 entries: DateTime + GPSInfoIFDPointer)
    writeU16(2)
    writeIfdEntry(0x0132, 2, 20, dateTimeOffset)
    writeIfdEntry(0x8825, 4, 1, gpsIfdOffset)
    writeU32(0)

    // DateTime ASCII at offset 38 (20 bytes including NUL)
    val dtBytes = dateTime.encodeToByteArray()
    for (i in 0 until 19) tiff.add(if (i < dtBytes.size) dtBytes[i] else 0)
    tiff.add(0)

    // GPS IFD at offset 58 (4 entries)
    writeU16(4)
    writeIfdEntry(0x0001, 2, 2, (latRef.code shl 24))
    writeIfdEntry(0x0002, 5, 3, latRationalsOffset)
    writeIfdEntry(0x0003, 2, 2, (lngRef.code shl 24))
    writeIfdEntry(0x0004, 5, 3, lngRationalsOffset)
    writeU32(0)

    // Latitude rationals at offset 112 (24 bytes)
    writeU32(latDeg); writeU32(1)
    writeU32(latMin); writeU32(1)
    writeU32(latSecMillis); writeU32(1000)

    // Longitude rationals at offset 136 (24 bytes)
    writeU32(lngDeg); writeU32(1)
    writeU32(lngMin); writeU32(1)
    writeU32(lngSecMillis); writeU32(1000)

    val exifHeader = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00) // "Exif\0\0"
    val app1Length = 2 + exifHeader.size + tiff.size
    val out = mutableListOf<Byte>()
    // SOI
    out.add(0xFF.toByte()); out.add(0xD8.toByte())
    // APP1
    out.add(0xFF.toByte()); out.add(0xE1.toByte())
    out.add(((app1Length shr 8) and 0xFF).toByte())
    out.add((app1Length and 0xFF).toByte())
    out.addAll(exifHeader.toList())
    out.addAll(tiff)
    // EOI
    out.add(0xFF.toByte()); out.add(0xD9.toByte())
    return out.toByteArray()
}
