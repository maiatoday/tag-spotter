package net.maiatoday.tagspotter.core.photo

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidPhotoProcessorMetadataTest {

    @Test
    fun extractMetadataFromBytes_readsGpsFromJpegExif() {
        val bytesWithExif = buildJpegWithExifGps(
            latDeg = 45, latMin = 27, latSecMillis = 51120, latRef = 'N',
            lngDeg = 9, lngMin = 11, lngSecMillis = 24000, lngRef = 'E'
        )
        val metadata = AndroidPhotoProcessor.extractMetadataFromBytes(bytesWithExif)

        assertNotNull(metadata)
        val lat = metadata.latitude
        val lng = metadata.longitude
        assertNotNull(lat)
        assertNotNull(lng)
        assertTrue(abs(lat - 45.4642) < 0.001, "Expected lat ~45.4642, got $lat")
        assertTrue(abs(lng - 9.1900) < 0.001, "Expected lng ~9.1900, got $lng")
    }

    @Test
    fun extractMetadataFromBytes_ignoresZeroZeroGpsCoordinates() {
        val bytesWithZeroGps = buildJpegWithExifGps(
            latDeg = 0, latMin = 0, latSecMillis = 0, latRef = 'N',
            lngDeg = 0, lngMin = 0, lngSecMillis = 0, lngRef = 'E'
        )
        val metadata = AndroidPhotoProcessor.extractMetadataFromBytes(bytesWithZeroGps)

        assertNull(metadata?.latitude)
        assertNull(metadata?.longitude)
    }

    @Test
    fun extractMetadataFromBytes_returnsNullForEmptyBytes() {
        assertNull(AndroidPhotoProcessor.extractMetadataFromBytes(byteArrayOf()))
    }
}
