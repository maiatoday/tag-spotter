package net.maiatoday.tagspotter.web

import androidx.compose.ui.window.CanvasBasedWindow
import net.maiatoday.tagspotter.core.settings.SecretsProvider
import net.maiatoday.tagspotter.feature.main.MainNavigation
import net.maiatoday.tagspotter.feature.main.initKoin
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import org.koin.dsl.module

val webSecretsModule = module {
    single<SecretsProvider> { WebSecretsProvider() }
}

class WebSecretsProvider : SecretsProvider {
    override fun getGeminiApiKey(): String {
        // TODO: read from environment or query param if needed
        return ""
    }
}

fun main() {
    initKoin(listOf(webSecretsModule))
    CanvasBasedWindow(
        title = "Tag Spotter",
        canvasElementId = "compose-canvas"
    ) {
        MyApplicationTheme {
            MainNavigation(
                initialSpotId = null,
                onNavigateToSpotHandled = {},
                onTriggerCamera = {},
                onTriggerFiles = { onPhotoPicked ->
                    // Web fallback: log the request; actual implementation would use a file input dialog
                    println("Photo picker requested: $onPhotoPicked")
                },
                versionName = "1.0.0-web",
                showToast = { println("Toast: $it") }
            )
        }
    }
}
