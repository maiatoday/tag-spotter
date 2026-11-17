package net.maiatoday.tagspotter.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    val thumb = thumbnailPath.trim()
    val img = imagePath.trim()

    if (thumb.startsWith("data:") || thumb.startsWith("http://") || thumb.startsWith("https://")) {
        return thumb
    }
    if (img.startsWith("data:") || img.startsWith("http://") || img.startsWith("https://")) {
        return img
    }

    val path = thumb.ifEmpty { img }
    return if (path.startsWith("file://")) path.removePrefix("file://") else path
}

@Composable
actual fun rememberPhotoDeletedChecker(): (imagePath: String, callback: (Boolean) -> Unit) -> Unit {
    return remember {
        { _, callback ->
            callback(false)
        }
    }
}
