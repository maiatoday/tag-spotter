package net.maiatoday.tagspotter.feature.map.res

import androidx.core.net.toUri
import java.io.File

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    val rawThumb = thumbnailPath.trim()
    val rawImg = imagePath.trim()

    val thumb = if (rawThumb.startsWith("file://")) rawThumb.removePrefix("file://") else rawThumb
    val img = if (rawImg.startsWith("file://")) rawImg.removePrefix("file://") else rawImg

    // 1. Check local file for thumbnail
    if (thumb.isNotEmpty() && !thumb.startsWith("http://") && !thumb.startsWith("https://") && !thumb.startsWith("data:") && !thumb.startsWith("android.resource://") && !thumb.startsWith("content://")) {
        val thumbFile = File(thumb)
        if (thumbFile.exists() && thumbFile.isFile) {
            return thumbFile
        }
    }

    // 2. Check local file for main image
    if (img.isNotEmpty() && !img.startsWith("http://") && !img.startsWith("https://") && !img.startsWith("data:") && !img.startsWith("android.resource://") && !img.startsWith("content://")) {
        val imgFile = File(img)
        if (imgFile.exists() && imgFile.isFile) {
            return imgFile
        }
    }

    // 3. Check remote URLs or data URIs
    if (rawThumb.startsWith("http://") || rawThumb.startsWith("https://") || rawThumb.startsWith("data:")) {
        return rawThumb.toUri()
    }
    if (rawImg.startsWith("http://") || rawImg.startsWith("https://") || rawImg.startsWith("data:")) {
        return rawImg.toUri()
    }

    // 4. Check local content:// or android.resource:// URIs
    if (rawThumb.startsWith("content://") || rawThumb.startsWith("android.resource://")) {
        return rawThumb.toUri()
    }
    if (rawImg.startsWith("content://") || rawImg.startsWith("android.resource://")) {
        return rawImg.toUri()
    }

    val path = rawThumb.ifEmpty { rawImg }
    return if (path.startsWith("content://") || path.startsWith("android.resource://")) {
        path.toUri()
    } else {
        File(if (path.startsWith("file://")) path.removePrefix("file://") else path)
    }
}
