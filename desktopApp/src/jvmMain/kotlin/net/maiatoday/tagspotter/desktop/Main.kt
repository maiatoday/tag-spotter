package net.maiatoday.tagspotter.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import net.maiatoday.tagspotter.feature.main.MainNavigation
import net.maiatoday.tagspotter.feature.main.initKoin
import org.koin.dsl.module
import java.awt.Taskbar
import java.io.File
import java.util.Properties
import javax.imageio.ImageIO

val desktopSecretsModule =
    module {
        single<SecretsProvider> { DesktopSecretsProvider() }
    }

class DesktopSecretsProvider : SecretsProvider {
    override fun getGeminiApiKey(): String {
        val envKey = System.getenv("GEMINI_API_KEY")
        if (!envKey.isNullOrEmpty()) return envKey

        val propertiesFile = File("local.properties")
        if (propertiesFile.exists()) {
            val props = Properties()
            propertiesFile.inputStream().use { props.load(it) }
            val key = props.getProperty("gemini.api.key")
            if (!key.isNullOrEmpty()) return key
        }
        return ""
    }
}

fun main() =
    application {
        // Initialize DI
        initKoin(listOf(desktopSecretsModule))

        // Set macOS Dock Icon / AWT Taskbar icon if supported
        try {
            val taskbar = Taskbar.getTaskbar()
            val resource = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
            if (resource != null) {
                val image = ImageIO.read(resource)
                taskbar.setIconImage(image)
            }
        } catch (_: Exception) {
            // Taskbar isn't supported or failed to set icon
        }

        val icon = useResource("icon.png") { loadImageBitmap(it) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Tag Spotter",
            icon = BitmapPainter(icon),
        ) {
            MyApplicationTheme {
                MainNavigation(
                    initialSpotId = null,
                    onNavigateToSpotHandled = {},
                    onTriggerFiles = { onPhotoPicked ->
                        val fileDialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Photo", java.awt.FileDialog.LOAD)
                        fileDialog.setFilenameFilter { _, name ->
                            val lower = name.lowercase()
                            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                        }
                        fileDialog.isVisible = true
                        val directory = fileDialog.directory
                        val file = fileDialog.file
                        if (directory != null && file != null) {
                            val selectedFile = File(directory, file)
                            val uriString = selectedFile.toURI().toString()
                            onPhotoPicked(uriString)
                        }
                    },
                    versionName = "1.0.0-desktop",
                    showToast = { println("Toast: $it") },
                )
            }
        }
    }
