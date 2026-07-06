package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("JVM Toast: $message")
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        null,
                        message,
                        "Tag Spotter",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }
    }
}
