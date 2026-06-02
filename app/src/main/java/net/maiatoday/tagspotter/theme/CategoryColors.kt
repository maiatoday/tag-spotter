package net.maiatoday.tagspotter.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

@Immutable
data class CategoryColors(
    val graffiti: Color,
    val sculpture: Color,
    val tree: Color,
    val architecture: Color,
    val publicPlace: Color,
    val default: Color
) {
    fun getColorForCategory(category: String): Color {
        return when (category) {
            "graffiti" -> graffiti
            "sculpture" -> sculpture
            "tree" -> tree
            "architecture" -> architecture
            "public_place" -> publicPlace
            else -> default
        }
    }
}

val LocalCategoryColors = staticCompositionLocalOf<CategoryColors> {
    error("No CategoryColors provided")
}

val MaterialTheme.categoryColors: CategoryColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCategoryColors.current
