package net.maiatoday.tagspotter.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.getCategoryInactiveStatusLabel
import net.maiatoday.tagspotter.core.ui.theme.categoryColors
import net.maiatoday.tagspotter.feature.gallery.FilterBottomSheet
import net.maiatoday.tagspotter.core.ui.OsmMapView
import net.maiatoday.tagspotter.core.ui.OsmMarker
import net.maiatoday.tagspotter.core.model.LocationUtils
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val spots by viewModel.spots.collectAsStateWithLifecycle()
    val selectedSpot by viewModel.selectedSpot.collectAsStateWithLifecycle()
    val initialCenter by viewModel.initialMapCenter.collectAsStateWithLifecycle()

    val activeFilterCenter by viewModel.activeFilterCenter.collectAsStateWithLifecycle()
    val activeRadiusMeters by viewModel.activeRadiusMeters.collectAsStateWithLifecycle()
    val homeCityName by viewModel.homeCity.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val showStarredOnly by viewModel.showStarredOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val darkMapEnabled by viewModel.darkMapEnabled.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val useDarkMap = isSystemDark && darkMapEnabled

    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var mapViewInstance: MapView? by remember { mutableStateOf(null) }

    // Trigger map camera update when activeFilterCenter or activeRadiusMeters changes
    LaunchedEffect(activeFilterCenter, activeRadiusMeters) {
        val center = activeFilterCenter
        val map = mapViewInstance
        if (map != null) {
            if (center != null) {
                map.controller.animateTo(GeoPoint(center.latitude, center.longitude))
                val zoom = when {
                    activeRadiusMeters <= 500.0 -> 16.0
                    activeRadiusMeters <= 1000.0 -> 15.0
                    activeRadiusMeters <= 2000.0 -> 14.0
                    activeRadiusMeters <= 5000.0 -> 13.0
                    activeRadiusMeters <= 10000.0 -> 12.0
                    activeRadiusMeters <= 20000.0 -> 11.0
                    else -> 10.0
                }
                map.controller.setZoom(zoom)
            } else {
                if (spots.isNotEmpty()) {
                    val points = spots.map { GeoPoint(it.spot.latitude, it.spot.longitude) }
                    val bbox = BoundingBox.fromGeoPoints(points)
                    map.zoomToBoundingBox(bbox, true, 80)
                }
            }
        }
    }

    // Map spots to OsmMarkers
    val markers = spots.map { spotDetails ->
        OsmMarker(
            id = spotDetails.spot.id,
            latitude = spotDetails.spot.latitude,
            longitude = spotDetails.spot.longitude,
            category = spotDetails.spot.category,
            status = spotDetails.spot.status,
            title = spotDetails.spot.category.replace("_", " ").replaceFirstChar { it.titlecase() },
            isStarred = spotDetails.spot.isStarred,
            onClick = {
                viewModel.selectSpot(spotDetails)
                mapViewInstance?.controller?.animateTo(
                    GeoPoint(spotDetails.spot.latitude, spotDetails.spot.longitude)
                )
            }
        )
    }

    val initialCenterState = initialCenter

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (initialCenterState != null) {
            OsmMapView(
                latitude = initialCenterState.latitude,
                longitude = initialCenterState.longitude,
                zoomLevel = 14.0,
                markers = markers,
                useDarkMap = useDarkMap,
                modifier = Modifier.fillMaxSize(),
                radiusCircleCenter = activeFilterCenter?.let { GeoPoint(it.latitude, it.longitude) },
                radiusCircleMeters = activeRadiusMeters,
                onMapClick = {
                    viewModel.selectSpot(null)
                },
                onMapReady = { map ->
                    mapViewInstance = map
                }
            )
        }

        // Floating Category Filter Overlay Row
        LazyRow(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 12.dp, end = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All") + Spot.CATEGORIES
            items(categories) { category ->
                val isSelected = selectedCategory == category
                val categoryColor = if (category == "All") {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.categoryColors.getColorForCategory(category)
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) categoryColor.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) categoryColor else Color.DarkGray,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.selectCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category.replace("_", " ").replaceFirstChar { it.titlecase() },
                        color = if (isSelected) categoryColor else Color.LightGray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Floating Location/Source/Starred Filter Active Chip Row or [+ Add Filters] button
        val hasMapActiveFilters = selectedSource != "All" || activeFilterCenter != null || showStarredOnly || searchQuery.isNotEmpty() || selectedCategory != "All"
        val mapActiveFiltersCount = (if (selectedSource != "All") 1 else 0) +
                (if (activeFilterCenter != null) 1 else 0) +
                (if (showStarredOnly) 1 else 0) +
                (if (searchQuery.isNotEmpty()) 1 else 0) +
                (if (selectedCategory != "All") 1 else 0)

        if (hasMapActiveFilters) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp, start = 16.dp, end = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                if (selectedCategory != "All") {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.categoryColors.getColorForCategory(selectedCategory).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.categoryColors.getColorForCategory(selectedCategory).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable { showFilterBottomSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedCategory.replace("_", " ").replaceFirstChar { it.titlecase() },
                                color = MaterialTheme.categoryColors.getColorForCategory(selectedCategory),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear category filter",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.selectCategory("All") }
                            )
                        }
                    }
                }

                // Search Query Chip
                if (searchQuery.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable { showFilterBottomSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Search: \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search query",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.setSearchQuery("") }
                            )
                        }
                    }
                }

                // Source Chip
                if (selectedSource != "All") {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable { showFilterBottomSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedSource,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear source filter",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.selectSource("All") }
                            )
                        }
                    }
                }

                // Location Chip
                if (activeFilterCenter != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable { showFilterBottomSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${activeFilterCenter?.displayName} (${LocationUtils.getRadiusLabel(activeRadiusMeters)})",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear location filter",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.clearLocationFilter() }
                            )
                        }
                    }
                }

                // Starred Only Chip
                if (showStarredOnly) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable { viewModel.toggleShowStarredOnly() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Starred Only",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear starred filter",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.setShowStarredOnly(false) }
                            )
                        }
                    }
                }

                // Clear All Button
                if (mapActiveFiltersCount > 1) {
                    item {
                        Text(
                            text = "Clear All",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    viewModel.setSearchQuery("")
                                    viewModel.selectCategory("All")
                                    viewModel.selectSource("All")
                                    viewModel.clearLocationFilter()
                                    viewModel.setShowStarredOnly(false)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            // Fallback Button
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { showFilterBottomSheet = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ Add Filters",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay Slide-up card for Selected Pin details
        AnimatedVisibility(
            visible = selectedSpot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            val spot = selectedSpot
            if (spot != null) {
                val latestImage = spot.images.firstOrNull { it.isMain } ?: spot.images.maxByOrNull { it.timestamp }
                val isErased = spot.spot.status == "erased"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isErased) Color.DarkGray else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (latestImage != null) {
                                val imageModel = remember(latestImage.imagePath, latestImage.thumbnailPath) {
                                    if (latestImage.thumbnailPath.isNotEmpty()) {
                                        File(latestImage.thumbnailPath)
                                    } else if (latestImage.imagePath.startsWith("content://")) {
                                        latestImage.imagePath.toUri()
                                    } else {
                                        File(latestImage.imagePath)
                                    }
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Detail Info Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = spot.spot.category.replace("_", " ").replaceFirstChar { it.titlecase() },
                                        color = if (isErased) Color.Gray else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (spot.spot.isStarred) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Starred",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (isErased) {
                                        Text(
                                            text = "(${spot.spot.category.getCategoryInactiveStatusLabel()})",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = spot.spot.description.ifEmpty { "No description added." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Display tags
                                if (spot.spot.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        spot.spot.tags.take(3).forEach { tag ->
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onSpotClick(spot.spot.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isErased) Color.DarkGray else MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.background
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("View History & Notes", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // Close icon overlay
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details overlay",
                            tint = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .clickable { viewModel.selectSpot(null) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterBottomSheet) {
        FilterBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            currentCenter = activeFilterCenter,
            currentRadiusMeters = activeRadiusMeters,
            homeCityName = homeCityName,
            onApplyFilter = { center, radius ->
                viewModel.setLocationFilter(center, radius)
            },
            selectedSource = selectedSource,
            onApplySourceFilter = { viewModel.selectSource(it) },
            showStarredOnly = showStarredOnly,
            onApplyStarredFilter = { viewModel.setShowStarredOnly(it) }
        )
    }
}
