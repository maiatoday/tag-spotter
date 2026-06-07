package net.maiatoday.spotcache.feature.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import net.maiatoday.spotcache.core.model.SpotDetails
import net.maiatoday.spotcache.core.ui.theme.categoryColors
import net.maiatoday.spotcache.core.model.FilterCenter
import net.maiatoday.spotcache.core.model.LocationUtils
import net.maiatoday.spotcache.core.database.PackManager
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    viewModel: GalleryViewModel = koinViewModel()
) {
    val context = LocalContext.current
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
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
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



    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            val selectedSpots = spots.filter { it.spot.id in selectedSpotIds }
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    PackManager.exportPack(context, selectedSpots, outputStream, minRating = exportMinRatingThreshold)
                }
                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_LONG).show()
                selectedSpotIds.clear()
                isMultiSelectMode = false
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val categories = listOf("All", "graffiti", "sculpture", "nature", "architecture", "public_place")
    val sources = listOf("All", "My Spots", "Imported")

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Selected Spots") },
            text = { Text("Are you sure you want to delete the ${selectedSpotIds.size} selected spot(s)? This action cannot be undone and will delete all associated images and notes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSpots(selectedSpotIds.toList()) {
                            selectedSpotIds.clear()
                            isMultiSelectMode = false
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
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
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Star toggle
                    IconButton(
                        onClick = {
                            val selectedSpots = spots.filter { it.spot.id in selectedSpotIds }
                            val anyUnstarred = selectedSpots.any { !it.spot.isStarred }
                            if (anyUnstarred) {
                                viewModel.bulkUpdateStarred(selectedSpotIds.toList(), isStarred = true) {
                                    selectedSpotIds.clear()
                                    isMultiSelectMode = false
                                    Toast.makeText(context, "Spots starred!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.bulkUpdateStarred(selectedSpotIds.toList(), isStarred = false) {
                                    selectedSpotIds.clear()
                                    isMultiSelectMode = false
                                    Toast.makeText(context, "Spots unstarred!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = selectedSpotIds.isNotEmpty()
                    ) {
                        val anyUnstarred = spots.filter { it.spot.id in selectedSpotIds }.any { !it.spot.isStarred }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Toggle Star",
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
                                contentDescription = "Export",
                                tint = if (selectedSpotIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        DropdownMenu(
                            expanded = isShareMenuExpanded,
                            onDismissRequest = { isShareMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export SpotCache Pack (.ts_pack)") },
                                onClick = {
                                    isShareMenuExpanded = false
                                    showExportOptionsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Get Route in Google Maps") },
                                onClick = {
                                    isShareMenuExpanded = false
                                    val selectedSpots = selectedSpotIds.mapNotNull { id -> spots.find { it.spot.id == id } }
                                    if (selectedSpots.isNotEmpty()) {
                                        val destinationSpot = selectedSpots.last().spot
                                        val waypointSpots = selectedSpots.dropLast(1)
                                        val base = "https://www.google.com/maps/dir/?api=1"
                                        val destParam = "&destination=${destinationSpot.latitude},${destinationSpot.longitude}"
                                        val waypointsParam = if (waypointSpots.isNotEmpty()) {
                                            "&waypoints=" + waypointSpots.joinToString("|") { "${it.spot.latitude},${it.spot.longitude}" }
                                        } else {
                                            ""
                                        }
                                        val url = base + destParam + waypointsParam
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No app available to open route.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share KML for Google My Maps") },
                                onClick = {
                                    isShareMenuExpanded = false
                                    val selectedSpots = selectedSpotIds.mapNotNull { id -> spots.find { it.spot.id == id } }
                                    if (selectedSpots.isNotEmpty()) {
                                        val kmlString = KmlExporter.generateKml(selectedSpots)
                                        try {
                                            val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                            val timestamp = sdf.format(Date())
                                            val filename = "spots_export_$timestamp.kml"
                                            val cacheFile = File(context.cacheDir, filename)
                                            cacheFile.writeText(kmlString)
                                            val authority = "${context.packageName}.fileprovider"
                                            val uri = FileProvider.getUriForFile(context, authority, cacheFile)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/vnd.google-earth.kml+xml"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "SpotCache KML Export")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share KML"))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Failed to share KML: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
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
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        placeholder = { Text("Search tags, artists, photographers...") },
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
                        isSearchExpanded = false
                        viewModel.setSearchQuery("")
                        viewModel.selectCategory("All")
                        viewModel.selectSource("All")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Collapse",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Categories Filter Header
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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

                // Sources Filter Header
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources) { source ->
                        val isSelected = selectedSource == source
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSource(source) },
                            label = { Text(source) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                labelColor = Color.Gray
                            )
                        )
                    }
                }

                // Location and Starred Filter Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeFilterCenter != null) {
                        FilterChip(
                            selected = true,
                            onClick = { showFilterBottomSheet = true },
                            label = {
                                Text(
                                    text = "${activeFilterCenter?.displayName} (${LocationUtils.getRadiusLabel(activeRadiusMeters)})"
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
                    } else {
                        FilterChip(
                            selected = false,
                            onClick = { showFilterBottomSheet = true },
                            label = { Text("+ Add Location") },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.Gray
                            )
                        )
                    }

                    FilterChip(
                        selected = showStarredOnly,
                        onClick = { viewModel.toggleShowStarredOnly() },
                        label = { Text("Starred Only") },
                        leadingIcon = {
                            if (showStarredOnly) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFFFD700)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            labelColor = Color.Gray
                        )
                    )
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
                    text = "SPOTCACHE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showFilterBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Location Filter",
                        tint = if (activeFilterCenter != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = { isSearchExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Active location and starred chips in normal mode
        if (!isSearchExpanded && (activeFilterCenter != null || showStarredOnly)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeFilterCenter != null) {
                    FilterChip(
                        selected = true,
                        onClick = { showFilterBottomSheet = true },
                        label = {
                            Text(
                                text = "${activeFilterCenter?.displayName} within ${LocationUtils.getRadiusLabel(activeRadiusMeters)}"
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

                if (showStarredOnly) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.toggleShowStarredOnly() },
                        label = { Text("Starred Only") },
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
                                    .clickable { viewModel.toggleShowStarredOnly() }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
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
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val columns = if (isLandscape) 4 else 2

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
            }
        )
    }


    if (showLimitExceededDialog) {
        AlertDialog(
            onDismissRequest = { showLimitExceededDialog = false },
            title = { Text("Starred Limit Reached") },
            text = {
                Text("Adding these spots would exceed the limit of 100 starred spots. Please unstar some spots first.")
            },
            confirmButton = {
                TextButton(onClick = { showLimitExceededDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showExportOptionsDialog) {
        var tempMinRating by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = { Text("Export Pack Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Filter images by minimum rating. Images below this rating will be excluded from the pack (the main hero image is always included).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Min Rating: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                val isStarred = i <= tempMinRating
                                Icon(
                                    imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Star $i",
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
                        0 -> "All photos (no filter)"
                        1 -> "1 Star and above"
                        2 -> "2 Stars and above"
                        3 -> "3 Stars and above"
                        4 -> "4 Stars and above"
                        5 -> "5 Stars only"
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
                        createDocumentLauncher.launch("spots_export.ts_pack")
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportOptionsDialog = false }) {
                    Text("Cancel")
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
    val latestImage = spotDetails.images.firstOrNull { it.isMain } ?: spotDetails.images.maxByOrNull { it.timestamp }
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
                    MaterialTheme.categoryColors.getColorForCategory(spotDetails.spot.category).copy(alpha = 0.4f)
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
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PAINTED OVER",
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
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val formattedDate = sdf.format(Date(spotDetails.spot.createdAt))

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
            text = "No Spots Found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (activeFilterCenter != null) {
                "No spots found within range of ${activeFilterCenter.displayName}."
            } else if (showStarredOnly) {
                "No starred spots found."
            } else if (query.isNotEmpty()) {
                "No spots match '$query'."
            } else if (category == "All") {
                "Document your city walks! Tap the 'Capture' tab below to photograph your first spot."
            } else {
                "You haven't tagged any items in the '${category.replace("_", " ")}' category yet."
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
                Text("Show All Spots")
            }
        }
    }
}
