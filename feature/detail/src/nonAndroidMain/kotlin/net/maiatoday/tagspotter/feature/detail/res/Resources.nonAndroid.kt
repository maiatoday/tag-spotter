package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("Toast: $message")
            }
            override fun showToast(id: StringResource) {
                CoroutineScope(Dispatchers.Main).launch {
                    val message = org.jetbrains.compose.resources.getString(id)
                    println("Toast: $message")
                }
            }
        }
    }
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return thumbnailPath.ifEmpty { imagePath }
}
