package net.maiatoday.tagspotter.feature.settings.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
