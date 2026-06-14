@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package net.maiatoday.tagspotter.core.photo

import org.koin.dsl.module
import org.koin.core.module.Module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val photoModule: Module = module {
    single<PhotoProcessor> { RealPhotoProcessor() }
}

actual fun resolveLocalPath(path: String): String {
    if (path.isEmpty() || path.startsWith("http")) return path
    
    val cleanPath = if (path.startsWith("file://")) {
        path.removePrefix("file://")
    } else if (path.startsWith("file:")) {
        path.removePrefix("file:")
    } else {
        path
    }
    
    val documentsPath = NSFileManager.defaultManager.URLForDirectory(
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
    
    return "file://$resolvedPath"
}

