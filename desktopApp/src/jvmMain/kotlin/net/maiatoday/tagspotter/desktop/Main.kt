package net.maiatoday.tagspotter.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.adk.kt.runners.Runner
import net.maiatoday.tagspotter.core.agent.createDesktopTagAlongRunner
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import net.maiatoday.tagspotter.feature.main.MainNavigation
import net.maiatoday.tagspotter.feature.main.initKoin
import net.maiatoday.tagspotter.feature.tagalong.di.tagalongModule
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongScreen
import org.koin.dsl.module
import java.awt.Taskbar
import java.io.File
import java.util.Properties
import javax.imageio.ImageIO

val desktopSecretsModule =
    module {
        single<SecretsProvider> { DesktopSecretsProvider() }
    }

val desktopAgentModule =
    module {
        val secrets = DesktopSecretsProvider()
        val apiKey = secrets.getGeminiApiKey()
        if (apiKey.isNotBlank()) {
            single<Runner> {
                createDesktopTagAlongRunner(
                    apiKey = apiKey,
                    hybridSpotRepository = get()
                )
            }
        }
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
        initKoin(listOf(desktopSecretsModule, desktopAgentModule, tagalongModule))

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

        val icon = useResource("icon.png") { it.readAllBytes().decodeToImageBitmap() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Tag Spotter",
            icon = BitmapPainter(icon),
        ) {
            val settingsRepository = remember {
                org.koin.core.context.GlobalContext.get().get<net.maiatoday.tagspotter.core.settings.SettingsRepository>()
            }
            val themeMode by settingsRepository.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
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
                    tagAlongContent = { onOpenSpotDetail, onViewOnMainMap ->
                        TagAlongScreen(
                            onOpenSpotDetail = onOpenSpotDetail,
                            onViewOnMainMap = onViewOnMainMap,
                            onPickPhoto = { onPhotoPicked ->
                                val fileDialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Street Art Photo", java.awt.FileDialog.LOAD)
                                fileDialog.setFilenameFilter { _, name ->
                                    val lower = name.lowercase()
                                    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
                                }
                                fileDialog.isVisible = true
                                val directory = fileDialog.directory
                                val file = fileDialog.file
                                if (directory != null && file != null) {
                                    val selectedFile = File(directory, file)
                                    onPhotoPicked(selectedFile.toURI().toString())
                                }
                            }
                        )
                    }
                )
            }
        }
    }
