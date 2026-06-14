package net.maiatoday.tagspotter.feature.main

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme

fun createMainViewController(
    onTriggerFiles: (onPhotoPicked: (String) -> Unit) -> Unit
): UIViewController = ComposeUIViewController {
    MyApplicationTheme {
        MainNavigation(
            initialSpotId = null,
            onNavigateToSpotHandled = {},
            onTriggerCamera = {},
            onTriggerFiles = onTriggerFiles,
            versionName = "1.0.0-ios",
            showToast = { println("Toast: $it") }
        )
    }
}
