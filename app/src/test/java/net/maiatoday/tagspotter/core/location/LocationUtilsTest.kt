package net.maiatoday.tagspotter.core.location

import net.maiatoday.tagspotter.core.model.LocationUtils
import org.junit.Assert
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun testCalculateDistance_sameCoordinates() {
        val distance = LocationUtils.calculateDistance(45.4642, 9.1899, 45.4642, 9.1899)
        Assert.assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testCalculateDistance_knownDistance() {
        // Distance between Milan Duomo (45.4642, 9.1899) and Milan Central Station (45.4858, 9.2037)
        // is approximately 2660 meters.
        val distance = LocationUtils.calculateDistance(45.4642, 9.1899, 45.4858, 9.2037)
        Assert.assertEquals(
            2660.0,
            distance,
            100.0
        ) // 100 meters tolerance for ellipsoid vs spherical earth model
    }

    @Test
    fun testGetLogarithmicRadiusMeters() {
        Assert.assertEquals(500.0, LocationUtils.getLogarithmicRadiusMeters(0.05f), 0.0)
        Assert.assertEquals(1000.0, LocationUtils.getLogarithmicRadiusMeters(0.20f), 0.0)
        Assert.assertEquals(5000.0, LocationUtils.getLogarithmicRadiusMeters(0.55f), 0.0)
        Assert.assertEquals(50000.0, LocationUtils.getLogarithmicRadiusMeters(0.95f), 0.0)
    }

    @Test
    fun testGetSliderValueForRadius() {
        Assert.assertEquals(0.0f, LocationUtils.getSliderValueForRadius(500.0))
        Assert.assertEquals(0.2f, LocationUtils.getSliderValueForRadius(1000.0))
        Assert.assertEquals(0.53f, LocationUtils.getSliderValueForRadius(5000.0))
        Assert.assertEquals(1.0f, LocationUtils.getSliderValueForRadius(50000.0))
    }

    @Test
    fun testGetRadiusLabel() {
        Assert.assertEquals("500m", LocationUtils.getRadiusLabel(500.0))
        Assert.assertEquals("1km", LocationUtils.getRadiusLabel(1000.0))
        Assert.assertEquals("5km", LocationUtils.getRadiusLabel(5000.0))
        Assert.assertEquals("50km", LocationUtils.getRadiusLabel(50000.0))
    }
}