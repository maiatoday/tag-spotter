package net.maiatoday.tagspotter.feature.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.maiatoday.tagspotter.core.model.SpotDetails

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
                                    val cleaned =
                                        customTagEditInput.trim().lowercase().removePrefix("#")
                                    val finalTags =
                                        if (cleaned.isNotEmpty() && !localTagsList.contains(cleaned)) {
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

                    val predefinedTags =
                        remember { setOf("mural", "stencil", "throwup", "pasteup", "sticker") }
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
                                    val cleaned =
                                        customTagEditInput.trim().lowercase().removePrefix("#")
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
                                val cleaned =
                                    customTagEditInput.trim().lowercase().removePrefix("#")
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
                                .background(
                                    MaterialTheme.colorScheme.secondary,
                                    RoundedCornerShape(8.dp)
                                )
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
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.secondary,
                                        RoundedCornerShape(8.dp)
                                    )
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
