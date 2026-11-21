package net.maiatoday.tagspotter.feature.tagalong

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import net.maiatoday.tagspotter.feature.tagalong.ui.parseInlineMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownTextTest {

    @Test
    fun parsesPlainTextWithoutSpans() {
        val result = parseInlineMarkdown("Hello Berlin street art!")
        assertEquals("Hello Berlin street art!", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun parsesBoldSpan() {
        val result = parseInlineMarkdown("Found **Neon Mural** nearby.")
        assertEquals("Found Neon Mural nearby.", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles.first()
        assertEquals(FontWeight.Bold, span.item.fontWeight)
        assertEquals("Neon Mural", result.text.substring(span.start, span.end))
    }

    @Test
    fun parsesItalicSpan() {
        val result = parseInlineMarkdown("Style: *wildstyle* graffiti.")
        assertEquals("Style: wildstyle graffiti.", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles.first()
        assertEquals(FontStyle.Italic, span.item.fontStyle)
        assertEquals("wildstyle", result.text.substring(span.start, span.end))
    }

    @Test
    fun parsesInlineCodeAndIgnoresMarkersInsideCode() {
        val bg = Color.LightGray
        val fg = Color.Black
        val result = parseInlineMarkdown(
            text = "Use `**not_bold**` in `/spot/isSaved`.",
            codeBackground = bg,
            codeColor = fg
        )
        assertEquals("Use **not_bold** in /spot/isSaved.", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals(FontFamily.Monospace, result.spanStyles[0].item.fontFamily)
        assertEquals(bg, result.spanStyles[0].item.background)
        assertEquals(fg, result.spanStyles[0].item.color)
        assertEquals("**not_bold**", result.text.substring(result.spanStyles[0].start, result.spanStyles[0].end))
        assertEquals("/spot/isSaved", result.text.substring(result.spanStyles[1].start, result.spanStyles[1].end))
    }

    @Test
    fun parsesMixedAndNestedSpans() {
        val result = parseInlineMarkdown("Check **East *Side* Gallery** and `graffiti` tag.")
        assertEquals("Check East Side Gallery and graffiti tag.", result.text)

        val boldSpan = result.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        val italicSpan = result.spanStyles.firstOrNull { it.item.fontStyle == FontStyle.Italic }
        val codeSpan = result.spanStyles.firstOrNull { it.item.fontFamily == FontFamily.Monospace }

        assertTrue(boldSpan != null)
        assertEquals("East Side Gallery", result.text.substring(boldSpan.start, boldSpan.end))

        assertTrue(italicSpan != null)
        assertEquals("Side", result.text.substring(italicSpan.start, italicSpan.end))

        assertTrue(codeSpan != null)
        assertEquals("graffiti", result.text.substring(codeSpan.start, codeSpan.end))
    }

    @Test
    fun parsesBulletLinesWithInlineFormatting() {
        val input = "- **Stop 1**: *Kreuzberg*\n* `stencil` art"
        val result = parseInlineMarkdown(input)
        assertEquals("• Stop 1: Kreuzberg\n• stencil art", result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(result.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun preservesUnclosedMarkersAndMathAsterisksGracefully() {
        val unclosed = "Streaming **unfinished bold and *unfinished italic and `unclosed code"
        val result = parseInlineMarkdown(unclosed)
        assertEquals(unclosed, result.text)
        assertTrue(result.spanStyles.isEmpty())

        val math = "2 * 3 * 4 = 24"
        val mathResult = parseInlineMarkdown(math)
        assertEquals(math, mathResult.text)
        assertTrue(mathResult.spanStyles.isEmpty())
    }
}
