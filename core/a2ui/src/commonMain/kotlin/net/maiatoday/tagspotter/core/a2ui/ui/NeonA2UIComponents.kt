package net.maiatoday.tagspotter.core.a2ui.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.model.ArtistBioCardData
import net.maiatoday.tagspotter.core.a2ui.model.CommunityVotingData
import net.maiatoday.tagspotter.core.a2ui.model.FallbackComponentData
import net.maiatoday.tagspotter.core.a2ui.model.MapPinOverlayData
import net.maiatoday.tagspotter.core.a2ui.model.SpotCardData
import net.maiatoday.tagspotter.core.a2ui.model.StyleQuizData
import net.maiatoday.tagspotter.core.a2ui.model.TourCardData
import net.maiatoday.tagspotter.core.ui.theme.DarkPrimaryContainer
import net.maiatoday.tagspotter.core.ui.theme.DarkSecondaryContainer
import net.maiatoday.tagspotter.core.ui.theme.DarkTertiaryContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeonSpotCard(
    data: SpotCardData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkPrimaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!data.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = data.title,
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
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!data.artist.isNullOrBlank()) {
                        Text(
                            text = "By ${data.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkSecondaryContainer
                        )
                    }
                }

                if (data.distanceMeters != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkPrimaryContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, DarkPrimaryContainer)
                    ) {
                        Text(
                            text = if (data.distanceMeters >= 1000) {
                                "${(data.distanceMeters / 100.0).toInt() / 10.0} km"
                            } else {
                                "${data.distanceMeters.toInt()} m"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (data.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    data.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkTertiaryContainer.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, DarkTertiaryContainer.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction(A2UIAction.StarSpot(data.spotId)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (data.isStarred) DarkSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (data.isStarred) DarkSecondaryContainer else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(if (data.isStarred) "★ Starred" else "☆ Star")
                }

                Button(
                    onClick = {
                        onAction(
                            A2UIAction.ViewOnMap(
                                latitude = data.latitude ?: 0.0,
                                longitude = data.longitude ?: 0.0,
                                title = data.title,
                                spotId = data.spotId
                            )
                        )
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPrimaryContainer,
                        contentColor = Color.Black
                    )
                ) {
                    Text("View on Map", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NeonTourCard(
    data: TourCardData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkTertiaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.tourTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (data.description.isNotBlank()) {
                        Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkTertiaryContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, DarkTertiaryContainer)
                ) {
                    Text(
                        text = "${data.estimatedMinutes} min • ${data.totalDistanceKm} km",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (data.waypoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Waypoints (${data.waypoints.size} stops)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.waypoints.forEach { stop ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .clickable {
                                    onAction(
                                        A2UIAction.NavigateToStop(
                                            stopIndex = stop.index,
                                            latitude = stop.latitude,
                                            longitude = stop.longitude,
                                            title = stop.title
                                        )
                                    )
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (stop.completed) DarkPrimaryContainer else DarkTertiaryContainer.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, if (stop.completed) DarkPrimaryContainer else DarkTertiaryContainer),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (stop.completed) "✓" else "${stop.index}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (stop.completed) Color.Black else DarkTertiaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stop.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!stop.address.isNullOrBlank()) {
                                    Text(
                                        text = stop.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val nextStop = data.waypoints.firstOrNull { !it.completed } ?: data.waypoints.firstOrNull()
                if (nextStop != null) {
                    Button(
                        onClick = {
                            onAction(
                                A2UIAction.NavigateToStop(
                                    stopIndex = nextStop.index,
                                    latitude = nextStop.latitude,
                                    longitude = nextStop.longitude,
                                    title = nextStop.title
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTertiaryContainer,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Navigate to Stop #${nextStop.index} (${nextStop.title})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeonArtistBioCard(
    data: ArtistBioCardData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkSecondaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!data.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = data.avatarUrl,
                        contentDescription = data.artistName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = DarkSecondaryContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, DarkSecondaryContainer),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = data.artistName.firstOrNull()?.uppercase() ?: "A",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = DarkSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = data.artistName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!data.activeYears.isNullOrBlank()) {
                        Text(
                            text = "Active: ${data.activeYears}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = data.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )

            if (data.styles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    data.styles.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSecondaryContainer.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, DarkSecondaryContainer.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (data.artworks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Notable Pieces",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.artworks.forEach { artwork ->
                        Column(
                            modifier = Modifier
                                .width(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onAction(
                                        A2UIAction.ViewOnMap(
                                            latitude = 0.0,
                                            longitude = 0.0,
                                            title = artwork.title,
                                            spotId = artwork.id
                                        )
                                    )
                                }
                        ) {
                            if (artwork.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = artwork.imageUrl,
                                    contentDescription = artwork.title,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎨", fontSize = 24.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = artwork.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeonStyleQuizWidget(
    data: StyleQuizData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    val isAnswered = selectedOptionId != null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkPrimaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkPrimaryContainer.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "STREET ART QUIZ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.question,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!data.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = data.question,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.options.forEach { option ->
                    val isSelected = selectedOptionId == option.id
                    val borderColor = when {
                        !isAnswered -> MaterialTheme.colorScheme.outline
                        isSelected && option.isCorrect -> DarkPrimaryContainer
                        isSelected && !option.isCorrect -> DarkSecondaryContainer
                        option.isCorrect -> DarkPrimaryContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected && option.isCorrect -> DarkPrimaryContainer.copy(alpha = 0.2f)
                            isSelected && !option.isCorrect -> DarkSecondaryContainer.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        },
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isAnswered) {
                                selectedOptionId = option.id
                                onAction(
                                    A2UIAction.AnswerQuiz(
                                        quizId = data.quizId,
                                        optionId = option.id,
                                        isCorrect = option.isCorrect
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    isSelected && option.isCorrect -> "✓ "
                                    isSelected && !option.isCorrect -> "✗ "
                                    else -> "• "
                                },
                                fontWeight = FontWeight.Bold,
                                color = borderColor
                            )
                            Text(
                                text = option.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (isAnswered && !data.explanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${data.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NeonCommunityVotingCard(
    data: CommunityVotingData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var votedOptionId by remember { mutableStateOf<String?>(null) }
    val total = if (votedOptionId != null) {
        data.totalVotes + 1
    } else {
        maxOf(data.totalVotes, 1)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkSecondaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "COMMUNITY POLL",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DarkSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (data.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.options.forEach { option ->
                    val isUserVote = votedOptionId == option.id
                    val count = option.votes + if (isUserVote) 1 else 0
                    val progress = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (isUserVote) DarkSecondaryContainer else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                votedOptionId = option.id
                                onAction(A2UIAction.VoteOption(data.pollId, option.id))
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = (if (isUserVote) "● " else "○ ") + option.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isUserVote) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isUserVote) DarkSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}% ($count)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isUserVote) DarkSecondaryContainer else DarkTertiaryContainer,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeonMapPinOverlayWidget(
    data: MapPinOverlayData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, DarkTertiaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkTertiaryContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, DarkTertiaryContainer)
                ) {
                    Text(
                        text = "${data.pins.size} spots",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                data.pins.take(4).forEach { pin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .clickable {
                                onAction(
                                    A2UIAction.ViewOnMap(
                                        latitude = pin.latitude,
                                        longitude = pin.longitude,
                                        title = pin.title,
                                        spotId = pin.id
                                    )
                                )
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pin.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (!pin.category.isNullOrBlank()) {
                            Text(
                                text = pin.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTertiaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onAction(
                        A2UIAction.ViewOnMap(
                            latitude = data.centerLatitude,
                            longitude = data.centerLongitude,
                            title = data.title,
                            spotId = null
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkTertiaryContainer,
                    contentColor = Color.Black
                )
            ) {
                Text("Focus Map on Cluster", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NeonFallbackCard(
    data: FallbackComponentData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚠️ ${data.title}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction(A2UIAction.Retry(data.rawJson)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Retry")
                }

                FilledTonalButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isExpanded) "Hide JSON" else "Inspect JSON")
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = data.rawJson,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF00FF66),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
