package net.maiatoday.tagspotter.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal expect fun resolveExistingLocalFilePath(rawPath: String): String?
internal expect fun toLocalFileModel(path: String): Any

actual fun formatImageModel(
    imagePath: String,
    thumbnailPath: String,
    remoteThumbnailUrl: String,
    preferFullImage: Boolean
): Any {
    val resolved = resolveImageSource(
        imagePath = imagePath,
        thumbnailPath = thumbnailPath,
        remoteThumbnailUrl = remoteThumbnailUrl,
        preferFullImage = preferFullImage,
        resolveExistingLocalFile = ::resolveExistingLocalFilePath
    )

    return when (resolved) {
        is ResolvedImageSource.LocalFile -> toLocalFileModel(resolved.resolvedPath)
        is ResolvedImageSource.RemoteOrDataUrl -> resolved.url
        is ResolvedImageSource.PlatformUri -> resolved.uri
        is ResolvedImageSource.Fallback -> {
            val raw = resolved.rawPath
            if (raw.isEmpty() || raw.isRemoteOrDataUrl() || raw.isPlatformUri()) {
                raw
            } else {
                toLocalFileModel(raw.stripFileScheme())
            }
        }
    }
}

@Composable
actual fun rememberPhotoDeletedChecker(): (imagePath: String, callback: (Boolean) -> Unit) -> Unit {
    return remember {
        { _, callback ->
            callback(false)
        }
    }
}
