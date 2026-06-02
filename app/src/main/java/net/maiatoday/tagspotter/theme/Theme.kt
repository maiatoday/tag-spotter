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

    CompositionLocalProvider(LocalCategoryColors provides categoryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
