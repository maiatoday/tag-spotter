package net.maiatoday.tagspotter.core.ui.image

import java.io.File
import java.net.URI

internal actual fun resolveExistingLocalFilePath(rawPath: String): String? {
    val cleanPath = if (rawPath.startsWith("file:")) {
        runCatching { File(URI(rawPath)).path }.getOrNull() ?: rawPath.stripFileScheme()
    } else {
        rawPath.stripFileScheme()
    }
    if (cleanPath.isBlank()) return null
    val file = File(cleanPath)
    return if (file.exists() && file.isFile) file.absolutePath else null
}

internal actual fun toLocalFileModel(path: String): Any {
    if (path.isBlank()) return ""
    return File(path).toURI().toString()
}
