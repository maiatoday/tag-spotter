package net.maiatoday.tagspotter.feature.map.res

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    val path = thumbnailPath.ifEmpty { imagePath }
    return net.maiatoday.tagspotter.core.photo.resolveLocalPath(path)
}
