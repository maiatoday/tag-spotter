package net.maiatoday.tagspotter.core.location

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocationProviderTest {
    @Test
    fun testFakeLocationProviderReturnsSetLocation() = runTest {
        val provider = FakeLocationProvider()
        assertNull(provider.getCurrentLocation())

        val expectedLocation = LocationData(1.0, 2.0, false)
        provider.locationToReturn = expectedLocation
        assertEquals(expectedLocation, provider.getCurrentLocation())
    }
}
