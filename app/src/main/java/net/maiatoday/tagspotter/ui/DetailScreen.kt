package net.maiatoday.tagspotter.ui

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotImage
import net.maiatoday.tagspotter.data.SpotNote
import net.maiatoday.tagspotter.theme.categoryColors
import net.maiatoday.tagspotter.ui.components.OsmMapView
import net.maiatoday.tagspotter.ui.components.OsmMarker
import net.maiatoday.tagspotter.ui.screens.ZoomableImageOverlay
import net.maiatoday.tagspotter.ui.viewmodel.AiState
import net.maiatoday.tagspotter.ui.viewmodel.DetailViewModel
import net.maiatoday.tagspotter.utils.ImageOptimizer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.AutoAwesome
import net.maiatoday.tagspotter.ui.viewmodel.AiSuggestion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    spotId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    draftImagePath: String? = null,
    draftThumbnailPath: String? = null,
    draftLatitude: Double? = null,
    draftLongitude: Double? = null,
    draftIsFallback: Boolean = false,
    draftDefaultCategory: String = "graffiti",
    draftCaptureTime: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as TagSpotterApplication
    val scope = rememberCoroutineScope()
    val isCreationMode = spotId == -1L

    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.provideFactory(
            spotId = spotId,
            repository = app.repository,
            settingsRepository = app.settingsRepository,
            draftImagePath = draftImagePath,
            draftThumbnailPath = draftThumbnailPath,
            draftLatitude = draftLatitude,
            draftLongitude = draftLongitude,
            draftCategory = draftDefaultCategory,
            draftCaptureTime = draftCaptureTime
        ),
        key = if (isCreationMode) draftImagePath ?: "new_spot" else spotId.toString()
    )

    val spotDetails by viewModel.spotDetails.collectAsStateWithLifecycle()
    val sortedImages = remember(spotDetails) { spotDetails?.images?.sortedBy { it.timestamp } ?: emptyList() }
    val defaultPhotographer by viewModel.defaultPhotographer.collectAsStateWithLifecycle()
    val recentCustomTags by viewModel.recentCustomTags.collectAsStateWithLifecycle()

    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val isArtistRecognitionEnabled by viewModel.isArtistRecognitionEnabled.collectAsStateWithLifecycle()

    val mainImage = sortedImages.firstOrNull { it.isMain } ?: sortedImages.firstOrNull()
    val mainImagePath = mainImage?.imagePath ?: ""

    val onSearchImage: () -> Unit = {
        if (mainImagePath.isNotEmpty()) {
            try {
                val file = File(mainImagePath)
                if (file.exists()) {
                    val authority = "${context.packageName}.fileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Search Artist"))
                } else {
                    Toast.makeText(context, "Image file not found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No image available to search.", Toast.LENGTH_SHORT).show()
        }
    }

    val onIdentifyArtist: () -> Unit = {
        if (mainImagePath.isNotEmpty()) {
            viewModel.identifyArtist(mainImagePath, context)
        } else {
            Toast.makeText(context, "No image available to analyze.", Toast.LENGTH_SHORT).show()
        }
    }

    // AI Suggestions Dialog
    if (aiState is AiState.Success) {
        val suggestion = (aiState as AiState.Success).suggestion
        var importArtist by remember { mutableStateOf(suggestion.artist != null) }
        var importTitle by remember { mutableStateOf(suggestion.title != null) }
        var importTags by remember { mutableStateOf(suggestion.tags.isNotEmpty()) }

        AlertDialog(
            onDismissRequest = { viewModel.resetAiState() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Recognition Suggestions")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Gemini identified the following details from your photo. Select which ones to apply:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    if (suggestion.artist != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importArtist = !importArtist }
                        ) {
                            Checkbox(
                                checked = importArtist,
                                onCheckedChange = { importArtist = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Artist / Crew", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(suggestion.artist, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (suggestion.title != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importTitle = !importTitle }
                        ) {
                            Checkbox(
                                checked = importTitle,
                                onCheckedChange = { importTitle = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Suggested Title", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(suggestion.title, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (suggestion.tags.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importTags = !importTags }
                        ) {
                            Checkbox(
                                checked = importTags,
                                onCheckedChange = { importTags = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Suggested Tags", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    suggestion.tags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importArtist && suggestion.artist != null) {
                            viewModel.updateArtists(listOf(suggestion.artist))
                        }
                        if (importTitle && suggestion.title != null) {
                            viewModel.updateDescription(suggestion.title)
                        }
                        if (importTags && suggestion.tags.isNotEmpty()) {
                            val currentTags = spotDetails?.spot?.tags ?: emptyList()
                            val merged = (currentTags + suggestion.tags).distinct()
                            viewModel.updateTags(merged)
                        }
                        viewModel.resetAiState()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Apply Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetAiState() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Error Dialog
    if (aiState is AiState.Error) {
        val error = aiState as AiState.Error
        val (title, message) = when (error) {
            is AiState.Error.MissingKey -> {
                "Gemini API Key Missing" to "A Gemini API Key is required for in-app recognition. Please configure it in Settings."
            }
            is AiState.Error.InvalidKey -> {
                "Invalid API Key" to "The configured Gemini API Key is invalid or unauthorized. Please verify it in Settings."
            }
            is AiState.Error.QuotaExceeded -> {
                "API Quota Exceeded" to "You have exceeded your Gemini API limit. Please wait a while before trying again."
            }
            is AiState.Error.SafetyBlocked -> {
                "Safety Blocked" to "The image was flagged by Gemini's safety filters and could not be analyzed."
            }
            is AiState.Error.Generic -> {
                "Recognition Error" to error.message
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.resetAiState() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title)
                }
            },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetAiState() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }

    var noteInput by remember { mutableStateOf("") }
    var zoomImage by remember { mutableStateOf<SpotImage?>(null) }
    var isMapPickerDialogVisible by remember { mutableStateOf(false) }

    var showPermissionDisclosure by remember { mutableStateOf(false) }
    var showLimitExceededDialog by remember { mutableStateOf(false) }

    // Collect UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                DetailViewModel.UiEvent.StarLimitExceeded -> {
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
            viewModel.toggleStarred()
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

    // Launcher to add another image to this spot
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            scope.launch(Dispatchers.Default) {
                val thumbnailPath = ImageOptimizer.createThumbnail(context, uri)
                if (thumbnailPath != null) {
                    viewModel.addImage(uri.toString(), thumbnailPath, System.currentTimeMillis())
                }
            }
        }
    }

    val onCancelClick = {
        if (isCreationMode && draftThumbnailPath != null) {
            val file = File(draftThumbnailPath)
            if (file.exists()) {
                file.delete()
            }
        }
        onBack()
    }

    val onSaveClick: () -> Unit = {
        viewModel.saveSpot(onSaved = { _ ->
            scope.launch {
                Toast.makeText(context, "Spot Saved!", Toast.LENGTH_SHORT).show()
                onBack()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isCreationMode) "NEW SPOT" else "SPOT DETAILS",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isCreationMode) {
                        val details = spotDetails
                        if (details != null) {
                            val isStarred = details.spot.isStarred
                            IconButton(
                                onClick = {
                                    if (isStarred) {
                                        viewModel.toggleStarred()
                                    } else {
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
                                            viewModel.toggleStarred()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (isStarred) "Unstar Spot" else "Star Spot",
                                    tint = if (isStarred) Color(0xFFFFD700) else Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val details = spotDetails
                                if (details != null) {
                                    viewModel.deleteSpot(details, onDeleted = {
                                        scope.launch(Dispatchers.Main) {
                                            Toast.makeText(context, "Spot deleted successfully", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                    })
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Spot", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                )
            )
        }
    ) { innerPadding ->
        val details = spotDetails
        if (details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading spot details...", color = Color.Gray)
            }
            return@Scaffold
        }

        val sortedNotes = details.notes.sortedBy { it.timestamp }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane: Photo Hero and Timeline
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val mainImage = sortedImages.firstOrNull { it.isMain } ?: sortedImages.firstOrNull()
                    if (mainImage != null) {
                        DetailHeroSection(
                            imagePath = mainImage.imagePath,
                            thumbnailPath = mainImage.thumbnailPath,
                            status = details.spot.status,
                            isFallback = if (isCreationMode) draftIsFallback else details.spot.isImported,
                            onImageClick = { zoomImage = mainImage }
                        )
                    }

                    if (!isCreationMode) {
                        DetailPhotoTimeline(
                            sortedImages = sortedImages,
                            onAddPhotoClick = {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onImageClick = { zoomImage = it },
                            onHeartClick = { viewModel.setMainImage(it.id) }
                        )
                    } else {
                        // Actions row in landscape creation mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Save Spot")
                            }
                        }
                    }
                }

                // Right Pane: Info Bento Card, Map and Notes Section
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailMetadataCard(
                        details = details,
                        isCreationMode = isCreationMode,
                        defaultPhotographer = defaultPhotographer,
                        isArtistRecognitionEnabled = isArtistRecognitionEnabled,
                        aiState = aiState,
                        hasImage = mainImagePath.isNotEmpty(),
                        onSearchImage = onSearchImage,
                        onIdentifyArtist = onIdentifyArtist,
                        onUpdateStatus = { viewModel.updateStatus(it) },
                        onUpdateCategory = { viewModel.updateCategory(it) },
                        onUpdateArtists = { viewModel.updateArtists(it) },
                        onUpdatePhotographer = { viewModel.updatePhotographer(it) },
                        onUpdateDescription = { viewModel.updateDescription(it) },
                        onMapPickerClick = { isMapPickerDialogVisible = true }
                    )

                    DetailTagsCard(
                        details = details,
                        isCreationMode = isCreationMode,
                        recentCustomTags = recentCustomTags,
                        onUpdateTags = { viewModel.updateTags(it) }
                    )

                    DetailMiniMapCard(
                        latitude = details.spot.latitude,
                        longitude = details.spot.longitude,
                        category = details.spot.category
                    )

                    if (!isCreationMode) {
                        DetailNotesSection(
                            sortedNotes = sortedNotes,
                            noteInput = noteInput,
                            onNoteInputChange = { noteInput = it },
                            onSendNote = {
                                val noteText = noteInput.trim()
                                if (noteText.isNotEmpty()) {
                                    viewModel.addNote(noteText, System.currentTimeMillis())
                                    noteInput = ""
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Portrait view
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = if (isCreationMode) 80.dp else 16.dp)
                ) {
                    val mainImage = sortedImages.firstOrNull { it.isMain } ?: sortedImages.firstOrNull()
                    if (mainImage != null) {
                        DetailHeroSection(
                            imagePath = mainImage.imagePath,
                            thumbnailPath = mainImage.thumbnailPath,
                            status = details.spot.status,
                            isFallback = if (isCreationMode) draftIsFallback else details.spot.isImported,
                            onImageClick = { zoomImage = mainImage }
                        )
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailMetadataCard(
                            details = details,
                            isCreationMode = isCreationMode,
                            defaultPhotographer = defaultPhotographer,
                            isArtistRecognitionEnabled = isArtistRecognitionEnabled,
                            aiState = aiState,
                            hasImage = mainImagePath.isNotEmpty(),
                            onSearchImage = onSearchImage,
                            onIdentifyArtist = onIdentifyArtist,
                            onUpdateStatus = { viewModel.updateStatus(it) },
                            onUpdateCategory = { viewModel.updateCategory(it) },
                            onUpdateArtists = { viewModel.updateArtists(it) },
                            onUpdatePhotographer = { viewModel.updatePhotographer(it) },
                            onUpdateDescription = { viewModel.updateDescription(it) },
                            onMapPickerClick = { isMapPickerDialogVisible = true }
                        )

                        DetailTagsCard(
                            details = details,
                            isCreationMode = isCreationMode,
                            recentCustomTags = recentCustomTags,
                            onUpdateTags = { viewModel.updateTags(it) }
                        )

                        DetailMiniMapCard(
                            latitude = details.spot.latitude,
                            longitude = details.spot.longitude,
                            category = details.spot.category
                        )

                        if (!isCreationMode) {
                            DetailPhotoTimeline(
                                sortedImages = sortedImages,
                                onAddPhotoClick = {
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onImageClick = { zoomImage = it },
                                onHeartClick = { viewModel.setMainImage(it.id) }
                            )

                            DetailNotesSection(
                                sortedNotes = sortedNotes,
                                noteInput = noteInput,
                                onNoteInputChange = { noteInput = it },
                                onSendNote = {
                                    val noteText = noteInput.trim()
                                    if (noteText.isNotEmpty()) {
                                        viewModel.addNote(noteText, System.currentTimeMillis())
                                        noteInput = ""
                                    }
                                }
                            )
                        }
                    }
                }

                // Floating Action row at the bottom for Portrait Creation Mode
                if (isCreationMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            )
                            .padding(16.dp),
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
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Save Spot")
                        }
                    }
                }
            }
        }
    }

    if (zoomImage != null) {
        val initialIndex = sortedImages.indexOfFirst { it.id == zoomImage?.id }.coerceAtLeast(0)
        val pagerState = rememberPagerState(initialPage = initialIndex) { sortedImages.size }
        var currentScale by remember { mutableFloatStateOf(1f) }

        LaunchedEffect(pagerState.currentPage) {
            currentScale = 1f
        }

        Dialog(
            onDismissRequest = { zoomImage = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentScale == 1f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val image = sortedImages[page]
                ZoomableImageOverlay(
                    imagePath = image.imagePath,
                    thumbnailPath = image.thumbnailPath,
                    onClose = { zoomImage = null },
                    onScaleChanged = { scale ->
                        if (pagerState.currentPage == page) {
                            currentScale = scale
                        }
                    }
                )
            }
        }
    }

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
                var tempLat by remember { mutableDoubleStateOf(spotDetails?.spot?.latitude ?: 0.0) }
                var tempLng by remember { mutableDoubleStateOf(spotDetails?.spot?.longitude ?: 0.0) }

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
                        text = "Update Coordinates",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Tap on the map to move the tag pin.",
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
                            category = spotDetails?.spot?.category ?: "graffiti",
                            status = "active",
                            title = "Spot Location",
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
                                viewModel.updateLocation(tempLat, tempLng)
                                isMapPickerDialogVisible = false
                                Toast.makeText(context, "Location Updated!", Toast.LENGTH_SHORT).show()
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
                            viewModel.toggleStarred()
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
                Text("You can only star up to 100 spots due to geofencing limits. Please unstar some spots first.")
            },
            confirmButton = {
                TextButton(onClick = { showLimitExceededDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun DetailHeroSection(
    imagePath: String,
    thumbnailPath: String,
    status: String,
    isFallback: Boolean,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(Color.Black)
    ) {
        val imageModel = remember(imagePath, thumbnailPath) {
            if (thumbnailPath.isNotEmpty() && !thumbnailPath.startsWith("android.resource://") && !thumbnailPath.startsWith("http")) {
                File(thumbnailPath)
            } else if (thumbnailPath.isNotEmpty() && (thumbnailPath.startsWith("android.resource://") || thumbnailPath.startsWith("http"))) {
                thumbnailPath.toUri()
            } else if (imagePath.startsWith("content://") || imagePath.startsWith("android.resource://") || imagePath.startsWith("http")) {
                imagePath.toUri()
            } else {
                File(imagePath)
            }
        }
        AsyncImage(
            model = imageModel,
            contentDescription = "Spot Hero Image",
            modifier = Modifier
                .fillMaxSize()
                .clickable { onImageClick() },
            contentScale = ContentScale.Crop
        )

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )

        // Floating Badges at bottom left
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Tag
            Box(
                modifier = Modifier
                    .background(
                        color = if (status == "erased") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = (if (status == "erased") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (status == "erased") "Painted Over" else "Active Spot",
                    color = if (status == "erased") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // GPS Signal / Verified Tag
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isFallback) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "GPS Signal Weak",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Text(
                            text = "Verified GPS",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailMetadataCard(
    details: SpotDetails,
    isCreationMode: Boolean,
    defaultPhotographer: String,
    isArtistRecognitionEnabled: Boolean,
    aiState: AiState,
    hasImage: Boolean,
    onSearchImage: () -> Unit,
    onIdentifyArtist: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateArtists: (List<String>) -> Unit,
    onUpdatePhotographer: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onMapPickerClick: () -> Unit
) {
    var isEditingArtists by remember { mutableStateOf(isCreationMode) }
    var artistEditInput by remember { mutableStateOf("") }
    val localArtistsList = remember { mutableStateListOf<String>() }

    var isEditingPhotographer by remember { mutableStateOf(isCreationMode) }
    var photographerEditInput by remember { mutableStateOf(details.spot.photographer) }

    var isEditingDescription by remember { mutableStateOf(isCreationMode) }
    var descriptionEditInput by remember { mutableStateOf(details.spot.description) }

    LaunchedEffect(isEditingArtists, details.spot.artists) {
        if (isEditingArtists) {
            localArtistsList.clear()
            localArtistsList.addAll(details.spot.artists)
        }
    }

    LaunchedEffect(isEditingPhotographer, details.spot.photographer) {
        if (isEditingPhotographer) {
            photographerEditInput = details.spot.photographer
        }
    }

    LaunchedEffect(isEditingDescription, details.spot.description) {
        if (isEditingDescription) {
            descriptionEditInput = details.spot.description
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category Badge & Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.categoryColors.getColorForCategory(details.spot.category),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { isCategoryDropdownExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = details.spot.category.replace("_", " ").uppercase(),
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    DropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        val categories = listOf("graffiti", "sculpture", "nature", "architecture", "public_place")
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.replace("_", " ").uppercase()) },
                                onClick = {
                                    onUpdateCategory(category)
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isCreationMode) {
                    Text(
                        text = "NEW SPOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val isErased = details.spot.status == "erased"
                    TextButton(
                        onClick = {
                            val newStatus = if (isErased) "active" else "erased"
                            onUpdateStatus(newStatus)
                        }
                    ) {
                        Text(
                            text = if (isErased) "Mark Active" else "Mark as Painted Over",
                            color = if (isErased) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Section (spot.description)
            if (isEditingDescription) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TITLE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        if (!isCreationMode) {
                            Row {
                                IconButton(
                                    onClick = {
                                        onUpdateDescription(descriptionEditInput.trim())
                                        isEditingDescription = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save title",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isEditingDescription = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel edit",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descriptionEditInput,
                        onValueChange = { 
                            descriptionEditInput = it 
                            if (isCreationMode) onUpdateDescription(it)
                        },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. Neon face stencil near Duomo") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TITLE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isEditingDescription = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit title",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (details.spot.description.isEmpty()) "\"No title logged.\"" else "\"${details.spot.description}\"",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = MaterialTheme.typography.displayMedium.fontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Artist Section
            if (isEditingArtists) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ARTIST / CREW",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )

                            if (isArtistRecognitionEnabled && hasImage) {
                                if (aiState is AiState.Identifying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                } else {
                                    IconButton(
                                        onClick = onSearchImage,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ImageSearch,
                                            contentDescription = "Search artist with Lens",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onIdentifyArtist,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Identify artist with AI",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isCreationMode) {
                            Row {
                                IconButton(
                                    onClick = {
                                        val cleaned = artistEditInput.trim()
                                        val finalArtists = if (cleaned.isNotEmpty() && !localArtistsList.contains(cleaned)) {
                                            localArtistsList.toList() + cleaned
                                        } else {
                                            localArtistsList.toList()
                                        }
                                        onUpdateArtists(finalArtists)
                                        artistEditInput = ""
                                        isEditingArtists = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save artists",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isEditingArtists = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel edit",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = artistEditInput,
                            onValueChange = { artistEditInput = it },
                            label = { Text("Artist Name") },
                            placeholder = { Text("e.g. Banksy") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val cleaned = artistEditInput.trim()
                                    if (cleaned.isNotEmpty()) {
                                        if (!localArtistsList.contains(cleaned)) {
                                            localArtistsList.add(cleaned)
                                            onUpdateArtists(localArtistsList.toList())
                                        }
                                        artistEditInput = ""
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
                                val cleaned = artistEditInput.trim()
                                if (cleaned.isNotEmpty()) {
                                    if (!localArtistsList.contains(cleaned)) {
                                        localArtistsList.add(cleaned)
                                        onUpdateArtists(localArtistsList.toList())
                                    }
                                    artistEditInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add artist",
                                tint = MaterialTheme.colorScheme.background
                            )
                        }
                    }

                    if (localArtistsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            localArtistsList.forEach { artist ->
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
                                            .clickable { 
                                                localArtistsList.remove(artist)
                                                onUpdateArtists(localArtistsList.toList())
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ARTIST / CREW",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isEditingArtists = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit artists",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (details.spot.artists.isEmpty()) {
                        Text(
                            text = "Unknown Artist",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = details.spot.artists.joinToString(", "),
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logged Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sdf = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())
                Column {
                    Text(
                        text = "LOGGED DATE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sdf.format(Date(details.spot.createdAt)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photographer Section
            if (isEditingPhotographer) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PHOTOGRAPHER",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        if (!isCreationMode) {
                            Row {
                                IconButton(
                                    onClick = {
                                        onUpdatePhotographer(photographerEditInput.trim())
                                        isEditingPhotographer = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save photographer",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isEditingPhotographer = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel edit",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = photographerEditInput,
                        onValueChange = { 
                            photographerEditInput = it 
                            if (isCreationMode) onUpdatePhotographer(it)
                        },
                        label = { Text("Photographer Name") },
                        placeholder = { Text("e.g. Jane Doe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    if (defaultPhotographer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { 
                                    photographerEditInput = defaultPhotographer
                                    onUpdatePhotographer(defaultPhotographer)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Use Profile Name ($defaultPhotographer)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PHOTOGRAPHER",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isEditingPhotographer = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit photographer",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.photographer.ifEmpty { "Unknown Photographer" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location coordinates & "Map It" or "Refine" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "LOCATION COORDINATES",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.6f° N, %.6f° W", details.spot.latitude, details.spot.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onMapPickerClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = if (isCreationMode) Icons.Default.EditLocationAlt else Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCreationMode) "REFINE" else "MAP IT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailTagsCard(
    details: SpotDetails,
    isCreationMode: Boolean,
    recentCustomTags: List<String>,
    onUpdateTags: (List<String>) -> Unit
) {
    var isEditingTags by remember { mutableStateOf(isCreationMode) }
    var customTagEditInput by remember { mutableStateOf("") }
    val localTagsList = remember { mutableStateListOf<String>() }

    LaunchedEffect(isEditingTags, details.spot.tags) {
        if (isEditingTags) {
            localTagsList.clear()
            localTagsList.addAll(details.spot.tags)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tags section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TAGS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                if (!isCreationMode) {
                    IconButton(
                        onClick = { isEditingTags = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit tags",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isEditingTags) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isCreationMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    val cleaned = customTagEditInput.trim().lowercase().removePrefix("#")
                                    val finalTags = if (cleaned.isNotEmpty() && !localTagsList.contains(cleaned)) {
                                        localTagsList.toList() + cleaned
                                    } else {
                                        localTagsList.toList()
                                    }
                                    onUpdateTags(finalTags)
                                    customTagEditInput = ""
                                    isEditingTags = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save tags",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { isEditingTags = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel edit",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Predefined Quick Tags
                    Text(
                        text = "Quick Select Tags",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val predefinedTags = remember { setOf("mural", "stencil", "throwup", "pasteup", "sticker") }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        predefinedTags.forEach { tag ->
                            val isSelected = localTagsList.contains(tag)
                            InputChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        localTagsList.remove(tag)
                                    } else {
                                        localTagsList.add(tag)
                                    }
                                    onUpdateTags(localTagsList.toList())
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

                    // Recent Custom Tags
                    val recentTagsToShow = recentCustomTags.filter { !predefinedTags.contains(it) }
                    if (recentTagsToShow.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recent Custom Tags",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            recentTagsToShow.forEach { tag ->
                                val isSelected = localTagsList.contains(tag)
                                InputChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            localTagsList.remove(tag)
                                        } else {
                                            localTagsList.add(tag)
                                        }
                                        onUpdateTags(localTagsList.toList())
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add Custom Tag Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customTagEditInput,
                            onValueChange = { customTagEditInput = it },
                            label = { Text("Add Custom Tag") },
                            placeholder = { Text("e.g. pasteup") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val cleaned = customTagEditInput.trim().lowercase().removePrefix("#")
                                    if (cleaned.isNotEmpty()) {
                                        if (!localTagsList.contains(cleaned)) {
                                            localTagsList.add(cleaned)
                                            onUpdateTags(localTagsList.toList())
                                        }
                                        customTagEditInput = ""
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
                                val cleaned = customTagEditInput.trim().lowercase().removePrefix("#")
                                if (cleaned.isNotEmpty()) {
                                    if (!localTagsList.contains(cleaned)) {
                                        localTagsList.add(cleaned)
                                        onUpdateTags(localTagsList.toList())
                                    }
                                    customTagEditInput = ""
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
                }
            } else {
                if (details.spot.tags.isEmpty()) {
                    Text(
                        text = "No tags added.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        details.spot.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#$tag".uppercase(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailMiniMapCard(
    latitude: Double,
    longitude: Double,
    category: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        val mapMarker = OsmMarker(
            id = 0L,
            latitude = latitude,
            longitude = longitude,
            category = category,
            status = "active",
            title = "Spot Location",
            onClick = {}
        )

        OsmMapView(
            latitude = latitude,
            longitude = longitude,
            zoomLevel = 17.0,
            markers = listOf(mapMarker),
            modifier = Modifier.fillMaxSize(),
            onMapClick = {}
        )
    }
}

@Composable
fun SpotTimelineCard(
    image: SpotImage,
    isMain: Boolean,
    onHeartClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(180.dp)
            .clickable { onClick() }
            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            val imageModel = remember(image.imagePath, image.thumbnailPath) {
                if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("android.resource://") && !image.thumbnailPath.startsWith("http")) {
                    File(image.thumbnailPath)
                } else if (image.thumbnailPath.isNotEmpty() && (image.thumbnailPath.startsWith("android.resource://") || image.thumbnailPath.startsWith("http"))) {
                    image.thumbnailPath.toUri()
                } else if (image.imagePath.startsWith("content://") || image.imagePath.startsWith("android.resource://") || image.imagePath.startsWith("http")) {
                    image.imagePath.toUri()
                } else {
                    File(image.imagePath)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onHeartClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMain) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Main thumbnail",
                        tint = if (isMain) Color(0xFFF43F5E) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(image.timestamp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DetailPhotoTimeline(
    sortedImages: List<SpotImage>,
    onAddPhotoClick: () -> Unit,
    onImageClick: (SpotImage) -> Unit,
    onHeartClick: (SpotImage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PHOTO TIMELINE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onAddPhotoClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add image",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(end = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedImages, key = { it.id }) { image ->
                SpotTimelineCard(
                    image = image,
                    isMain = image.isMain,
                    onHeartClick = { onHeartClick(image) },
                    onClick = { onImageClick(image) }
                )
            }
        }
    }
}

@Composable
private fun DetailNotesSection(
    sortedNotes: List<SpotNote>,
    noteInput: String,
    onNoteInputChange: (String) -> Unit,
    onSendNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FIELD NOTES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sortedNotes.isEmpty()) {
            Text(
                text = "No notes written at this spot yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            sortedNotes.forEach { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                        Text(
                            text = sdf.format(Date(note.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.noteText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = noteInput,
                onValueChange = onNoteInputChange,
                label = { Text("Write a note...") },
                placeholder = { Text("e.g. Tag faded, style details...") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendNote() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendNote,
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send note",
                    tint = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}
