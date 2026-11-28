@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package net.maiatoday.tagspotter.core.sync

import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun readBytesFromFile(filePath: String): ByteArray? {
    if (filePath.isBlank() || filePath.startsWith("http://") || filePath.startsWith("https://")) {
        return null
    }
    return try {
        val cleanPath = when {
            filePath.startsWith("file://") -> filePath.removePrefix("file://")
            filePath.startsWith("file:") -> filePath.removePrefix("file:")
            else -> filePath
        }
        val resolvedPath = rebaseIosDocumentsPath(cleanPath)
        val path = resolvedPath.toPath()
        if (FileSystem.SYSTEM.exists(path)) {
            FileSystem.SYSTEM.read(path) {
                readByteArray()
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun rebaseIosDocumentsPath(cleanPath: String): String {
    val index = cleanPath.indexOf("/Documents/")
    if (index == -1) return cleanPath
    val documentsPath = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path ?: return cleanPath
    if (documentsPath.isEmpty()) return cleanPath
    val relativePath = cleanPath.substring(index + "/Documents/".length)
    return "$documentsPath/$relativePath"
}
