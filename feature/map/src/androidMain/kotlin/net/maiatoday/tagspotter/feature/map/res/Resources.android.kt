package net.maiatoday.tagspotter.feature.map.res

import androidx.core.net.toUri
import java.io.File

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    val thumb = thumbnailPath.trim()
    val img = imagePath.trim()

    if (thumb.startsWith("http://") || thumb.startsWith("https://") || thumb.startsWith("data:")) {
        return thumb.toUri()
    }
    if (thumb.isNotEmpty() && !thumb.startsWith("android.resource://") && !thumb.startsWith("content://")) {
        val thumbFile = File(thumb)
        if (thumbFile.exists() && thumbFile.isFile) {
            return thumbFile
        }
    }

    if (img.startsWith("http://") || img.startsWith("https://") || img.startsWith("data:")) {
        return img.toUri()
    }
    if (img.isNotEmpty() && !img.startsWith("android.resource://") && !img.startsWith("content://")) {
        val imgFile = File(img)
        if (imgFile.exists() && imgFile.isFile) {
            return imgFile
        }
    }

    val path = thumb.ifEmpty { img }
    return if (path.startsWith("content://") || path.startsWith("android.resource://")) {
        path.toUri()
    } else {
        File(path)
    }
}
