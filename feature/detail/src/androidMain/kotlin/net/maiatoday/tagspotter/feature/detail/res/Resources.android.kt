package net.maiatoday.tagspotter.feature.detail.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
