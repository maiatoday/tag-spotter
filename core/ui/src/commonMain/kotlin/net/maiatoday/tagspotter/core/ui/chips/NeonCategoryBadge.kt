package net.maiatoday.tagspotter.core.ui.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.maiatoday.tagspotter.core.ui.theme.categoryColors

/**
 * Reusable stateless badge displaying a spot category with neon color coding.
 */
@Composable
fun NeonCategoryBadge(
    category: String,
    modifier: Modifier = Modifier,
    shortForm: Boolean = false,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    contentColor: Color = Color.Black,
    shapeRadius: Dp = if (shortForm) 4.dp else 6.dp,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val bg = backgroundColor ?: MaterialTheme.categoryColors.getColorForCategory(category)
    val displayText = if (shortForm) {
        category.take(3).uppercase()
    } else {
        category.replace("_", " ").uppercase()
    }

    val boxModifier = modifier
        .background(
            color = bg,
            shape = RoundedCornerShape(shapeRadius)
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(
            horizontal = if (shortForm) 6.dp else 12.dp,
            vertical = if (shortForm) 2.dp else 6.dp
        )

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (trailingContent != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayText,
                    color = contentColor,
                    style = if (shortForm) {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                trailingContent()
            }
        } else {
            Text(
                text = displayText,
                color = contentColor,
                style = if (shortForm) {
                    MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                }
            )
        }
    }
}
