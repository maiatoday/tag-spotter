package net.maiatoday.tagspotter.core.settings

import net.maiatoday.tagspotter.core.model.FilterCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilterManagerTest {

    @Test
    fun testInitialState() {
        val manager = FilterManager()
        assertEquals("All", manager.selectedCategory.value)
        assertEquals("All", manager.selectedSource.value)
        assertEquals("", manager.searchQuery.value)
        assertFalse(manager.showStarredOnly.value)
        assertNull(manager.activeFilterCenter.value)
        assertEquals(5000.0, manager.activeRadiusMeters.value)
    }

    @Test
    fun testSelectCategory() {
        val manager = FilterManager()
        manager.selectCategory("Graffiti")
        assertEquals("Graffiti", manager.selectedCategory.value)
    }

    @Test
    fun testSelectSource() {
        val manager = FilterManager()
        manager.selectSource("Device")
        assertEquals("Device", manager.selectedSource.value)
    }

    @Test
    fun testSetSearchQuery() {
        val manager = FilterManager()
        manager.setSearchQuery("tags")
        assertEquals("tags", manager.searchQuery.value)
    }

    @Test
    fun testSetShowStarredOnly() {
        val manager = FilterManager()
        manager.setShowStarredOnly(true)
        assertTrue(manager.showStarredOnly.value)

        manager.setShowStarredOnly(false)
        assertFalse(manager.showStarredOnly.value)
    }

    @Test
    fun testToggleShowStarredOnly() {
        val manager = FilterManager()
        assertFalse(manager.showStarredOnly.value)

        manager.toggleShowStarredOnly()
        assertTrue(manager.showStarredOnly.value)

        manager.toggleShowStarredOnly()
        assertFalse(manager.showStarredOnly.value)
    }

    @Test
    fun testSetLocationFilter() {
        val manager = FilterManager()
        val gps = FilterCenter.GPS(45.4642, 9.1900)
        manager.setLocationFilter(gps, 10000.0)

        assertEquals(gps, manager.activeFilterCenter.value)
        assertEquals(10000.0, manager.activeRadiusMeters.value)
    }

    @Test
    fun testClearLocationFilter() {
        val manager = FilterManager()
        val gps = FilterCenter.GPS(45.4642, 9.1900)
        manager.setLocationFilter(gps, 10000.0)
        assertEquals(gps, manager.activeFilterCenter.value)

        manager.clearLocationFilter()
        assertNull(manager.activeFilterCenter.value)
    }

    @Test
    fun testClearAll() {
        val manager = FilterManager()
        manager.selectCategory("Stencil")
        manager.selectSource("Remote")
        manager.setSearchQuery("hello")
        manager.setShowStarredOnly(true)
        manager.setLocationFilter(FilterCenter.GPS(1.0, 2.0), 3000.0)

        manager.clearAll()

        assertEquals("All", manager.selectedCategory.value)
        assertEquals("All", manager.selectedSource.value)
        assertEquals("", manager.searchQuery.value)
        assertFalse(manager.showStarredOnly.value)
        assertNull(manager.activeFilterCenter.value)
    }
}
