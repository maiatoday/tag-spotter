package com.example.tagspotter.ui

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import com.example.tagspotter.ui.screens.ZoomableImageOverlay
import com.example.tagspotter.TagSpotterApplication
import com.example.tagspotter.data.SpotImage
import com.example.tagspotter.utils.ImageOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    spotId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as TagSpotterApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()

    // Fetch the spot detail flow
    val spotDetails by repository.getSpotById(spotId).collectAsState(initial = null)
    var noteInput by remember { mutableStateOf("") }
    var zoomImagePath by remember { mutableStateOf<String?>(null) }

    var isEditingArtists by remember { mutableStateOf(false) }
    var artistEditInput by remember { mutableStateOf("") }
    val localArtistsList = remember { mutableStateListOf<String>() }
    var isMapPickerDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(spotDetails, isEditingArtists) {
        if (isEditingArtists && spotDetails != null) {
            localArtistsList.clear()
            localArtistsList.addAll(spotDetails!!.spot.artists)
        }
    }

    // Launcher to add another image to this spot
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.Default) {
                val optimizedPath = ImageOptimizer.optimizeAndSaveImage(context, uri)
                if (optimizedPath != null) {
                    repository.addImageToSpot(spotId, optimizedPath, System.currentTimeMillis())
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val details = spotDetails
                            if (details != null) {
                                scope.launch(Dispatchers.IO) {
                                    repository.deleteSpot(details)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Spot deleted successfully", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                }
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

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Photo Timeline Carousel Header
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
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
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

            // Horizontally Scrollable Timeline Images
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(end = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedImages, key = { it.id }) { image ->
                    SpotTimelineCard(
                        image = image,
                        onClick = { zoomImagePath = image.imagePath }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Category & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (details.spot.category) {
                                        "graffiti" -> MaterialTheme.colorScheme.tertiary
                                        "sculpture" -> MaterialTheme.colorScheme.secondary
                                        "tree" -> MaterialTheme.colorScheme.primary
                                        "architecture" -> Color(0xFFBF5AF2)
                                        "public_place" -> Color(0xFFFF9F0A)
                                        else -> Color.DarkGray
                                    },
                                    shape = RoundedCornerShape(4.dp)
                               )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = details.spot.category.replace("_", " ").uppercase(),
                                color = Color.Black,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Erased Status Toggle Switch
                        Button(
                            onClick = {
                                val nextStatus = if (isErased) "active" else "erased"
                                scope.launch(Dispatchers.IO) {
                                    repository.updateSpotStatus(details.spot.id, nextStatus)
                                }
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

                    // Date & GPS
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
                                text = String.format("GPS: %.6f, %.6f", details.spot.latitude, details.spot.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }

                        IconButton(
                            onClick = { isMapPickerDialogVisible = true },
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

                    // Artists Section
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
                                            scope.launch(Dispatchers.IO) {
                                                repository.updateSpotArtists(spotId, localArtistsList.toList())
                                                withContext(Dispatchers.Main) {
                                                    isEditingArtists = false
                                                }
                                            }
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

                    // Description text
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Original Description",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.description.ifEmpty { "No description given." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Display all tags
                    if (details.spot.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.spot.tags.forEach { tag ->
                                Text(
                                    text = "#$tag",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notes Timeline section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
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

            // List of chronological notes
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

            // Write Note Form
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Write a note at this spot...") },
                    placeholder = { Text("e.g. Tag faded, style details...") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val noteText = noteInput.trim()
                            if (noteText.isNotEmpty()) {
                                scope.launch(Dispatchers.IO) {
                                    repository.addNoteToSpot(spotId, noteText, System.currentTimeMillis())
                                    withContext(Dispatchers.Main) {
                                        noteInput = ""
                                    }
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val noteText = noteInput.trim()
                        if (noteText.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                repository.addNoteToSpot(spotId, noteText, System.currentTimeMillis())
                                withContext(Dispatchers.Main) {
                                    noteInput = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send note",
                        tint = MaterialTheme.colorScheme.background
                    )
                }
            }
        }
    }

    if (zoomImagePath != null) {
        Dialog(
            onDismissRequest = { zoomImagePath = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ZoomableImageOverlay(
                imagePath = zoomImagePath!!,
                onClose = { zoomImagePath = null }
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
                var mapInstance: MapView? by remember { mutableStateOf(null) }

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

                    // OpenStreetMap AndroidView
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    controller.setZoom(17.0)
                                    controller.setCenter(GeoPoint(tempLat, tempLng))

                                    // Add marker overlay
                                    val marker = Marker(this).apply {
                                        position = GeoPoint(tempLat, tempLng)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "Graffiti Spot"
                                    }
                                    overlays.add(marker)
                                    
                                    // Tap to move marker
                                    val receiver = object : MapEventsReceiver {
                                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                            if (p != null) {
                                                tempLat = p.latitude
                                                tempLng = p.longitude
                                                marker.position = p
                                                invalidate()
                                            }
                                            return true
                                        }

                                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                                    }
                                    overlays.add(MapEventsOverlay(receiver))
                                    mapInstance = this
                                }
                            },
                            update = { map ->
                                map.controller.setCenter(GeoPoint(tempLat, tempLng))
                            },
                            modifier = Modifier.fillMaxSize()
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
                                scope.launch(Dispatchers.IO) {
                                    repository.updateSpotLocation(spotId, tempLat, tempLng)
                                    withContext(Dispatchers.Main) {
                                        isMapPickerDialogVisible = false
                                        Toast.makeText(context, "Location Updated!", Toast.LENGTH_SHORT).show()
                                    }
                                }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = File(image.imagePath),
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
