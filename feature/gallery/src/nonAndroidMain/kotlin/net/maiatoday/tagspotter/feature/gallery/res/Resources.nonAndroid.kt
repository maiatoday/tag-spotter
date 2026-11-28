package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("Toast: $message")
            }
        }
    }
}
