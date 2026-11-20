package net.maiatoday.tagspotter.core.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun PlatformThemeSideEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
actual fun StatusBarAppearanceEffect(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(darkIcons) {
            val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
            val controller = WindowCompat.getInsetsController(window, view)
            val previousState = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = darkIcons
            onDispose {
                controller.isAppearanceLightStatusBars = previousState
            }
        }
    }
}
