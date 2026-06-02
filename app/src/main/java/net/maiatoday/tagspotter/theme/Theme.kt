package net.maiatoday.tagspotter.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val UrbanDarkColorScheme = darkColorScheme(
    primary = UrbanPrimary,
    secondary = UrbanSecondary,
    tertiary = UrbanTertiary,
    background = UrbanBackground,
    surface = UrbanSurface,
    onPrimary = UrbanBackground,
    onSecondary = UrbanBackground,
    onTertiary = UrbanBackground,
    onBackground = UrbanOnBackground,
    onSurface = UrbanOnSurface,
    error = UrbanError,
    onError = UrbanOnError
)

private val UrbanLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    error = LightError,
    onError = LightOnError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) UrbanDarkColorScheme else UrbanLightColorScheme
    val categoryColors = if (darkTheme) DarkCategoryColors else LightCategoryColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCategoryColors provides categoryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
