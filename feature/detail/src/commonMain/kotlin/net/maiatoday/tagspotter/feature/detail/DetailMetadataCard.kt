package net.maiatoday.tagspotter.feature.detail

import net.maiatoday.tagspotter.feature.detail.res.DetailRes


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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import net.maiatoday.tagspotter.feature.detail.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.ui.getCategoryCreatorLabel
import net.maiatoday.tagspotter.core.ui.getCategoryCreatorPlaceholder
import net.maiatoday.tagspotter.core.ui.getCategoryCreatorTextFieldLabel
import net.maiatoday.tagspotter.core.ui.getCategoryCreatorUnknownLabel
import net.maiatoday.tagspotter.core.ui.getCategoryDateLabel
import net.maiatoday.tagspotter.core.ui.getCategoryDatePlaceholder
import net.maiatoday.tagspotter.core.ui.getCategoryDateTextFieldLabel
import net.maiatoday.tagspotter.core.ui.getCategoryStatusActionMarkInactiveText
import net.maiatoday.tagspotter.core.ui.theme.categoryColors

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
    onUpdateArtworkDate: (String) -> Unit,
    onMapPickerClick: () -> Unit
) {
    val platformHelper = net.maiatoday.tagspotter.feature.detail.res.rememberDetailPlatformHelper()
    var isEditingArtists by remember { mutableStateOf(isCreationMode) }
    var artistEditInput by remember { mutableStateOf("") }
    val localArtistsList = remember { mutableStateListOf<String>() }

    var isEditingPhotographer by remember { mutableStateOf(isCreationMode) }
    var photographerEditInput by remember { mutableStateOf(details.spot.photographer) }

    var isEditingDescription by remember { mutableStateOf(isCreationMode) }
    var descriptionEditInput by remember { mutableStateOf(details.spot.description) }

    var isEditingArtworkDate by remember { mutableStateOf(isCreationMode) }
    var artworkDateEditInput by remember { mutableStateOf(details.spot.artworkDate) }

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

    LaunchedEffect(isEditingArtworkDate, details.spot.artworkDate) {
        if (isEditingArtworkDate) {
            artworkDateEditInput = details.spot.artworkDate
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
                        val categories = Spot.CATEGORIES
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
                        text = stringResource(DetailRes.string.new_spot),
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
                            text = if (isErased) stringResource(DetailRes.string.mark_active) else stringResource(details.spot.category.getCategoryStatusActionMarkInactiveText()),
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
                            text = stringResource(DetailRes.string.title_header),
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
                                        contentDescription = stringResource(DetailRes.string.content_desc_save_title),
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
                                        contentDescription = stringResource(DetailRes.string.content_desc_cancel_edit),
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
                        label = { Text(stringResource(DetailRes.string.title_label)) },
                        placeholder = { Text(stringResource(DetailRes.string.title_placeholder)) },
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
                            text = stringResource(DetailRes.string.title_header),
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
                                contentDescription = stringResource(DetailRes.string.content_desc_edit_title),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.description.ifEmpty { stringResource(DetailRes.string.no_title_logged) },
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
                                text = stringResource(details.spot.category.getCategoryCreatorLabel()),
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
                                            contentDescription = stringResource(DetailRes.string.content_desc_search_lens),
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
                                            contentDescription = stringResource(DetailRes.string.content_desc_identify_ai),
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
                                        val finalArtists =
                                            if (cleaned.isNotEmpty() && !localArtistsList.contains(
                                                    cleaned
                                                )
                                            ) {
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
                                        contentDescription = stringResource(DetailRes.string.content_desc_save_artists),
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
                                        contentDescription = stringResource(DetailRes.string.content_desc_cancel_edit),
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
                            label = { Text(stringResource(details.spot.category.getCategoryCreatorTextFieldLabel())) },
                            placeholder = { Text(stringResource(details.spot.category.getCategoryCreatorPlaceholder())) },
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
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = stringResource(DetailRes.string.content_desc_add_artist),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (localArtistsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            localArtistsList.forEach { artist ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(DetailRes.string.content_desc_remove_artist),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    localArtistsList.remove(artist)
                                                    if (isCreationMode) onUpdateArtists(
                                                        localArtistsList.toList()
                                                    )
                                                }
                                        )
                                    }
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
                                text = stringResource(details.spot.category.getCategoryCreatorLabel()),
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
                                    contentDescription = stringResource(DetailRes.string.content_desc_edit_artists),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (details.spot.artists.isEmpty()) stringResource(details.spot.category.getCategoryCreatorUnknownLabel()) else details.spot.artists.joinToString(
                            ", "
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                            text = stringResource(DetailRes.string.photographer_header),
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
                                        contentDescription = stringResource(DetailRes.string.content_desc_save_photographer),
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
                                        contentDescription = stringResource(DetailRes.string.content_desc_cancel_edit),
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
                        label = { Text(stringResource(DetailRes.string.photographer_label)) },
                        placeholder = { Text(defaultPhotographer.ifEmpty { stringResource(DetailRes.string.photographer_placeholder_name) }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                            text = stringResource(DetailRes.string.photographer_header),
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
                                contentDescription = stringResource(DetailRes.string.content_desc_edit_photographer),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.photographer.ifEmpty { defaultPhotographer.ifEmpty { stringResource(DetailRes.string.not_set) } },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Artwork Date Section
            if (isEditingArtworkDate) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(details.spot.category.getCategoryDateLabel()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        if (!isCreationMode) {
                            Row {
                                IconButton(
                                    onClick = {
                                        onUpdateArtworkDate(artworkDateEditInput.trim())
                                        isEditingArtworkDate = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(DetailRes.string.content_desc_save_artwork_date),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isEditingArtworkDate = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(DetailRes.string.content_desc_cancel_edit),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = artworkDateEditInput,
                        onValueChange = {
                            artworkDateEditInput = it
                            if (isCreationMode) onUpdateArtworkDate(it)
                        },
                        label = { Text(stringResource(details.spot.category.getCategoryDateTextFieldLabel())) },
                        placeholder = { Text(stringResource(details.spot.category.getCategoryDatePlaceholder())) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                            text = stringResource(details.spot.category.getCategoryDateLabel()),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isEditingArtworkDate = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(DetailRes.string.content_desc_edit_artwork_date),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.spot.artworkDate.ifEmpty { stringResource(DetailRes.string.date_unknown) },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons (Refine, Map It, Navigate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isCreationMode) {
                    Button(
                        onClick = {
                            platformHelper.navigateToLocation(details.spot.latitude, details.spot.longitude)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(DetailRes.string.btn_navigate),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Button(
                    onClick = onMapPickerClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = if (isCreationMode) Modifier.fillMaxWidth() else Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isCreationMode) Icons.Default.EditLocationAlt else Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCreationMode) stringResource(DetailRes.string.btn_refine) else stringResource(DetailRes.string.btn_map_it),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
