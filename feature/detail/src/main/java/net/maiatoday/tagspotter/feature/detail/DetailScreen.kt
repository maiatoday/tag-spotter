package net.maiatoday.tagspotter.feature.detail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.model.getCategoryCreatorLabel
import net.maiatoday.tagspotter.core.ui.theme.categoryColors
import net.maiatoday.tagspotter.core.ui.OsmMapView
import net.maiatoday.tagspotter.core.ui.OsmMarker
import net.maiatoday.tagspotter.core.photo.ImageOptimizer
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val scope = rememberCoroutineScope()
    val isCreationMode = spotId == -1L

    val viewModel: DetailViewModel = koinViewModel(
        key = if (isCreationMode) draftImagePath ?: "new_spot" else spotId.toString(),
        parameters = {
            parametersOf(
                spotId,
                SpotDraftParams(
                    imagePath = draftImagePath,
                    thumbnailPath = draftThumbnailPath,
                    latitude = draftLatitude,
                    longitude = draftLongitude,
                    category = draftDefaultCategory,
                    captureTime = draftCaptureTime
                )
            )
        }
    )

    val spotDetails by viewModel.spotDetails.collectAsStateWithLifecycle()
    val sortedImages = remember(spotDetails) { spotDetails?.images?.sortedBy { it.timestamp } ?: emptyList() }
    val defaultPhotographer by viewModel.defaultPhotographer.collectAsStateWithLifecycle()
    val recentCustomTags by viewModel.recentCustomTags.collectAsStateWithLifecycle()

    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val isArtistRecognitionEnabled by viewModel.isArtistRecognitionEnabled.collectAsStateWithLifecycle()
    val wikiSearchState by viewModel.wikiSearchState.collectAsStateWithLifecycle()
    val darkMapEnabled by viewModel.darkMapEnabled.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val useDarkMap = isSystemDark && darkMapEnabled

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
            viewModel.identifyArtist(mainImagePath)
        } else {
            Toast.makeText(context, "No image available to analyze.", Toast.LENGTH_SHORT).show()
        }
    }

    // AI Suggestions Dialog
    if (aiState is AiState.Success) {
        val suggestion = (aiState as AiState.Success).suggestion
        val suggestionArtist = suggestion.artist
        val suggestionTitle = suggestion.title

        val currentArtist = spotDetails?.spot?.artists?.joinToString(", ") ?: ""
        val currentTitle = spotDetails?.spot?.description ?: ""
        val isArtistEmpty = currentArtist.isBlank()
        val isTitleEmpty = currentTitle.isBlank()
        
        val artistMatches = !isArtistEmpty && suggestionArtist?.trim()?.equals(currentArtist.trim(), ignoreCase = true) == true
        val titleMatches = !isTitleEmpty && suggestionTitle?.trim()?.equals(currentTitle.trim(), ignoreCase = true) == true
        
        val shouldProposeArtist = suggestionArtist != null && (isArtistEmpty || !artistMatches)
        val shouldProposeTitle = suggestionTitle != null && (isTitleEmpty || !titleMatches)

        var importArtist by remember(shouldProposeArtist) { mutableStateOf(shouldProposeArtist) }
        var importTitle by remember(shouldProposeTitle) { mutableStateOf(shouldProposeTitle) }
        val selectedSuggestedTags = remember(suggestion.tags) {
            mutableStateListOf<String>().apply { addAll(suggestion.tags) }
        }
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

                    if (shouldProposeArtist) {
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
                                Text(spotDetails?.spot?.category?.getCategoryCreatorLabel() ?: "Artist / Crew", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(suggestionArtist, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (shouldProposeTitle) {
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
                                Text(suggestionTitle, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (suggestion.tags.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newImportValue = !importTags
                                        importTags = newImportValue
                                        if (newImportValue) {
                                            selectedSuggestedTags.clear()
                                            selectedSuggestedTags.addAll(suggestion.tags)
                                        } else {
                                            selectedSuggestedTags.clear()
                                        }
                                    }
                            ) {
                                Checkbox(
                                    checked = importTags,
                                    onCheckedChange = { checked ->
                                        importTags = checked
                                        if (checked) {
                                            selectedSuggestedTags.clear()
                                            selectedSuggestedTags.addAll(suggestion.tags)
                                        } else {
                                            selectedSuggestedTags.clear()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Suggested Tags", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestion.tags.forEach { tag ->
                                    val isSelected = selectedSuggestedTags.contains(tag)
                                    InputChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                selectedSuggestedTags.remove(tag)
                                                if (selectedSuggestedTags.isEmpty()) {
                                                    importTags = false
                                                }
                                            } else {
                                                selectedSuggestedTags.add(tag)
                                                importTags = true
                                            }
                                        },
                                        label = { Text("#$tag") },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedLabelColor = MaterialTheme.colorScheme.background,
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                            labelColor = Color.Gray,
                                            containerColor = Color.Transparent
                                        ),
                                        border = InputChipDefaults.inputChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) Color.Transparent else Color.Gray,
                                            selectedBorderColor = Color.Transparent
                                        ),
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Deselect",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (shouldProposeArtist && importArtist) {
                            viewModel.updateArtists(listOf(suggestionArtist))
                        }
                        if (shouldProposeTitle && importTitle) {
                            viewModel.updateDescription(suggestionTitle)
                        }
                        if (importTags && selectedSuggestedTags.isNotEmpty()) {
                            val currentTags = spotDetails?.spot?.tags ?: emptyList()
                            val merged = (currentTags + selectedSuggestedTags).distinct()
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

    // Wikipedia AI Search Dialog / States
    when (val state = wikiSearchState) {
        is WikiSearchState.Searching -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetWikiSearchState() },
                confirmButton = {},
                title = { Text("Searching Wikipedia...") },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        is WikiSearchState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetWikiSearchState() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Wikipedia Link")
                    }
                },
                text = {
                    Text("Found relevant Wikipedia page for \"${state.title}\":\n\n${state.url}\n\nWould you like to add this to the field notes?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addNote(state.url, System.currentTimeMillis())
                            viewModel.resetWikiSearchState()
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetWikiSearchState() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        is WikiSearchState.NotFound -> {
            LaunchedEffect(state) {
                Toast.makeText(context, "No relevant Wikipedia page found.", Toast.LENGTH_SHORT).show()
                viewModel.resetWikiSearchState()
            }
        }
        is WikiSearchState.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetWikiSearchState()
            }
        }
        WikiSearchState.Idle -> {}
    }

    var noteInput by remember { mutableStateOf("") }
    var zoomImage by remember { mutableStateOf<SpotImage?>(null) }
    var isMapPickerDialogVisible by remember { mutableStateOf(false) }

    var showLimitExceededDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<SpotImage?>(null) }

    if (imageToDelete != null) {
        val targetImg = imageToDelete!!
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text("Delete Photo?") },
            text = { Text("Are you sure you want to delete this photo from the timeline? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteImage(targetImg)
                        imageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    var noteToDelete by remember { mutableStateOf<SpotNote?>(null) }

    if (noteToDelete != null) {
        val targetNote = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(targetNote.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

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



    // Launcher to add multiple images to this spot
    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            scope.launch(Dispatchers.Default) {
                uris.forEachIndexed { index, uri ->
                    val thumbnailPath = ImageOptimizer.createThumbnail(context, uri)
                    if (thumbnailPath != null) {
                        viewModel.addImage(uri.toString(), thumbnailPath, System.currentTimeMillis() + index)
                    }
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
                                    viewModel.toggleStarred()
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
                            category = details.spot.category,
                            isFallback = if (isCreationMode) draftIsFallback else details.spot.isImported,
                            onImageClick = { zoomImage = mainImage }
                        )
                    }

                    if (!isCreationMode) {
                        DetailPhotoTimeline(
                            sortedImages = sortedImages,
                            onAddPhotoClick = {
                                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onImageClick = { zoomImage = it },
                            onHeartClick = { viewModel.setMainImage(it.id) },
                            onDeleteClick = { imageToDelete = it },
                            onRatingChange = { img, rating -> viewModel.updateImageRating(img, rating) }
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
                        onUpdateArtworkDate = { viewModel.updateArtworkDate(it) },
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
                        category = details.spot.category,
                        useDarkMap = useDarkMap
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
                            },
                            isArtistRecognitionEnabled = isArtistRecognitionEnabled,
                            onWikiSearchClick = { viewModel.searchWikipediaForSpot() },
                            onDeleteNote = { noteToDelete = it },
                            onUpdateNote = { id, text -> viewModel.updateNote(id, text) }
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
                            category = details.spot.category,
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
                            onUpdateArtworkDate = { viewModel.updateArtworkDate(it) },
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
                            category = details.spot.category,
                            useDarkMap = useDarkMap
                        )

                        if (!isCreationMode) {
                            DetailPhotoTimeline(
                                sortedImages = sortedImages,
                                onAddPhotoClick = {
                                    pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onImageClick = { zoomImage = it },
                                onHeartClick = { viewModel.setMainImage(it.id) },
                                onDeleteClick = { imageToDelete = it },
                                onRatingChange = { img, rating -> viewModel.updateImageRating(img, rating) }
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
                                },
                                isArtistRecognitionEnabled = isArtistRecognitionEnabled,
                                onWikiSearchClick = { viewModel.searchWikipediaForSpot() },
                                onDeleteNote = { noteToDelete = it },
                                onUpdateNote = { id, text -> viewModel.updateNote(id, text) }
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
                            useDarkMap = useDarkMap,
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




