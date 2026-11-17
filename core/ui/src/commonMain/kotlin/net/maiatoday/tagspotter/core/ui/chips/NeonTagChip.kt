package net.maiatoday.tagspotter.core.ui.chips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.maiatoday.tagspotter.core.ui.theme.DarkTertiaryContainer

/**
 * Reusable stateless neon-styled chip for tags and styles.
 */
@Composable
fun NeonTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    accentColor: Color = DarkTertiaryContainer,
    leadingHash: Boolean = true,
    shapeRadius: Dp = 6.dp
) {
    val displayTag = if (leadingHash && !tag.startsWith("#")) "#$tag" else tag
    val containerColor = if (isSelected) {
        accentColor.copy(alpha = 0.35f)
    } else {
        accentColor.copy(alpha = 0.12f)
    }
    val borderColor = if (isSelected) {
        accentColor
    } else {
        accentColor.copy(alpha = 0.5f)
    }
    val textColor = if (isSelected) {
        accentColor
    } else {
        accentColor.copy(alpha = 0.9f)
    }

    val surfaceModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        shape = RoundedCornerShape(shapeRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = surfaceModifier
    ) {
        Text(
            text = displayTag,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
