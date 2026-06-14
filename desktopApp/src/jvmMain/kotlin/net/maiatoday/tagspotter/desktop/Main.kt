package net.maiatoday.tagspotter.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.feature.main.MainNavigation
import net.maiatoday.tagspotter.feature.main.initKoin
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import org.koin.dsl.module
import java.io.File
import java.util.Properties

val desktopSecretsModule = module {
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

fun main() = application {
    // Initialize DI
    initKoin(listOf(desktopSecretsModule))

    Window(
        onCloseRequest = ::exitApplication,
        title = "Tag Spotter"
    ) {
        MyApplicationTheme {
            MainNavigation(
                initialSpotId = null,
                onNavigateToSpotHandled = {},
                onTriggerCamera = {},
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
                        val selectedFile = java.io.File(directory, file)
                        val uriString = selectedFile.toURI().toString()
                        onPhotoPicked(uriString)
                    }
                },
                versionName = "1.0.0-desktop",
                showToast = { println("Toast: $it") }
            )
        }
    }
}
