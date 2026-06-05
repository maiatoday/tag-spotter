package net.maiatoday.tagspotter.theme

import androidx.compose.ui.graphics.Color

// Neon Urbanist (Dark Theme) colors
val DarkBackground = Color(0xFF131313)
val DarkSurface = Color(0xFF131313)
val DarkOnBackground = Color(0xFFE5E2E1)
val DarkOnSurface = Color(0xFFE5E2E1)
val DarkPrimary = Color(0xFFFFFFFF)
val DarkOnPrimary = Color(0xFF283500)
val DarkPrimaryContainer = Color(0xFFC3F400) // Lime Green
val DarkOnPrimaryContainer = Color(0xFF556D00)
val DarkSecondary = Color(0xFFFFB1C4)
val DarkOnSecondary = Color(0xFF65002E)
val DarkSecondaryContainer = Color(0xFFFF4A8D) // Hot Pink
val DarkOnSecondaryContainer = Color(0xFF590028)
val DarkTertiary = Color(0xFFFFFFFF)
val DarkOnTertiary = Color(0xFF003737)
val DarkTertiaryContainer = Color(0xFF00FBFB) // Cyan
val DarkOnTertiaryContainer = Color(0xFF007070)
val DarkSurfaceVariant = Color(0xFF353534)
val DarkOnSurfaceVariant = Color(0xFFC4C9AC)
val DarkOutline = Color(0xFF8E9379)
val DarkOutlineVariant = Color(0xFF444933)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Neon Urbanist Light (Light Theme) colors
val LightBackground = Color(0xFFFCF8FD)
val LightSurface = Color(0xFFFCF8FD)
val LightOnBackground = Color(0xFF1B1B1E)
val LightOnSurface = Color(0xFF1B1B1E)
val LightPrimary = Color(0xFFB60055) // Hot Pink
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE4006C)
val LightOnPrimaryContainer = Color(0xFFFFFBFF)
val LightSecondary = Color(0xFF006875) // Cyan-Teal
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFF00E3FD) // Cyan
val LightOnSecondaryContainer = Color(0xFF00616D)
val LightTertiary = Color(0xFF436600) // Lime-Green
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFF558100)
val LightOnTertiaryContainer = Color(0xFFFAFFE9)
val LightSurfaceVariant = Color(0xFFE4E1E6)
val LightOnSurfaceVariant = Color(0xFF5C3F45)
val LightOutline = Color(0xFF906E75)
val LightOutlineVariant = Color(0xFFE5BCC4)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF93000A)

val DarkCategoryColors = CategoryColors(
    graffiti = Color(0xFFFF4A8D),     // Neon Hot Pink (matches DarkSecondaryContainer)
    sculpture = Color(0xFF00FBFB),    // Neon Cyan (matches DarkTertiaryContainer)
    nature = Color(0xFFC3F400),       // Lime Green (matches DarkPrimaryContainer)
    architecture = Color(0xFFBF5AF2), // Neon Purple
    publicPlace = Color(0xFFFF9F0A),  // Neon Orange
    default = Color.DarkGray
)

val LightCategoryColors = CategoryColors(
    graffiti = Color(0xFFE4006C),     // Hot Pink (matches LightPrimaryContainer)
    sculpture = Color(0xFF00E3FD),    // Cyan (matches LightSecondaryContainer)
    nature = Color(0xFF558100),       // Lime Green/Olive (matches LightTertiaryContainer)
    architecture = Color(0xFF8E24AA), // Darker Purple
    publicPlace = Color(0xFFE65100),  // Darker Orange
    default = Color.Gray
)
