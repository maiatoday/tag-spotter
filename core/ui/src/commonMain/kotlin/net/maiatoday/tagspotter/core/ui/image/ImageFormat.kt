package net.maiatoday.tagspotter.core.ui.image

import androidx.compose.runtime.Composable
import net.maiatoday.tagspotter.core.model.SpotImage

/**
 * Resolves the best Coil image model across platforms with consistent fallback to [remoteThumbnailUrl].
 *
 * - When [preferFullImage] is `false` (thumbnails/cards): existing local `thumbnailPath` ->
 *   existing local `imagePath` -> `remoteThumbnailUrl` -> remote/data URL in `thumbnailPath`/`imagePath` ->
 *   platform URI (`content://`, `android.resource://`).
 * - When [preferFullImage] is `true` (full-screen viewer): existing local or platform URI `imagePath` ->
 *   existing local or platform URI `thumbnailPath` -> `remoteThumbnailUrl` -> remote/data URL in
 *   `imagePath`/`thumbnailPath`.
 */
expect fun formatImageModel(
    imagePath: String,
    thumbnailPath: String,
    remoteThumbnailUrl: String = "",
    preferFullImage: Boolean = false
): Any

fun formatImageModel(
    image: SpotImage,
    preferFullImage: Boolean = false
): Any = formatImageModel(
    imagePath = image.imagePath,
    thumbnailPath = image.thumbnailPath,
    remoteThumbnailUrl = image.remoteThumbnailUrl,
    preferFullImage = preferFullImage
)

@Composable
expect fun rememberPhotoDeletedChecker(): (imagePath: String, callback: (Boolean) -> Unit) -> Unit

internal sealed interface ResolvedImageSource {
    data class LocalFile(val resolvedPath: String) : ResolvedImageSource
    data class RemoteOrDataUrl(val url: String) : ResolvedImageSource
    data class PlatformUri(val uri: String) : ResolvedImageSource
    data class Fallback(val rawPath: String) : ResolvedImageSource
}

internal fun String.isRemoteOrDataUrl(): Boolean =
    startsWith("http://") || startsWith("https://") || startsWith("data:")

internal fun String.isPlatformUri(): Boolean =
    startsWith("content://") || startsWith("android.resource://")

internal fun String.stripFileScheme(): String = when {
    startsWith("file://") -> removePrefix("file://")
    startsWith("file:") -> removePrefix("file:")
    else -> this
}

internal fun resolveImageSource(
    imagePath: String,
    thumbnailPath: String,
    remoteThumbnailUrl: String = "",
    preferFullImage: Boolean = false,
    resolveExistingLocalFile: (String) -> String?
): ResolvedImageSource {
    val rawImg = imagePath.trim()
    val rawThumb = thumbnailPath.trim()
    val rawRemote = remoteThumbnailUrl.trim()

    fun tryLocalFile(raw: String): ResolvedImageSource.LocalFile? {
        if (raw.isEmpty() || raw.isRemoteOrDataUrl() || raw.isPlatformUri()) return null
        val resolved = resolveExistingLocalFile(raw) ?: return null
        return ResolvedImageSource.LocalFile(resolved)
    }

    if (preferFullImage) {
        tryLocalFile(rawImg)?.let { return it }
        if (rawImg.isPlatformUri()) return ResolvedImageSource.PlatformUri(rawImg)

        tryLocalFile(rawThumb)?.let { return it }
        if (rawThumb.isPlatformUri()) return ResolvedImageSource.PlatformUri(rawThumb)

        if (rawRemote.isRemoteOrDataUrl()) return ResolvedImageSource.RemoteOrDataUrl(rawRemote)
        if (rawImg.isRemoteOrDataUrl()) return ResolvedImageSource.RemoteOrDataUrl(rawImg)
        if (rawThumb.isRemoteOrDataUrl()) return ResolvedImageSource.RemoteOrDataUrl(rawThumb)

        val fallback = rawImg.ifEmpty { rawThumb.ifEmpty { rawRemote } }
        return ResolvedImageSource.Fallback(fallback)
    }

    // Thumbnail / default mode: prefer fast local files, then remote URLs, then platform URIs.
    tryLocalFile(rawThumb)?.let { return it }
    tryLocalFile(rawImg)?.let { return it }

    if (rawRemote.isRemoteOrDataUrl()) return ResolvedImageSource.RemoteOrDataUrl(rawRemote)
    if (rawThumb.isRemoteOrDataUrl()) return ResolvedImageSource.RemoteOrDataUrl(rawThumb)
    if (rawImg.isRemoteOrDataUrl()) return ResolvedImageSource.RemoteOrDataUrl(rawImg)

    if (rawThumb.isPlatformUri()) return ResolvedImageSource.PlatformUri(rawThumb)
    if (rawImg.isPlatformUri()) return ResolvedImageSource.PlatformUri(rawImg)

    val fallback = rawThumb.ifEmpty { rawImg.ifEmpty { rawRemote } }
    return ResolvedImageSource.Fallback(fallback)
}
