package net.maiatoday.tagspotter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.ui.viewmodel.GalleryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun GalleryScreen(
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val spots by viewModel.spots.collectAsStateWithLifecycle()

    val categories = listOf("All", "graffiti", "sculpture", "tree", "architecture", "public_place")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Categories Filter Header
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category.replace("_", " ").replaceFirstChar { it.titlecase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (category) {
                            "graffiti" -> MaterialTheme.colorScheme.primary
                            "sculpture" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                        labelColor = Color.Gray
                    )
                )
            }
        }

        if (spots.isEmpty()) {
            EmptyGalleryState(category = selectedCategory)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(spots, key = { it.spot.id }) { spotDetails ->
                    SpotGridCard(
                        spotDetails = spotDetails,
                        onClick = { onSpotClick(spotDetails.spot.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotGridCard(
    spotDetails: SpotDetails,
    onClick: () -> Unit
) {
    // Get latest image based on timestamp
    val latestImage = spotDetails.images.maxByOrNull { it.timestamp }
    val isErased = spotDetails.spot.status == "erased"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                if (isErased) Color.DarkGray else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.DarkGray)
            ) {
                if (latestImage != null) {
                    val imageModel = remember(latestImage.imagePath, latestImage.thumbnailPath) {
                        if (latestImage.thumbnailPath.isNotEmpty() && !latestImage.thumbnailPath.startsWith("android.resource://") && !latestImage.thumbnailPath.startsWith("http")) {
                            File(latestImage.thumbnailPath)
                        } else if (latestImage.thumbnailPath.isNotEmpty() && (latestImage.thumbnailPath.startsWith("android.resource://") || latestImage.thumbnailPath.startsWith("http"))) {
                            latestImage.thumbnailPath.toUri()
                        } else if (latestImage.imagePath.startsWith("content://") || latestImage.imagePath.startsWith("android.resource://") || latestImage.imagePath.startsWith("http")) {
                            latestImage.imagePath.toUri()
                        } else {
                            File(latestImage.imagePath)
                        }
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Spot photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Erased Overlay
                if (isErased) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GONE / ERASED",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = when (spotDetails.spot.category) {
                                    "graffiti" -> MaterialTheme.colorScheme.tertiary
                                    "sculpture" -> MaterialTheme.colorScheme.secondary
                                    "tree" -> MaterialTheme.colorScheme.primary
                                    "architecture" -> Color(0xFFBF5AF2)
                                    "public_place" -> Color(0xFFFF9F0A)
                                    else -> Color.DarkGray
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = spotDetails.spot.category.take(3).uppercase(),
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Formatting timestamp
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val formattedDate = sdf.format(Date(spotDetails.spot.createdAt))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (spotDetails.spot.artists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by " + spotDetails.spot.artists.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = spotDetails.spot.description.ifEmpty { "No description added." },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Sub-tags list
                if (spotDetails.spot.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        maxItemsInEachRow = 2
                    ) {
                        spotDetails.spot.tags.take(3).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGalleryState(category: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Spots Found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (category == "All") {
                "Document your city walks! Tap the 'Capture' tab below to photograph your first spot."
            } else {
                "You haven't tagged any items in the '${category.replace("_", " ")}' category yet."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
