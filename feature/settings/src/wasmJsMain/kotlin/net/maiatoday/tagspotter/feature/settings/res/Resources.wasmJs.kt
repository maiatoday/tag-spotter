package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@OptIn(ExperimentalWasmJsInterop::class)
private fun webAlert(message: String): Unit = js("alert(message)")

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("WasmJs Toast: $message")
                webAlert(message)
            }
        }
    }
}
