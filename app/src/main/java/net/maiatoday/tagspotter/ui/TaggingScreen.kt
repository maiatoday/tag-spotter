package net.maiatoday.tagspotter.ui

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.ui.components.OsmMapView
import net.maiatoday.tagspotter.ui.components.OsmMarker
import net.maiatoday.tagspotter.ui.viewmodel.TaggingViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaggingScreen(
    imagePath: String,
    thumbnailPath: String,
    latitude: Double,
    longitude: Double,
    isFallback: Boolean,
    modifier: Modifier = Modifier,
    defaultCategory: String = "graffiti",
    captureTime: Long? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TagSpotterApplication
    val scope = rememberCoroutineScope()

    LaunchedEffect(imagePath) {
        if (imagePath.startsWith("content://")) {
            try {
                val uri = imagePath.toUri()
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val viewModel: TaggingViewModel = viewModel(
        factory = TaggingViewModel.provideFactory(
            repository = app.repository,
            settingsRepository = app.settingsRepository,
            initialLat = latitude,
            initialLng = longitude,
            initialCategory = if (defaultCategory == "All") "graffiti" else defaultCategory
        ),
        key = imagePath
    )

    val currentLat by viewModel.currentLat.collectAsStateWithLifecycle()
    val currentLng by viewModel.currentLng.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val photographer by viewModel.photographer.collectAsStateWithLifecycle()
    val selectedArtists by viewModel.selectedArtists.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val artistInput by viewModel.artistInput.collectAsStateWithLifecycle()
    val customTagInput by viewModel.customTagInput.collectAsStateWithLifecycle()
    val recentCustomTags by viewModel.recentCustomTags.collectAsStateWithLifecycle()

    var isMapPickerDialogVisible by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val predefinedTags = remember { setOf("mural", "stencil", "throwup", "pasteup", "sticker") }
    val categories = listOf("graffiti", "sculpture", "tree", "architecture", "public_place")

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onCancelClick = {
        val file = File(thumbnailPath)
        if (file.exists()) {
            file.delete()
        }
        onBack()
    }

    val onSaveClick: () -> Unit = {
        viewModel.saveSpot(imagePath, thumbnailPath, captureTime, onSaved = {
            scope.launch {
                Toast.makeText(context, "Spot Saved!", Toast.LENGTH_SHORT).show()
                onBack()
            }
        })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("New Urban Spot") },
                navigationIcon = {
                    IconButton(onClick = onCancelClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Visuals, location and primary actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Image Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = File(thumbnailPath),
                                    contentDescription = "Captured spot preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // GPS Warning Badge
                                if (isFallback) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "GPS Signal Weak",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Location Refinement
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Location Coordinates",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.6f, %.6f", currentLat, currentLng),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }

                                OutlinedButton(
                                    onClick = { isMapPickerDialogVisible = true },
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(Icons.Default.EditLocationAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refine")
                                }
                            }
                        }

                        // Save / Cancel actions row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.DarkGray,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = onSaveClick,
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.background
                                )
                            ) {
                                Text("Save Spot")
                            }
                        }
                    }

                    // Right Column: Form Inputs
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState())
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FormFields(
                            selectedCategory = selectedCategory,
                            onCategoryChange = { viewModel.updateCategory(it) },
                            showCategoryMenu = showCategoryMenu,
                            onShowCategoryMenuChange = { showCategoryMenu = it },
                            categories = categories,
                            description = description,
                            onDescriptionChange = { viewModel.updateDescription(it) },
                            photographer = photographer,
                            onPhotographerChange = { viewModel.updatePhotographer(it) },
                            artistInput = artistInput,
                            onArtistInputChange = { viewModel.updateArtistInput(it) },
                            selectedArtists = selectedArtists,
                            onAddArtist = { viewModel.addArtist(it) },
                            onRemoveArtist = { viewModel.removeArtist(it) },
                            predefinedTags = predefinedTags,
                            selectedTags = selectedTags,
                            onAddTag = { viewModel.addTag(it) },
                            onRemoveTag = { viewModel.removeTag(it) },
                            recentCustomTags = recentCustomTags,
                            customTagInput = customTagInput,
                            onCustomTagInputChange = { viewModel.updateCustomTagInput(it) }
                        )
                    }
                }
            } else {
                // Portrait view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 72.dp)
                ) {
                    // Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = File(thumbnailPath),
                            contentDescription = "Captured spot preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // GPS Warning Badge
                        if (isFallback) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GPS Signal Weak",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Refinement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Location Coordinates",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = String.format(Locale.US, "%.6f, %.6f", currentLat, currentLng),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        OutlinedButton(
                            onClick = { isMapPickerDialogVisible = true },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.EditLocationAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refine")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FormFields(
                        selectedCategory = selectedCategory,
                        onCategoryChange = { viewModel.updateCategory(it) },
                        showCategoryMenu = showCategoryMenu,
                        onShowCategoryMenuChange = { showCategoryMenu = it },
                        categories = categories,
                        description = description,
                        onDescriptionChange = { viewModel.updateDescription(it) },
                        photographer = photographer,
                        onPhotographerChange = { viewModel.updatePhotographer(it) },
                        artistInput = artistInput,
                        onArtistInputChange = { viewModel.updateArtistInput(it) },
                        selectedArtists = selectedArtists,
                        onAddArtist = { viewModel.addArtist(it) },
                        onRemoveArtist = { viewModel.removeArtist(it) },
                        predefinedTags = predefinedTags,
                        selectedTags = selectedTags,
                        onAddTag = { viewModel.addTag(it) },
                        onRemoveTag = { viewModel.removeTag(it) },
                        recentCustomTags = recentCustomTags,
                        customTagInput = customTagInput,
                        onCustomTagInputChange = { viewModel.updateCustomTagInput(it) }
                    )
                }

                // Floating actions at the bottom in portrait
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text("Save Spot")
                    }
                }
            }
        }
    }

    // Map Picker Dialog using OsmMapView
    if (isMapPickerDialogVisible) {
        Dialog(
            onDismissRequest = { isMapPickerDialogVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                var tempLat by remember { mutableDoubleStateOf(currentLat) }
                var tempLng by remember { mutableDoubleStateOf(currentLng) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.85f)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Set Coordinates",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Tap on the map to place the tag pin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        val mapMarker = OsmMarker(
                            id = 0L,
                            latitude = tempLat,
                            longitude = tempLng,
                            category = selectedCategory,
                            status = "active",
                            title = "New Spot Location",
                            onClick = {}
                        )

                        OsmMapView(
                            latitude = tempLat,
                            longitude = tempLng,
                            zoomLevel = 17.0,
                            markers = listOf(mapMarker),
                            modifier = Modifier.fillMaxSize(),
                            onMapClick = { gp ->
                                tempLat = gp.latitude
                                tempLng = gp.longitude
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { isMapPickerDialogVisible = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray
                            )
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                viewModel.updateCoordinates(tempLat, tempLng)
                                isMapPickerDialogVisible = false
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.background
                            )
                        ) {
                            Text("Confirm Location")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormFields(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    showCategoryMenu: Boolean,
    onShowCategoryMenuChange: (Boolean) -> Unit,
    categories: List<String>,
    description: String,
    onDescriptionChange: (String) -> Unit,
    photographer: String,
    onPhotographerChange: (String) -> Unit,
    artistInput: String,
    onArtistInputChange: (String) -> Unit,
    selectedArtists: List<String>,
    onAddArtist: (String) -> Unit,
    onRemoveArtist: (String) -> Unit,
    predefinedTags: Set<String>,
    selectedTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    recentCustomTags: List<String>,
    customTagInput: String,
    onCustomTagInputChange: (String) -> Unit
) {
    // Category Selection Dropdown
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedCategory.replace("_", " ").replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .clickable { onShowCategoryMenuChange(!showCategoryMenu) }
                        .padding(8.dp)
                ) {
                    Text("▼", color = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        DropdownMenu(
            expanded = showCategoryMenu,
            onDismissRequest = { onShowCategoryMenuChange(false) },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onCategoryChange(category)
                        onShowCategoryMenuChange(false)
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Description Input
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Description / Details") },
        placeholder = { Text("e.g. Artist info, style, notes...") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Photographer Input
    OutlinedTextField(
        value = photographer,
        onValueChange = onPhotographerChange,
        label = { Text("Photographer") },
        placeholder = { Text("e.g. Jane Doe") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Artists Input Section
    Text(
        text = "Artists / Writers",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = artistInput,
            onValueChange = onArtistInputChange,
            label = { Text("Add Artist / Writer") },
            placeholder = { Text("e.g. Banksy, Retna, Cope2...") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val cleaned = artistInput.trim()
                    if (cleaned.isNotEmpty()) {
                        onAddArtist(cleaned)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = MaterialTheme.colorScheme.tertiary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                val cleaned = artistInput.trim()
                if (cleaned.isNotEmpty()) {
                    onAddArtist(cleaned)
                }
            },
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add artist",
                tint = MaterialTheme.colorScheme.background
            )
        }
    }

    if (selectedArtists.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedArtists.forEach { artist ->
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artist,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove artist",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRemoveArtist(artist) }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Predefined Quick Tags
    Text(
        text = "Quick Select Tags",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        predefinedTags.forEach { tag ->
            val isSelected = selectedTags.contains(tag)
            InputChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) onRemoveTag(tag) else onAddTag(tag)
                },
                label = { Text("#$tag") },
                colors = InputChipDefaults.inputChipColors(
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    labelColor = Color.Gray,
                    containerColor = Color.Transparent
                ),
                border = InputChipDefaults.inputChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Color.Transparent else Color.Gray,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }

    // Dynamic Recent Custom Tags
    if (recentCustomTags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Recent Custom Tags",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            recentCustomTags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                InputChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) onRemoveTag(tag) else onAddTag(tag)
                    },
                    label = { Text("#$tag") },
                    colors = InputChipDefaults.inputChipColors(
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        labelColor = Color.LightGray,
                        containerColor = Color.Transparent
                    ),
                    border = InputChipDefaults.inputChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color.Transparent else Color.DarkGray,
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Add Custom Tags Input
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = customTagInput,
            onValueChange = onCustomTagInputChange,
            label = { Text("Add Custom Tag") },
            placeholder = { Text("e.g. stencil") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val cleaned = customTagInput.trim().lowercase().removePrefix("#")
                    if (cleaned.isNotEmpty()) {
                        onAddTag(cleaned)
                        onCustomTagInputChange("")
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = MaterialTheme.colorScheme.secondary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                val cleaned = customTagInput.trim().lowercase().removePrefix("#")
                if (cleaned.isNotEmpty()) {
                    onAddTag(cleaned)
                    onCustomTagInputChange("")
                }
            },
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add custom tag",
                tint = MaterialTheme.colorScheme.background
            )
        }
    }

    // Display current selected tags
    if (selectedTags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .background(Color.DarkGray, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#$tag",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove tag",
                        tint = Color.LightGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRemoveTag(tag) }
                    )
                }
            }
        }
    }
}
