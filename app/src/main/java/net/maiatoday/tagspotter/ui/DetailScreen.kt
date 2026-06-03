package net.maiatoday.tagspotter.ui

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import net.maiatoday.tagspotter.theme.categoryColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotImage
import net.maiatoday.tagspotter.ui.components.OsmMapView
import net.maiatoday.tagspotter.ui.components.OsmMarker
import net.maiatoday.tagspotter.ui.screens.ZoomableImageOverlay
import net.maiatoday.tagspotter.ui.viewmodel.DetailViewModel
import net.maiatoday.tagspotter.utils.ImageOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    spotId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as TagSpotterApplication
    val scope = rememberCoroutineScope()

    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.provideFactory(spotId, app.repository, app.settingsRepository),
        key = spotId.toString()
    )

    val spotDetails by viewModel.spotDetails.collectAsStateWithLifecycle()
    val defaultPhotographer by viewModel.defaultPhotographer.collectAsStateWithLifecycle()
    val recentCustomTags by viewModel.recentCustomTags.collectAsStateWithLifecycle()

    var noteInput by remember { mutableStateOf("") }
    var zoomImage by remember { mutableStateOf<SpotImage?>(null) }
    var isMapPickerDialogVisible by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spot Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
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

        val isErased = details.spot.status == "erased"
        val sortedImages = details.images.sortedBy { it.timestamp }
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Photo Timeline
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailPhotoTimeline(
                        sortedImages = sortedImages,
                        onAddPhotoClick = {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onImageClick = { zoomImage = it }
                    )
                }

                // Right Pane: Info Card and Notes Section
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailInfoCard(
                        details = details,
                        defaultPhotographer = defaultPhotographer,
                        recentCustomTags = recentCustomTags,
                        onUpdateStatus = { nextStatus -> viewModel.updateStatus(nextStatus) },
                        onUpdateCategory = { category -> viewModel.updateCategory(category) },
                        onUpdateArtists = { list -> viewModel.updateArtists(list) },
                        onUpdatePhotographer = { name -> viewModel.updatePhotographer(name) },
                        onUpdateDescription = { desc -> viewModel.updateDescription(desc) },
                        onUpdateTags = { tags -> viewModel.updateTags(tags) },
                        onMapPickerClick = { isMapPickerDialogVisible = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                DetailPhotoTimeline(
                    sortedImages = sortedImages,
                    onAddPhotoClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onImageClick = { zoomImage = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                DetailInfoCard(
                    details = details,
                    defaultPhotographer = defaultPhotographer,
                    recentCustomTags = recentCustomTags,
                    onUpdateStatus = { nextStatus -> viewModel.updateStatus(nextStatus) },
                    onUpdateCategory = { category -> viewModel.updateCategory(category) },
                    onUpdateArtists = { list -> viewModel.updateArtists(list) },
                    onUpdatePhotographer = { name -> viewModel.updatePhotographer(name) },
                    onUpdateDescription = { desc -> viewModel.updateDescription(desc) },
                    onUpdateTags = { tags -> viewModel.updateTags(tags) },
                    onMapPickerClick = { isMapPickerDialogVisible = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

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

    if (zoomImage != null) {
        Dialog(
            onDismissRequest = { zoomImage = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ZoomableImageOverlay(
                imagePath = zoomImage!!.imagePath,
                thumbnailPath = zoomImage!!.thumbnailPath,
                onClose = { zoomImage = null }
            )
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

                    // OpenStreetMap via shared OsmMapView
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
}

@Composable
fun SpotTimelineCard(
    image: SpotImage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(200.dp)
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
                    .height(150.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Format photo timestamp
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Photo Timeline",
                style = MaterialTheme.typography.titleLarge,
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
                    onClick = { onImageClick(image) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailInfoCard(
    details: SpotDetails,
    defaultPhotographer: String,
    recentCustomTags: List<String>,
    onUpdateStatus: (String) -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateArtists: (List<String>) -> Unit,
    onUpdatePhotographer: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateTags: (List<String>) -> Unit,
    onMapPickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isErased = details.spot.status == "erased"

    var isEditingArtists by remember { mutableStateOf(false) }
    var artistEditInput by remember { mutableStateOf("") }
    val localArtistsList = remember { mutableStateListOf<String>() }

    var isEditingPhotographer by remember { mutableStateOf(false) }
    var photographerEditInput by remember { mutableStateOf("") }

    var isEditingDescription by remember { mutableStateOf(false) }
    var descriptionEditInput by remember { mutableStateOf("") }

    var isEditingTags by remember { mutableStateOf(false) }
    var customTagEditInput by remember { mutableStateOf("") }
    val localTagsList = remember { mutableStateListOf<String>() }

    LaunchedEffect(isEditingArtists) {
        if (isEditingArtists) {
            localArtistsList.clear()
            localArtistsList.addAll(details.spot.artists)
        }
    }

    LaunchedEffect(isEditingPhotographer) {
        if (isEditingPhotographer) {
            photographerEditInput = details.spot.photographer
        }
    }

    LaunchedEffect(isEditingDescription) {
        if (isEditingDescription) {
            descriptionEditInput = details.spot.description
        }
    }

    LaunchedEffect(isEditingTags) {
        if (isEditingTags) {
            localTagsList.clear()
            localTagsList.addAll(details.spot.tags)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { isCategoryDropdownExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = details.spot.category.replace("_", " ").uppercase(),
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        val categories = listOf("graffiti", "sculpture", "tree", "architecture", "public_place")
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

                Button(
                    onClick = {
                        val nextStatus = if (isErased) "active" else "erased"
                        onUpdateStatus(nextStatus)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isErased) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        contentColor = if (isErased) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (isErased) "Mark Active" else "Mark as Erased")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val sdf = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())
                Text(
                    text = "Created: ${sdf.format(Date(details.spot.createdAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "GPS: %.6f, %.6f", details.spot.latitude, details.spot.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }

                IconButton(
                    onClick = onMapPickerClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit location",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (isEditingArtists) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Edit Artist(s) / Writer(s)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

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
                                            .clickable { localArtistsList.remove(artist) }
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
                            text = "Artist(s) / Writer(s)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.spot.artists.forEach { artist ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = artist,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (isEditingPhotographer) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Edit Photographer",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

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

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = photographerEditInput,
                        onValueChange = { photographerEditInput = it },
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
                            androidx.compose.material3.TextButton(
                                onClick = { photographerEditInput = defaultPhotographer },
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
                            text = "Photographer",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
                        )
                        if (!details.spot.isImported) {
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
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.photographer.ifEmpty { "Unknown Photographer" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (isEditingDescription) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Edit Description",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

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
                                    contentDescription = "Save description",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descriptionEditInput,
                        onValueChange = { descriptionEditInput = it },
                        label = { Text("Description") },
                        placeholder = { Text("e.g. Artist info, style, notes...") },
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
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
                        )
                        IconButton(
                            onClick = { isEditingDescription = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit description",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.description.ifEmpty { "No description given." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (isEditingTags) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Edit Tags",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Predefined Quick Tags
                    Text(
                        text = "Quick Select Tags",
                        style = MaterialTheme.typography.labelMedium,
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recent Custom Tags",
                            style = MaterialTheme.typography.labelMedium,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Custom Tag Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customTagEditInput,
                            onValueChange = { customTagEditInput = it },
                            label = { Text("Add Custom Tag") },
                            placeholder = { Text("e.g. stencil") },
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

                    // Display current local tags list in edit mode
                    if (localTagsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            localTagsList.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .background(Color.DarkGray, RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        tint = Color.LightGray,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { localTagsList.remove(tag) }
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
                            text = "Tags",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
                        )
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
                    Spacer(modifier = Modifier.height(4.dp))
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
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailNotesSection(
    sortedNotes: List<net.maiatoday.tagspotter.data.SpotNote>,
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
                text = "Observations & Notes Log",
                style = MaterialTheme.typography.titleLarge,
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
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
