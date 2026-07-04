package net.maiatoday.tagspotter.feature.map.res

import net.maiatoday.tagspotter.core.photo.resolveLocalPath

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    val path = thumbnailPath.ifEmpty { imagePath }
    return resolveLocalPath(path)
}
