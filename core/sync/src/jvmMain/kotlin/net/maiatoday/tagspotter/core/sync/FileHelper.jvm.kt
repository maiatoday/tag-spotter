package net.maiatoday.tagspotter.core.sync

import java.io.File

actual fun readBytesFromFile(filePath: String): ByteArray? {
    return try {
        val cleanPath = if (filePath.startsWith("file://")) {
            filePath.removePrefix("file://")
        } else if (filePath.startsWith("file:")) {
            filePath.removePrefix("file:")
        } else {
            filePath
        }
        val file = File(cleanPath)
        if (file.exists() && file.isFile) {
            file.readBytes()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
