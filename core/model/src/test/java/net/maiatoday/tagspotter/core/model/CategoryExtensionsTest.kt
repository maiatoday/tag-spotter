package net.maiatoday.tagspotter.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CategoryExtensionsTest {

    @Test
    fun testGraffitiMappings() {
        val cat = "graffiti"
        assertEquals("ARTIST / CREW", cat.getCategoryCreatorLabel())
        assertEquals("e.g. Banksy", cat.getCategoryCreatorPlaceholder())
        assertEquals("Artist Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Artist", cat.getCategoryCreatorUnknownLabel())
        assertEquals("Active Spot", cat.getCategoryActiveStatusLabel())
        assertEquals("Painted Over", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Painted Over", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("PAINTED DATE", cat.getCategoryDateLabel())
        assertEquals("Painted Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. circa 2023, Dec 2024", cat.getCategoryDatePlaceholder())
    }

    @Test
    fun testSculptureMappings() {
        val cat = "sculpture"
        assertEquals("SCULPTOR", cat.getCategoryCreatorLabel())
        assertEquals("e.g. Michelangelo", cat.getCategoryCreatorPlaceholder())
        assertEquals("Sculptor Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Sculptor", cat.getCategoryCreatorUnknownLabel())
        assertEquals("On Display", cat.getCategoryActiveStatusLabel())
        assertEquals("Removed", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Removed", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("CREATION DATE", cat.getCategoryDateLabel())
        assertEquals("Creation Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. 1504, circa 2020", cat.getCategoryDatePlaceholder())
    }

    @Test
    fun testArchitectureMappings() {
        val cat = "architecture"
        assertEquals("ARCHITECT", cat.getCategoryCreatorLabel())
        assertEquals("e.g. Frank Gehry", cat.getCategoryCreatorPlaceholder())
        assertEquals("Architect Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Architect", cat.getCategoryCreatorUnknownLabel())
        assertEquals("Standing", cat.getCategoryActiveStatusLabel())
        assertEquals("Demolished", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Demolished", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("COMPLETED DATE", cat.getCategoryDateLabel())
        assertEquals("Completed Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. 1939, built 2005", cat.getCategoryDatePlaceholder())
    }

    @Test
    fun testNatureMappings() {
        val cat = "nature"
        assertEquals("GARDENER / PLANNER", cat.getCategoryCreatorLabel())
        assertEquals("e.g. Landscape Architect, City Parks", cat.getCategoryCreatorPlaceholder())
        assertEquals("Gardener/Planner Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Designer", cat.getCategoryCreatorUnknownLabel())
        assertEquals("Vibrant", cat.getCategoryActiveStatusLabel())
        assertEquals("Gone", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Gone", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("ESTABLISHED DATE", cat.getCategoryDateLabel())
        assertEquals("Established Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. planted 2020, Spring 2023", cat.getCategoryDatePlaceholder())
    }

    @Test
    fun testPublicPlaceMappings() {
        val cat = "public_place"
        assertEquals("DESIGNER / CREATOR", cat.getCategoryCreatorLabel())
        assertEquals("e.g. City Council", cat.getCategoryCreatorPlaceholder())
        assertEquals("Designer/Creator Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Creator", cat.getCategoryCreatorUnknownLabel())
        assertEquals("Active", cat.getCategoryActiveStatusLabel())
        assertEquals("Closed", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Closed", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("ESTABLISHED DATE", cat.getCategoryDateLabel())
        assertEquals("Established Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. opened 2010, circa 1990", cat.getCategoryDatePlaceholder())
    }

    @Test
    fun testFoodMappings() {
        val cat = "food"
        assertEquals("CHEF", cat.getCategoryCreatorLabel())
        assertEquals("e.g. Gordon Ramsay", cat.getCategoryCreatorPlaceholder())
        assertEquals("Chef Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Chef", cat.getCategoryCreatorUnknownLabel())
        assertEquals("Open", cat.getCategoryActiveStatusLabel())
        assertEquals("Closed", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Closed", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("VISITED DATE", cat.getCategoryDateLabel())
        assertEquals("Visited Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. June 2024, last week", cat.getCategoryDatePlaceholder())
    }

    @Test
    fun testFallbackMappings() {
        val cat = "unknown_random_category"
        assertEquals("ARTIST / CREW", cat.getCategoryCreatorLabel())
        assertEquals("e.g. Banksy", cat.getCategoryCreatorPlaceholder())
        assertEquals("Artist Name", cat.getCategoryCreatorTextFieldLabel())
        assertEquals("Unknown Artist", cat.getCategoryCreatorUnknownLabel())
        assertEquals("Active Spot", cat.getCategoryActiveStatusLabel())
        assertEquals("Inactive", cat.getCategoryInactiveStatusLabel())
        assertEquals("Mark as Inactive", cat.getCategoryStatusActionMarkInactiveText())
        assertEquals("CREATION DATE", cat.getCategoryDateLabel())
        assertEquals("Creation Date", cat.getCategoryDateTextFieldLabel())
        assertEquals("e.g. circa 2023, Dec 2024", cat.getCategoryDatePlaceholder())
    }
}
