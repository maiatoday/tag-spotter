@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package net.maiatoday.tagspotter.core.ui.image

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal actual fun resolveExistingLocalFilePath(rawPath: String): String? {
    val cleanPath = rawPath.stripFileScheme()
    if (cleanPath.isBlank()) return null

    val fileManager = NSFileManager.defaultManager
    val documentsPath = fileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path ?: ""

    val index = cleanPath.indexOf("/Documents/")
    val resolvedPath = if (index != -1 && documentsPath.isNotEmpty()) {
        val relativePath = cleanPath.substring(index + "/Documents/".length)
        "$documentsPath/$relativePath"
    } else {
        cleanPath
    }

    return if (fileManager.fileExistsAtPath(resolvedPath)) resolvedPath else null
}

internal actual fun toLocalFileModel(path: String): Any {
    if (path.isBlank()) return ""
    return NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
}
