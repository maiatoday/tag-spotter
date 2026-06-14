package net.maiatoday.tagspotter.core.photo

import org.koin.dsl.module
import org.koin.core.module.Module

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
    return "file://$cleanPath"
}

