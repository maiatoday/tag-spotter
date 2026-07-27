package net.maiatoday.tagspotter.core.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.FilterCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilterManagerTest {

    @Test
    fun testCategoryFlowWithTurbine() = runTest {
        val manager = FilterManager()

        manager.selectedCategory.test {
            assertEquals("All", awaitItem())

            manager.selectCategory("Graffiti")
            assertEquals("Graffiti", awaitItem())

            manager.selectCategory("Sculpture")
            assertEquals("Sculpture", awaitItem())

            manager.clearAll()
            assertEquals("All", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testSourceFlowWithTurbine() = runTest {
        val manager = FilterManager()

        manager.selectedSource.test {
            assertEquals("All", awaitItem())

            manager.selectSource("My Spots")
            assertEquals("My Spots", awaitItem())

            manager.selectSource("Imported")
            assertEquals("Imported", awaitItem())

            manager.clearAll()
            assertEquals("All", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testSearchQueryFlowWithTurbine() = runTest {
        val manager = FilterManager()

        manager.searchQuery.test {
            assertEquals("", awaitItem())

            manager.setSearchQuery("tag1")
            assertEquals("tag1", awaitItem())

            manager.clearAll()
            assertEquals("", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testStarredOnlyFlowWithTurbine() = runTest {
        val manager = FilterManager()

        manager.showStarredOnly.test {
            assertFalse(awaitItem())

            manager.setShowStarredOnly(true)
            assertTrue(awaitItem())

            manager.toggleShowStarredOnly()
            assertFalse(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testLocationFilterFlowsWithTurbine() = runTest {
        val manager = FilterManager()
        val gps = FilterCenter.GPS(45.4642, 9.1900)

        manager.activeFilterCenter.test {
            assertNull(awaitItem())

            manager.setLocationFilter(gps, 10000.0)
            assertEquals(gps, awaitItem())

            manager.clearLocationFilter()
            assertNull(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }
}
