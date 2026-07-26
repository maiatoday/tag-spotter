package net.maiatoday.tagspotter.feature.map.res

import net.maiatoday.tagspotter.core.photo.resolveLocalPath

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
    return resolveLocalPath(path)
}
