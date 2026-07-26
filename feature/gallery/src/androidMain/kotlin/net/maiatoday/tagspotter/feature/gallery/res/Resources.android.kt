package net.maiatoday.tagspotter.feature.gallery.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.File

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ToastLauncher {
            override fun showToast(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

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
