package net.maiatoday.tagspotter.feature.detail.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.File
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ToastLauncher {
            override fun showToast(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            override fun showToast(id: StringResource) {
                CoroutineScope(Dispatchers.Main).launch {
                    val message = org.jetbrains.compose.resources.getString(id)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

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
