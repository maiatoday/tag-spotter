package net.maiatoday.tagspotter.theme

import androidx.compose.ui.graphics.Color

val UrbanBackground = Color(0xFF0F0F11)
val UrbanSurface = Color(0xFF1B1B1F)
val UrbanPrimary = Color(0xFF00FF88) // Neon Green
val UrbanSecondary = Color(0xFF00F0FF) // Neon Cyan
val UrbanTertiary = Color(0xFFFF007F) // Hot Pink
val UrbanOnBackground = Color(0xFFF3F3F5)
val UrbanOnSurface = Color(0xFFE4E4E9)
val UrbanError = Color(0xFFFF453A)
val UrbanOnError = Color(0xFFFFFFFF)

// Light Theme colors
val LightBackground = Color(0xFFF9F9FB)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimary = Color(0xFF007D44) // Forest Green (high contrast primary)
val LightSecondary = Color(0xFF006B75) // Deep Teal
val LightTertiary = Color(0xFFC2185B) // Magenta/Pink
val LightOnBackground = Color(0xFF1A1A1E)
val LightOnSurface = Color(0xFF2E2E33)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

val DarkCategoryColors = CategoryColors(
    graffiti = Color(0xFFFF007F),     // Neon Hot Pink (matches UrbanTertiary)
    sculpture = Color(0xFF00F0FF),    // Neon Cyan (matches UrbanSecondary)
    tree = Color(0xFF00FF88),         // Neon Green (matches UrbanPrimary)
    architecture = Color(0xFFBF5AF2), // Neon Purple
    publicPlace = Color(0xFFFF9F0A),  // Neon Orange
    default = Color.DarkGray
)

val LightCategoryColors = CategoryColors(
    graffiti = Color(0xFFD81B60),     // Darker Pink/Magenta
    sculpture = Color(0xFF007A78),    // Darker Teal/Cyan
    tree = Color(0xFF2E7D32),         // Darker Green
    architecture = Color(0xFF8E24AA), // Darker Purple
    publicPlace = Color(0xFFE65100),  // Darker Orange
    default = Color.Gray
)
