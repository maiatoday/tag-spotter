package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Parses lightweight inline Markdown (`**bold**`, `*italic*`, `` `code` ``, and leading
 * `- ` / `* ` bullet lines) into a Compose [AnnotatedString].
 *
 * Unclosed delimiters are preserved literally so partial streaming chunks never drop text
 * or throw exceptions.
 */
fun parseInlineMarkdown(
    text: String,
    codeBackground: Color = Color.Unspecified,
    codeColor: Color = Color.Unspecified
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")

    val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = codeBackground,
        color = codeColor
    )
    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)

    return buildAnnotatedString {
        val lines = text.split('\n')
        lines.forEachIndexed { lineIndex, rawLine ->
            if (lineIndex > 0) append('\n')

            val bulletContent = extractBulletLine(rawLine)
            if (bulletContent != null) {
                append(bulletContent.first)
                append("• ")
                appendInlineMarkdown(
                    builder = this,
                    text = bulletContent.second,
                    codeStyle = codeStyle,
                    boldStyle = boldStyle,
                    italicStyle = italicStyle
                )
            } else {
                appendInlineMarkdown(
                    builder = this,
                    text = rawLine,
                    codeStyle = codeStyle,
                    boldStyle = boldStyle,
                    italicStyle = italicStyle
                )
            }
        }
    }
}

private fun extractBulletLine(line: String): Pair<String, String>? {
    var idx = 0
    while (idx < line.length && (line[idx] == ' ' || line[idx] == '\t')) {
        idx++
    }
    if (idx + 1 < line.length && (line[idx] == '-' || line[idx] == '*') && line[idx + 1] == ' ') {
        val indent = line.substring(0, idx)
        val rest = line.substring(idx + 2)
        return indent to rest
    }
    return null
}

private fun appendInlineMarkdown(
    builder: AnnotatedString.Builder,
    text: String,
    codeStyle: SpanStyle,
    boldStyle: SpanStyle,
    italicStyle: SpanStyle
) {
    var i = 0
    while (i < text.length) {
        when {
            text[i] == '`' -> {
                val close = text.indexOf('`', startIndex = i + 1)
                if (close > i + 1) {
                    builder.pushStyle(codeStyle)
                    builder.append(text.substring(i + 1, close))
                    builder.pop()
                    i = close + 1
                } else {
                    builder.append('`')
                    i++
                }
            }

            text.startsWith("**", i) -> {
                val close = findClosingDoubleAsterisk(text, i + 2)
                if (close > i + 2) {
                    builder.pushStyle(boldStyle)
                    appendInlineMarkdown(
                        builder = builder,
                        text = text.substring(i + 2, close),
                        codeStyle = codeStyle,
                        boldStyle = boldStyle,
                        italicStyle = italicStyle
                    )
                    builder.pop()
                    i = close + 2
                } else {
                    builder.append("**")
                    i += 2
                }
            }

            text[i] == '*' -> {
                val close = findClosingSingleAsterisk(text, i + 1)
                if (close > i + 1) {
                    builder.pushStyle(italicStyle)
                    appendInlineMarkdown(
                        builder = builder,
                        text = text.substring(i + 1, close),
                        codeStyle = codeStyle,
                        boldStyle = boldStyle,
                        italicStyle = italicStyle
                    )
                    builder.pop()
                    i = close + 1
                } else {
                    builder.append('*')
                    i++
                }
            }

            else -> {
                builder.append(text[i])
                i++
            }
        }
    }
}

private fun findClosingDoubleAsterisk(text: String, startIndex: Int): Int {
    if (startIndex >= text.length || text[startIndex].isWhitespace()) return -1
    var j = startIndex
    while (j < text.length - 1) {
        if (text[j] == '`') {
            val codeEnd = text.indexOf('`', j + 1)
            if (codeEnd > j + 1) {
                j = codeEnd + 1
                continue
            }
        }
        if (text[j] == '*' && text[j + 1] == '*' && !text[j - 1].isWhitespace()) {
            return j
        }
        j++
    }
    return -1
}

private fun findClosingSingleAsterisk(text: String, startIndex: Int): Int {
    if (startIndex >= text.length || text[startIndex].isWhitespace()) return -1
    var j = startIndex
    while (j < text.length) {
        if (text[j] == '`') {
            val codeEnd = text.indexOf('`', j + 1)
            if (codeEnd > j + 1) {
                j = codeEnd + 1
                continue
            }
        }
        if (text[j] == '*') {
            val isDouble = j + 1 < text.length && text[j + 1] == '*'
            if (isDouble) {
                j += 2
                continue
            }
            if (!text[j - 1].isWhitespace()) {
                return j
            }
        }
        j++
    }
    return -1
}
