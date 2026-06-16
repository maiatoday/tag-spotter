package net.maiatoday.tagspotter.feature.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.ui.getCategoryInactiveStatusLabel
import net.maiatoday.tagspotter.core.ui.theme.categoryColors
import net.maiatoday.tagspotter.feature.gallery.res.DateFormatter
import net.maiatoday.tagspotter.feature.gallery.res.GalleryStrings
import net.maiatoday.tagspotter.feature.gallery.res.formatImageModel
import net.maiatoday.tagspotter.feature.gallery.res.rememberGalleryPlatformHelper
import net.maiatoday.tagspotter.feature.gallery.res.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GalleryScreen(
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    viewModel: GalleryViewModel = koinViewModel()
) {
    val platformHelper = rememberGalleryPlatformHelper()
    val deletedSuccessfullyText = stringResource(GalleryStrings.deleted_successfully)
    val spotsStarredText = stringResource(GalleryStrings.spots_starred)
    val spotsUnstarredText = stringResource(GalleryStrings.spots_unstarred)
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val spots by viewModel.spots.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showStarredOnly by viewModel.showStarredOnly.collectAsStateWithLifecycle()

    val activeFilterCenter by viewModel.activeFilterCenter.collectAsStateWithLifecycle()
    val activeRadiusMeters by viewModel.activeRadiusMeters.collectAsStateWithLifecycle()
    val homeCityName by viewModel.homeCity.collectAsStateWithLifecycle()

    var showFilterBottomSheet by remember { mutableStateOf(false) }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedSpotIds = remember { mutableStateListOf<Long>() }
    var spotsToDelete by remember { mutableStateOf<List<Long>>(emptyList()) }
    var spotsToExport by remember { mutableStateOf<List<SpotDetails>>(emptyList()) }
    var isShareMenuExpanded by remember { mutableStateOf(false) }

    var showLimitExceededDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var exportMinRatingThreshold by remember { mutableIntStateOf(0) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                GalleryViewModel.UiEvent.StarLimitExceeded -> {
                    showLimitExceededDialog = true
                }
            }
        }
    }

    val createDocumentLauncher = platformHelper.rememberLauncher { uriString ->
        platformHelper.exportPack(
            uriString = uriString,
            spots = spotsToExport,
            minRating = exportMinRatingThreshold,
            onSuccess = {
                platformHelper.showToast("Saved successfully!")
                if (isMultiSelectMode) {
                    selectedSpotIds.clear()
                    isMultiSelectMode = false
                }
                spotsToExport = emptyList()
            },
            onError = { e ->
                platformHelper.showToast("Failed to save: ${e.message}")
            }
        )
    }

    val categories = listOf("All") + Spot.CATEGORIES

    if (spotsToDelete.isNotEmpty()) {
        val isBulk = spotsToDelete.size == spots.size && !isMultiSelectMode
        AlertDialog(
            onDismissRequest = { spotsToDelete = emptyList() },
            title = {
                Text(
                    stringResource(
                        if (isBulk) GalleryStrings.delete_filtered_spots_title
                        else GalleryStrings.delete_selected_spots_title
                    )
                )
            },
            text = {
                Text(
                    if (isBulk) {
                        stringResource(GalleryStrings.delete_filtered_spots_confirm, spotsToDelete.size)
                    } else {
                        stringResource(GalleryStrings.delete_selected_spots_confirm, spotsToDelete.size)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSpots(spotsToDelete) {
                            if (isMultiSelectMode) {
                                selectedSpotIds.clear()
                                isMultiSelectMode = false
                            }
                            spotsToDelete = emptyList()
                            platformHelper.showToast(deletedSuccessfullyText)
                        }
                    }
                ) {
                    Text(stringResource(GalleryStrings.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { spotsToDelete = emptyList() }) {
                    Text(stringResource(GalleryStrings.cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isMultiSelectMode) {
            // Multi-Select Action Bar (Consolidated Top Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        selectedSpotIds.clear()
                        isMultiSelectMode = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedSpotIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    val allVisibleIds = spots.map { it.spot.id }
                    val isAllSelected = selectedSpotIds.size == allVisibleIds.size

                    TextButton(
                        onClick = {
                            if (isAllSelected) {
                                selectedSpotIds.clear()
                            } else {
                                selectedSpotIds.clear()
                                selectedSpotIds.addAll(allVisibleIds)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(if (isAllSelected) GalleryStrings.deselect_all else GalleryStrings.select_all),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Star toggle
                    IconButton(
                        onClick = {
                            val selectedSpots = spots.filter { it.spot.id in selectedSpotIds }
                            val anyUnstarred = selectedSpots.any { !it.spot.isStarred }
                            if (anyUnstarred) {
                                viewModel.bulkUpdateStarred(
                                    selectedSpotIds.toList(),
                                    isStarred = true
                               ) {
                                    selectedSpotIds.clear()
                                    isMultiSelectMode = false
                                    platformHelper.showToast(spotsStarredText)
                                }
                            } else {
                                viewModel.bulkUpdateStarred(
                                    selectedSpotIds.toList(),
                                    isStarred = false
                                ) {
                                    selectedSpotIds.clear()
                                    isMultiSelectMode = false
                                    platformHelper.showToast(spotsUnstarredText)
                                }
                            }
                        },
                        enabled = selectedSpotIds.isNotEmpty()
                    ) {
                        val anyUnstarred = spots.filter { it.spot.id in selectedSpotIds }
                            .any { !it.spot.isStarred }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(GalleryStrings.content_desc_toggle_star),
                            tint = if (selectedSpotIds.isEmpty()) {
                                Color.Gray
                            } else if (anyUnstarred) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFFFFD700) // Starred gold
                            }
                        )
                    }

                    Box {
                        IconButton(
                            onClick = {
                                isShareMenuExpanded = true
                            },
                            enabled = selectedSpotIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(GalleryStrings.content_desc_export),
                                tint = if (selectedSpotIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        DropdownMenu(
                            expanded = isShareMenuExpanded,
                            onDismissRequest = { isShareMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(GalleryStrings.export_pack)) },
                                onClick = {
                                    isShareMenuExpanded = false
                                    spotsToExport = spots.filter { it.spot.id in selectedSpotIds }
                                    showExportOptionsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(GalleryStrings.get_route_google_maps)) },
                                onClick = {
                                    isShareMenuExpanded = false
                                    val selectedSpots =
                                        selectedSpotIds.mapNotNull { id -> spots.find { it.spot.id == id } }
                                    if (selectedSpots.isNotEmpty()) {
                                        platformHelper.getRoute(selectedSpots)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(GalleryStrings.share_kml)) },
                                onClick = {
                                    isShareMenuExpanded = false
                                    val selectedSpots =
                                        selectedSpotIds.mapNotNull { id -> spots.find { it.spot.id == id } }
                                    if (selectedSpots.isNotEmpty()) {
                                        platformHelper.shareKml(selectedSpots)
                                    }
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { spotsToDelete = selectedSpotIds.toList() },
                        enabled = selectedSpotIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (selectedSpotIds.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            }
        } else if (isSearchExpanded) {
            // Expanded Search/Filter Top Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        placeholder = { Text(stringResource(GalleryStrings.search_placeholder)) },
                        leadingIcon = null,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        showFilterBottomSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = if (selectedSource != "All" || activeFilterCenter != null || showStarredOnly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = {
                        isSearchExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Collapse",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Categories Filter Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectCategory(category) },
                                label = {
                                    Text(
                                        category.replace("_", " ")
                                            .replaceFirstChar { it.titlecase() })
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (category == "All") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.categoryColors.getColorForCategory(category)
                                    },
                                    selectedLabelColor = MaterialTheme.colorScheme.background,
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }

                    val hasActiveAdvancedFilters = searchQuery.isNotEmpty() ||
                            selectedCategory != "All" ||
                            selectedSource != "All" ||
                            activeFilterCenter != null ||
                            showStarredOnly

                    if (hasActiveAdvancedFilters) {
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
                                .padding(end = 16.dp, start = 8.dp)
                        )
                    }
                }
            }
        } else {
            // Normal Mode Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMenuClick() }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "TAGSPOTTER",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { isSearchExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Active chips in normal mode (scrollable row)
        val hasActiveFilters = searchQuery.isNotEmpty() ||
                selectedCategory != "All" ||
                selectedSource != "All" ||
                activeFilterCenter != null ||
                showStarredOnly

        if (!isSearchExpanded && hasActiveFilters) {
            val activeFilterCount = (if (searchQuery.isNotEmpty()) 1 else 0) +
                    (if (selectedCategory != "All") 1 else 0) +
                    (if (selectedSource != "All") 1 else 0) +
                    (if (activeFilterCenter != null) 1 else 0) +
                    (if (showStarredOnly) 1 else 0)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Query Chip
                if (searchQuery.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { isSearchExpanded = true },
                            label = { Text(stringResource(GalleryStrings.search_label, searchQuery)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search query",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setSearchQuery("") }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Category Chip
                if (selectedCategory != "All") {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { isSearchExpanded = true },
                            label = {
                                Text(
                                    selectedCategory.replace("_", " ")
                                        .replaceFirstChar { it.titlecase() })
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear category filter",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.selectCategory("All") }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.categoryColors.getColorForCategory(
                                    selectedCategory
                                ).copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.categoryColors.getColorForCategory(
                                    selectedCategory
                                )
                            )
                        )
                    }
                }

                // Source Chip
                if (selectedSource != "All") {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { showFilterBottomSheet = true },
                            label = { Text(selectedSource) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear source filter",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.selectSource("All") }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                // Location Chip
                if (activeFilterCenter != null) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { showFilterBottomSheet = true },
                            label = {
                                Text(
                                    text = "${activeFilterCenter?.displayName} (${
                                        LocationUtils.getRadiusLabel(
                                            activeRadiusMeters
                                        )
                                    })"
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear location filter",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.clearLocationFilter() }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Starred Only Chip
                if (showStarredOnly) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleShowStarredOnly() },
                            label = { Text(stringResource(GalleryStrings.starred_only)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFFFD700)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear starred filter",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setShowStarredOnly(false) }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Clear All Button
                if (activeFilterCount > 1) {
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
        }

        if (spots.isEmpty()) {
            EmptyGalleryState(
                category = selectedCategory,
                query = searchQuery,
                activeFilterCenter = activeFilterCenter,
                showStarredOnly = showStarredOnly,
                onClearLocationFilter = { viewModel.clearLocationFilter() },
                onClearStarredFilter = { if (showStarredOnly) viewModel.toggleShowStarredOnly() }
            )
        } else {
            val columns = 2 // Since LocalConfiguration is not KMP-ready for wasm/ios cleanly, static 2-column looks great on most screens, or we can use adaptive width or similar. For simplicity, static 2 is standard for phone grid.

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp
            ) {
                items(spots, key = { it.spot.id }) { spotDetails ->
                    val isSelected = selectedSpotIds.contains(spotDetails.spot.id)
                    SpotGridCard(
                        modifier = Modifier.fillMaxWidth(),
                        spotDetails = spotDetails,
                        isSelected = isSelected,
                        isMultiSelectMode = isMultiSelectMode,
                        activeFilterCenter = activeFilterCenter,
                        onClick = {
                            if (isMultiSelectMode) {
                                if (isSelected) {
                                    selectedSpotIds.remove(spotDetails.spot.id)
                                } else {
                                    selectedSpotIds.add(spotDetails.spot.id)
                                }
                            } else {
                                onSpotClick(spotDetails.spot.id)
                            }
                        },
                        onLongClick = {
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                                selectedSpotIds.add(spotDetails.spot.id)
                            }
                        }
                    )
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

    if (showLimitExceededDialog) {
        AlertDialog(
            onDismissRequest = { showLimitExceededDialog = false },
            title = { Text(stringResource(GalleryStrings.starred_limit_reached_title)) },
            text = {
                Text(stringResource(GalleryStrings.starred_limit_reached_message))
            },
            confirmButton = {
                TextButton(onClick = { showLimitExceededDialog = false }) {
                    Text(stringResource(GalleryStrings.ok))
                }
            }
        )
    }

    if (showExportOptionsDialog) {
        var tempMinRating by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = { Text(stringResource(GalleryStrings.export_pack_options_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(GalleryStrings.export_min_rating_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            stringResource(GalleryStrings.min_rating_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                val isStarred = i <= tempMinRating
                                Icon(
                                    imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = stringResource(GalleryStrings.content_desc_star_rating, i),
                                    tint = if (isStarred) Color(0xFFFFD700) else Color.Gray,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            tempMinRating = if (tempMinRating == i) 0 else i
                                        }
                                )
                            }
                        }
                    }
                    val ratingLabel = when (tempMinRating) {
                        0 -> stringResource(GalleryStrings.rating_all_photos)
                        1 -> stringResource(GalleryStrings.rating_1_and_above)
                        2 -> stringResource(GalleryStrings.rating_2_and_above)
                        3 -> stringResource(GalleryStrings.rating_3_and_above)
                        4 -> stringResource(GalleryStrings.rating_4_and_above)
                        5 -> stringResource(GalleryStrings.rating_5_only)
                        else -> ""
                    }
                    Text(
                        text = ratingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        exportMinRatingThreshold = tempMinRating
                        showExportOptionsDialog = false
                        createDocumentLauncher()
                    }
                ) {
                    Text(stringResource(GalleryStrings.export))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportOptionsDialog = false }) {
                    Text(stringResource(GalleryStrings.cancel))
                }
            }
        )
    }
}
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SpotGridCard(
    spotDetails: SpotDetails,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeFilterCenter: FilterCenter? = null
) {
    // Get main thumbnail or latest image based on timestamp
    val latestImage = spotDetails.images.firstOrNull { it.isMain }
        ?: spotDetails.images.maxByOrNull { it.timestamp }
    val isErased = spotDetails.spot.status == "erased"

    Card(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                1.dp,
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else if (isErased) {
                    Color.DarkGray
                } else {
                    MaterialTheme.categoryColors.getColorForCategory(spotDetails.spot.category)
                        .copy(alpha = 0.4f)
                },
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
                        formatImageModel(latestImage.imagePath, latestImage.thumbnailPath)
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
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = spotDetails.spot.category.getCategoryInactiveStatusLabel())
                                .uppercase(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(4.dp)
                                )
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
                                color = MaterialTheme.categoryColors.getColorForCategory(spotDetails.spot.category),
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

                // Imported Badge
                if (spotDetails.spot.isImported) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "IMPORTED",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Starred Badge
                if (spotDetails.spot.isStarred) {
                    Box(
                        modifier = Modifier
                            .align(if (isMultiSelectMode) Alignment.BottomEnd else Alignment.TopStart)
                            .padding(6.dp)
                            .background(
                                color = Color(0xFFFFD700), // Gold
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Starred Spot",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Selection Overlay
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent
                            )
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
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
                val formattedDate = remember(spotDetails.spot.createdAt) {
                    DateFormatter.formatDate(spotDetails.spot.createdAt, "MMM dd, yyyy")
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (activeFilterCenter != null) {
                    val distanceMeters = LocationUtils.calculateDistance(
                        activeFilterCenter.latitude,
                        activeFilterCenter.longitude,
                        spotDetails.spot.latitude,
                        spotDetails.spot.longitude
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${LocationUtils.getRadiusLabel(distanceMeters)} away",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = spotDetails.spot.description.ifEmpty { "No description added." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (spotDetails.spot.artists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by " + spotDetails.spot.artists.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
fun EmptyGalleryState(
    category: String,
    query: String = "",
    activeFilterCenter: FilterCenter? = null,
    showStarredOnly: Boolean = false,
    onClearLocationFilter: () -> Unit = {},
    onClearStarredFilter: () -> Unit = {}
) {
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
            text = stringResource(GalleryStrings.no_spots_found),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (activeFilterCenter != null) {
                stringResource(GalleryStrings.no_spots_range, activeFilterCenter.displayName)
            } else if (showStarredOnly) {
                stringResource(GalleryStrings.no_starred_spots)
            } else if (query.isNotEmpty()) {
                stringResource(GalleryStrings.no_spots_match_query, query)
            } else if (category == "All") {
                stringResource(GalleryStrings.first_spot_instruction)
            } else {
                stringResource(GalleryStrings.empty_category_instruction, category.replace("_", " "))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        if (activeFilterCenter != null || showStarredOnly) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onClearLocationFilter()
                    onClearStarredFilter()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(GalleryStrings.show_all_spots))
            }
        }
    }
}
