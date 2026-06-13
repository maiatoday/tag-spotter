package net.maiatoday.tagspotter.core.ui.theme

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformThemeSideEffect(darkTheme: Boolean) {
    // No-op for non-Android platforms
}
