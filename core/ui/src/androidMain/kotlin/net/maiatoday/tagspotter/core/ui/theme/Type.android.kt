package net.maiatoday.tagspotter.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import net.maiatoday.tagspotter.core.ui.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val SpaceGroteskFont = GoogleFont("Space Grotesk")
actual val SpaceGroteskFontFamily: FontFamily = FontFamily(
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val HankenGroteskFont = GoogleFont("Hanken Grotesk")
actual val HankenGroteskFontFamily: FontFamily = FontFamily(
    Font(googleFont = HankenGroteskFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = HankenGroteskFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = HankenGroteskFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = HankenGroteskFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val InterFont = GoogleFont("Inter")
actual val InterFontFamily: FontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val JetBrainsMonoFont = GoogleFont("JetBrains Mono")
actual val JetBrainsMonoFontFamily: FontFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)
