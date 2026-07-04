package net.maiatoday.tagspotter.core.sync

import okio.FileSystem
import okio.Path.Companion.toPath

actual fun readBytesFromFile(filePath: String): ByteArray? {
    return try {
        val cleanPath = if (filePath.startsWith("file://")) {
            filePath.removePrefix("file://")
        } else if (filePath.startsWith("file:")) {
            filePath.removePrefix("file:")
        } else {
            filePath
        }
        val path = cleanPath.toPath()
        if (FileSystem.SYSTEM.exists(path)) {
            FileSystem.SYSTEM.read(path) {
                readByteArray()
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
