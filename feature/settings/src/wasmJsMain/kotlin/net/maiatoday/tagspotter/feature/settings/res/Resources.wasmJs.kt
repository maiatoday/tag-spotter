package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("WasmJs Toast: $message")
            }
        }
    }
}
