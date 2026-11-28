package net.maiatoday.tagspotter.core.ui.image

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.File

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
        resolveExistingLocalFile = { raw ->
            val file = File(raw.stripFileScheme())
            if (file.exists() && file.isFile) file.absolutePath else null
        }
    )

    return when (resolved) {
        is ResolvedImageSource.LocalFile -> File(resolved.resolvedPath)
        is ResolvedImageSource.RemoteOrDataUrl -> resolved.url.toUri()
        is ResolvedImageSource.PlatformUri -> resolved.uri.toUri()
        is ResolvedImageSource.Fallback -> {
            val raw = resolved.rawPath
            if (raw.isPlatformUri() || raw.isRemoteOrDataUrl()) {
                raw.toUri()
            } else {
                File(raw.stripFileScheme())
            }
        }
    }
}

@Composable
actual fun rememberPhotoDeletedChecker(): (imagePath: String, callback: (Boolean) -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { imagePath: String, callback: (Boolean) -> Unit ->
            if (imagePath.startsWith("android.resource://") || imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("data:")) {
                callback(false)
            } else if (imagePath.startsWith("content://")) {
                val uri = Uri.parse(imagePath)
                try {
                    context.contentResolver.openInputStream(uri)?.use { }
                    callback(false)
                } catch (_: Exception) {
                    callback(true)
                }
            } else {
                val cleanPath = imagePath.stripFileScheme()
                val file = File(cleanPath)
                callback(!file.exists())
            }
        }
    }
}
