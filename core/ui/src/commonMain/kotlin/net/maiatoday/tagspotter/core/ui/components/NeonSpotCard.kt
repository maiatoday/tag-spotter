package net.maiatoday.tagspotter.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.maiatoday.tagspotter.core.ui.chips.NeonTagChip
import net.maiatoday.tagspotter.core.ui.theme.DarkPrimaryContainer
import net.maiatoday.tagspotter.core.ui.theme.DarkSecondaryContainer

/**
 * Pure, stateless Compose Multiplatform card for displaying a spot.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeonSpotCard(
    title: String,
    spotId: String = "",
    artist: String? = null,
    imageUrl: String? = null,
    tags: List<String> = emptyList(),
    distanceMeters: Double? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    isStarred: Boolean = false,
    onStarToggle: () -> Unit = {},
    isSaved: Boolean = false,
    onSaveSpot: (() -> Unit)? = null,
    onViewOnMap: (latitude: Double, longitude: Double, title: String) -> Unit = { _, _, _ -> },
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (onCardClick != null) {
        modifier.fillMaxWidth().clickable(onClick = onCardClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkPrimaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!artist.isNullOrBlank()) {
                        Text(
                            text = "By $artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkSecondaryContainer
                        )
                    }
                }

                if (distanceMeters != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkPrimaryContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, DarkPrimaryContainer)
                    ) {
                        Text(
                            text = if (distanceMeters >= 1000) {
                                "${(distanceMeters / 100.0).toInt() / 10.0} km"
                            } else {
                                "${distanceMeters.toInt()} m"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        NeonTagChip(tag = tag, leadingHash = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val viewOnMapClick = {
                onViewOnMap(
                    latitude ?: 0.0,
                    longitude ?: 0.0,
                    title
                )
            }

            if (onSaveSpot != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpotStarButton(
                        isStarred = isStarred,
                        onClick = onStarToggle,
                        modifier = Modifier.weight(1f)
                    )
                    SpotSaveButton(
                        isSaved = isSaved,
                        onClick = onSaveSpot,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SpotViewOnMapButton(
                    onClick = viewOnMapClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpotStarButton(
                        isStarred = isStarred,
                        onClick = onStarToggle,
                        modifier = Modifier.weight(1f)
                    )
                    SpotViewOnMapButton(
                        onClick = viewOnMapClick,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotStarButton(
    isStarred: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isStarred) DarkSecondaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isStarred) DarkSecondaryContainer else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = if (isStarred) "★ Starred" else "☆ Star",
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpotSaveButton(
    isSaved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saveAccent = MaterialTheme.colorScheme.tertiaryContainer
    OutlinedButton(
        onClick = onClick,
        enabled = !isSaved,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = saveAccent.copy(alpha = 0.2f),
            disabledContentColor = saveAccent
        ),
        border = BorderStroke(1.dp, saveAccent)
    ) {
        Icon(
            imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isSaved) "✓ Saved" else "Save Spot",
            fontWeight = if (isSaved) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpotViewOnMapButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkPrimaryContainer,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = "View on Map",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
