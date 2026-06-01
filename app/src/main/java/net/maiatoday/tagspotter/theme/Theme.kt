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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode by default for urban aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = UrbanDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
