package net.maiatoday.tagspotter.feature.detail

import android.content.Intent
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.ui.getCategoryCreatorLabel
import androidx.compose.ui.res.stringResource
import net.maiatoday.tagspotter.feature.detail.R
import net.maiatoday.tagspotter.core.photo.ImageOptimizer
import net.maiatoday.tagspotter.core.ui.OsmMapView
import net.maiatoday.tagspotter.core.ui.OsmMarker
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

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
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.content_desc_search_lens)))
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_img_file_not_found), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.toast_failed_share_img, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.toast_no_img_search), Toast.LENGTH_SHORT).show()
        }
    }

    val onIdentifyArtist: () -> Unit = {
        if (mainImagePath.isNotEmpty()) {
            viewModel.identifyArtist(mainImagePath)
        } else {
            Toast.makeText(context, context.getString(R.string.toast_no_img_analyze), Toast.LENGTH_SHORT).show()
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
                    Text(stringResource(R.string.ai_suggestions_title))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.ai_suggestions_intro),
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
                                Text(spotDetails?.spot?.category?.let { stringResource(it.getCategoryCreatorLabel()) } ?: stringResource(net.maiatoday.tagspotter.core.ui.R.string.creator_label_default), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
                                Text(stringResource(R.string.suggested_title), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
                                Text(stringResource(R.string.suggested_tags), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
                                                    contentDescription = stringResource(R.string.content_desc_deselect),
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
                    Text(stringResource(R.string.apply_selected))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetAiState() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // AI Error Dialog
    if (aiState is AiState.Error) {
        val error = aiState as AiState.Error
        val (title, message) = when (error) {
            is AiState.Error.MissingKey -> {
                context.getString(R.string.err_key_missing_title) to context.getString(R.string.err_key_missing_msg)
            }
            is AiState.Error.InvalidKey -> {
                context.getString(R.string.err_key_invalid_title) to context.getString(R.string.err_key_invalid_msg)
            }
            is AiState.Error.QuotaExceeded -> {
                context.getString(R.string.err_quota_exceeded_title) to context.getString(R.string.err_quota_exceeded_msg)
            }
            is AiState.Error.SafetyBlocked -> {
                context.getString(R.string.err_safety_blocked_title) to context.getString(R.string.err_safety_blocked_msg)
            }
            is AiState.Error.Generic -> {
                context.getString(R.string.err_generic_recognition_title) to error.message
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
                    Text(stringResource(R.string.ok))
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
                title = { Text(stringResource(R.string.wiki_searching_title)) },
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
                        Text(stringResource(R.string.wiki_add_link_title))
                    }
                },
                text = {
                    Text(stringResource(R.string.wiki_success_msg, state.title, state.url))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addNote(state.url, System.currentTimeMillis())
                            viewModel.resetWikiSearchState()
                        }
                    ) {
                        Text(stringResource(R.string.wiki_btn_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetWikiSearchState() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        is WikiSearchState.NotFound -> {
            LaunchedEffect(state) {
                Toast.makeText(context, context.getString(R.string.wiki_not_found_toast), Toast.LENGTH_SHORT).show()
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
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_msg)) },
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
                    Text(stringResource(R.string.delete_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    var noteToDelete by remember { mutableStateOf<SpotNote?>(null) }

    if (noteToDelete != null) {
        val targetNote = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_msg)) },
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
                    Text(stringResource(R.string.delete_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
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
                                Text(stringResource(R.string.cancel))
                            }

                            Button(
                                onClick = onSaveClick,
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text(stringResource(R.string.save_spot))
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
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = onSaveClick,
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(stringResource(R.string.save_spot))
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
                        text = stringResource(R.string.update_coords_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.update_coords_msg),
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
                            title = stringResource(R.string.spot_location_marker),
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
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                viewModel.updateLocation(tempLat, tempLng)
                                isMapPickerDialogVisible = false
                                Toast.makeText(context, context.getString(R.string.location_updated_toast), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.background
                            )
                        ) {
                            Text(stringResource(R.string.confirm_location_btn))
                        }
                    }
                }
            }
        }
    }



    if (showLimitExceededDialog) {
        AlertDialog(
            onDismissRequest = { showLimitExceededDialog = false },
            title = { Text(stringResource(R.string.starred_limit_title)) },
            text = {
                Text(stringResource(R.string.starred_limit_msg))
            },
            confirmButton = {
                TextButton(onClick = { showLimitExceededDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}




