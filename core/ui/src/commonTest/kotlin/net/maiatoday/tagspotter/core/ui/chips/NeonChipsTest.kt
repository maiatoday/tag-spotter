package net.maiatoday.tagspotter.core.ui.chips

import net.maiatoday.tagspotter.core.ui.theme.DarkCategoryColors
import kotlin.test.Test
import kotlin.test.assertEquals

class NeonChipsTest {

    @Test
    fun testCategoryBadgeTextFormatting() {
        val category1 = "graffiti"
        val category2 = "public_place"

        assertEquals("GRA", category1.take(3).uppercase())
        assertEquals("GRAFFITI", category1.replace("_", " ").uppercase())

        assertEquals("PUB", category2.take(3).uppercase())
        assertEquals("PUBLIC PLACE", category2.replace("_", " ").uppercase())
    }

    @Test
    fun testCategoryColorResolution() {
        val pink = DarkCategoryColors.getColorForCategory("graffiti")
        assertEquals(DarkCategoryColors.graffiti, pink)

        val cyan = DarkCategoryColors.getColorForCategory("sculpture")
        assertEquals(DarkCategoryColors.sculpture, cyan)

        val unknown = DarkCategoryColors.getColorForCategory("unknown_category")
        assertEquals(DarkCategoryColors.default, unknown)
    }

    @Test
    fun testTagFormatting() {
        val rawTag = "stencil"
        val formatted = if (!rawTag.startsWith("#")) "#$rawTag" else rawTag
        assertEquals("#stencil", formatted)

        val alreadyHashed = "#mural"
        val formattedHashed = if (!alreadyHashed.startsWith("#")) "#$alreadyHashed" else alreadyHashed
        assertEquals("#mural", formattedHashed)
    }
}
