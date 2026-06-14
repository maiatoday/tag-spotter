package net.maiatoday.tagspotter.feature.map.res

import androidx.core.net.toUri
import java.io.File

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return if (thumbnailPath.isNotEmpty() && !thumbnailPath.startsWith("android.resource://") && !thumbnailPath.startsWith("http")) {
        File(thumbnailPath)
    } else if (thumbnailPath.isNotEmpty() && (thumbnailPath.startsWith("android.resource://") || thumbnailPath.startsWith("http"))) {
        thumbnailPath.toUri()
    } else if (imagePath.startsWith("content://") || imagePath.startsWith("android.resource://") || imagePath.startsWith("http")) {
        imagePath.toUri()
    } else {
        File(imagePath)
    }
}
