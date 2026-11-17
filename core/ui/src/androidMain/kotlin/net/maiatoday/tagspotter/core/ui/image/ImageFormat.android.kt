package net.maiatoday.tagspotter.core.ui.image

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
                val cleanPath = if (imagePath.startsWith("file://")) imagePath.removePrefix("file://") else imagePath
                val file = File(cleanPath)
                callback(!file.exists())
            }
        }
    }
}
