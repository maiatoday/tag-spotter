package net.maiatoday.tagspotter.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import net.maiatoday.tagspotter.theme.categoryColors
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
import net.maiatoday.tagspotter.utils.PackManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun GalleryScreen(
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val context = LocalContext.current
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val spots by viewModel.spots.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedSpotIds = remember { mutableStateListOf<Long>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showPermissionDisclosure by remember { mutableStateOf(false) }
    var showLimitExceededDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                GalleryViewModel.UiEvent.StarLimitExceeded -> {
                    showLimitExceededDialog = true
                }
            }
        }
    }

    val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val hasBackgroundLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.bulkUpdateStarred(selectedSpotIds.toList(), isStarred = true) {
                selectedSpotIds.clear()
                isMultiSelectMode = false
                Toast.makeText(context, "Spots starred!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Background location permission is required for proximity alerts.", Toast.LENGTH_LONG).show()
        }
    }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            showPermissionDisclosure = true
        } else {
            Toast.makeText(context, "Location permission is required for starred spots geofencing.", Toast.LENGTH_LONG).show()
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            val selectedSpots = spots.filter { it.spot.id in selectedSpotIds }
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    PackManager.exportPack(context, selectedSpots, outputStream)
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

    val categories = listOf("All", "graffiti", "sculpture", "tree", "architecture", "public_place")
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
                                if (!hasFineLocation) {
                                    val permissions = mutableListOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    foregroundLocationLauncher.launch(permissions.toTypedArray())
                                } else if (!hasBackgroundLocation) {
                                    showPermissionDisclosure = true
                                } else {
                                    viewModel.bulkUpdateStarred(selectedSpotIds.toList(), isStarred = true) {
                                        selectedSpotIds.clear()
                                        isMultiSelectMode = false
                                        Toast.makeText(context, "Spots starred!", Toast.LENGTH_SHORT).show()
                                    }
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

                    IconButton(
                        onClick = {
                            createDocumentLauncher.launch("spots_export.ts_pack")
                        },
                        enabled = selectedSpotIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export",
                            tint = if (selectedSpotIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
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

        if (spots.isEmpty()) {
            EmptyGalleryState(category = selectedCategory, query = searchQuery)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    spots.forEach { spotDetails ->
                        val isSelected = selectedSpotIds.contains(spotDetails.spot.id)
                        SpotGridCard(
                            modifier = Modifier.width(170.dp),
                            spotDetails = spotDetails,
                            isSelected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
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
    }
    if (showPermissionDisclosure) {
        AlertDialog(
            onDismissRequest = { showPermissionDisclosure = false },
            title = { Text("Background Location Access") },
            text = {
                Text("Tag Spotter needs background location access ('Allow all the time') to monitor starred spots and notify you when walking near them, even when the app is closed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDisclosure = false
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            viewModel.bulkUpdateStarred(selectedSpotIds.toList(), isStarred = true) {
                                selectedSpotIds.clear()
                                isMultiSelectMode = false
                                Toast.makeText(context, "Spots starred!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDisclosure = false }) {
                    Text("Cancel")
                }
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
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SpotGridCard(
    spotDetails: SpotDetails,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get latest image based on timestamp
    val latestImage = spotDetails.images.maxByOrNull { it.timestamp }
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
                                shape = androidx.compose.foundation.shape.CircleShape
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
fun EmptyGalleryState(category: String, query: String = "") {
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
            text = if (query.isNotEmpty()) {
                "No spots match '$query'."
            } else if (category == "All") {
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
