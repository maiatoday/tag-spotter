package net.maiatoday.tagspotter.feature.tagalong.ui

import android.util.Log

private const val LOGCAT_CHUNK_SIZE = 3500

actual fun logA2uiDebug(tag: String, message: String) {
    val chunks = message.chunked(LOGCAT_CHUNK_SIZE)
    val total = chunks.size
    chunks.forEachIndexed { index, chunk ->
        val prefixed = if (total > 1) "[${index + 1}/$total] $chunk" else chunk
        try {
            Log.d(tag, prefixed)
        } catch (_: Throwable) {
            println("[$tag] $prefixed")
        }
    }
}
