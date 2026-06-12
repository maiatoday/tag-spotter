package net.maiatoday.tagspotter.core.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationUtilsTest {

    private fun assertDoubleEquals(expected: Double, actual: Double, absoluteTolerance: Double) {
        assertTrue(
            abs(expected - actual) <= absoluteTolerance,
            "Expected $expected but was $actual (tolerance $absoluteTolerance)"
        )
    }

    @Test
    fun testCalculateDistance_sameCoordinates() {
        val distance = LocationUtils.calculateDistance(45.4642, 9.1899, 45.4642, 9.1899)
        assertDoubleEquals(0.0, distance, 0.001)
    }

    @Test
    fun testCalculateDistance_knownDistance() {
        // Distance between Milan Duomo (45.4642, 9.1899) and Milan Central Station (45.4858, 9.2037)
        // is approximately 2660 meters.
        val distance = LocationUtils.calculateDistance(45.4642, 9.1899, 45.4858, 9.2037)
        assertDoubleEquals(
            2660.0,
            distance,
            100.0
        ) // 100 meters tolerance for ellipsoid vs spherical earth model
    }

    @Test
    fun testGetLogarithmicRadiusMeters() {
        assertEquals(500.0, LocationUtils.getLogarithmicRadiusMeters(0.05f))
        assertEquals(1000.0, LocationUtils.getLogarithmicRadiusMeters(0.20f))
        assertEquals(5000.0, LocationUtils.getLogarithmicRadiusMeters(0.55f))
        assertEquals(50000.0, LocationUtils.getLogarithmicRadiusMeters(0.95f))
    }

    @Test
    fun testGetSliderValueForRadius() {
        assertEquals(0.0f, LocationUtils.getSliderValueForRadius(500.0))
        assertEquals(0.2f, LocationUtils.getSliderValueForRadius(1000.0))
        assertEquals(0.38f, LocationUtils.getSliderValueForRadius(2000.0))
        assertEquals(0.53f, LocationUtils.getSliderValueForRadius(5000.0))
        assertEquals(0.68f, LocationUtils.getSliderValueForRadius(10000.0))
        assertEquals(0.83f, LocationUtils.getSliderValueForRadius(20000.0))
        assertEquals(1.0f, LocationUtils.getSliderValueForRadius(50000.0))
    }

    @Test
    fun testGetRadiusLabel() {
        assertEquals("500m", LocationUtils.getRadiusLabel(500.0))
        assertEquals("1km", LocationUtils.getRadiusLabel(1000.0))
        assertEquals("5km", LocationUtils.getRadiusLabel(5000.0))
        assertEquals("50km", LocationUtils.getRadiusLabel(50000.0))
    }
}
