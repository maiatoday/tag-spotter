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

@Composable
fun NeonSpotCard(
    data: SpotCardData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    net.maiatoday.tagspotter.core.ui.components.NeonSpotCard(
        title = data.title,
        spotId = data.spotId,
        artist = data.artist,
        imageUrl = data.imageUrl,
        tags = data.tags,
        distanceMeters = data.distanceMeters,
        latitude = data.latitude,
        longitude = data.longitude,
        isStarred = data.isStarred,
        onStarToggle = { onAction(A2UIAction.StarSpot(data.spotId)) },
        onViewOnMap = { lat, lon, title ->
            onAction(
                A2UIAction.ViewOnMap(
                    latitude = lat,
                    longitude = lon,
                    title = title,
                    spotId = data.spotId
                )
            )
        },
        modifier = modifier
    )
}

@Composable
fun NeonTourCard(
    data: TourCardData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    net.maiatoday.tagspotter.core.ui.components.NeonTourCard(
        tourTitle = data.tourTitle,
        description = data.description,
        estimatedMinutes = data.estimatedMinutes,
        totalDistanceKm = data.totalDistanceKm,
        waypoints = data.waypoints.map {
            net.maiatoday.tagspotter.core.ui.components.NeonTourWaypoint(
                index = it.index,
                title = it.title,
                address = it.address,
                latitude = it.latitude,
                longitude = it.longitude,
                imageUrl = it.imageUrl,
                completed = it.completed
            )
        },
        onNavigateToStop = { stopIndex, lat, lon, title ->
            onAction(
                A2UIAction.NavigateToStop(
                    stopIndex = stopIndex,
                    latitude = lat,
                    longitude = lon,
                    title = title
                )
            )
        },
        modifier = modifier
    )
}

@Composable
fun NeonArtistBioCard(
    data: ArtistBioCardData,
    onAction: (A2UIAction) -> Unit,
    modifier: Modifier = Modifier
) {
    net.maiatoday.tagspotter.core.ui.components.NeonArtistBioCard(
        artistName = data.artistName,
        bio = data.bio,
        avatarUrl = data.avatarUrl,
        activeYears = data.activeYears,
        styles = data.styles,
        artworks = data.artworks.map {
            net.maiatoday.tagspotter.core.ui.components.NeonArtworkThumbnail(
                id = it.id,
                title = it.title,
                imageUrl = it.imageUrl
            )
        },
        onArtworkClick = { id, title ->
            onAction(
                A2UIAction.ViewOnMap(
                    latitude = 0.0,
                    longitude = 0.0,
                    title = title,
                    spotId = id
                )
            )
        },
        modifier = modifier
    )
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
    net.maiatoday.tagspotter.core.ui.components.NeonMapPinOverlayWidget(
        title = data.title,
        centerLatitude = data.centerLatitude,
        centerLongitude = data.centerLongitude,
        zoomLevel = data.zoomLevel,
        pins = data.pins.map {
            net.maiatoday.tagspotter.core.ui.components.NeonMapPin(
                id = it.id,
                title = it.title,
                latitude = it.latitude,
                longitude = it.longitude,
                category = it.category
            )
        },
        onPinClick = { pinId, lat, lon, title ->
            onAction(
                A2UIAction.ViewOnMap(
                    latitude = lat,
                    longitude = lon,
                    title = title,
                    spotId = pinId
                )
            )
        },
        onFocusCluster = { lat, lon, title ->
            onAction(
                A2UIAction.ViewOnMap(
                    latitude = lat,
                    longitude = lon,
                    title = title,
                    spotId = null
                )
            )
        },
        modifier = modifier
    )
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
