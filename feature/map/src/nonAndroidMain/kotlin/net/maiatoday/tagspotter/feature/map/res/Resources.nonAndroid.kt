package net.maiatoday.tagspotter.feature.map.res

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return thumbnailPath.ifEmpty { imagePath }
}
