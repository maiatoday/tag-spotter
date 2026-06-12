package net.maiatoday.tagspotter.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current



    val savedName by viewModel.photographerName.collectAsStateWithLifecycle()
    var photographerNameInput by remember(savedName) { mutableStateOf(savedName) }

    val savedHomeCity by viewModel.homeCity.collectAsStateWithLifecycle()
    val isInitiallyCustom = savedHomeCity.startsWith("Custom:")
    var homeCityInput by remember(savedHomeCity) {
        mutableStateOf(if (isInitiallyCustom) "Custom" else savedHomeCity)
    }

    var customLatInput by remember(savedHomeCity) {
        mutableStateOf(
            if (isInitiallyCustom) {
                savedHomeCity.removePrefix("Custom:").trim().split(",").firstOrNull()?.trim() ?: ""
            } else ""
        )
    }

    var customLngInput by remember(savedHomeCity) {
        mutableStateOf(
            if (isInitiallyCustom) {
                savedHomeCity.removePrefix("Custom:").trim().split(",").getOrNull(1)?.trim() ?: ""
            } else ""
        )
    }

    var showCityDropdown by remember { mutableStateOf(false) }
    val cities = listOf("Milan", "London", "New York", "Paris", "Tokyo", "Berlin", "Rome", "San Francisco", "Sydney", "Custom")

    val artistRecognitionEnabled by viewModel.artistRecognitionEnabled.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    var geminiApiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
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
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Settings Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                    Text(
                        text = stringResource(R.string.profile_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_section_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = photographerNameInput,
                        onValueChange = { photographerNameInput = it },
                        label = { Text(stringResource(R.string.photographer_name_label)) },
                        placeholder = { Text(stringResource(R.string.photographer_name_placeholder)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.map_preferences_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.map_preferences_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (homeCityInput == "Custom") stringResource(R.string.city_custom) else homeCityInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.home_city_label)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .clickable { showCityDropdown = !showCityDropdown }
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
                            expanded = showCityDropdown,
                            onDismissRequest = { showCityDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(if (city == "Custom") stringResource(R.string.city_custom) else city) },
                                    onClick = {
                                        homeCityInput = city
                                        showCityDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (homeCityInput == "Custom") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = customLatInput,
                                onValueChange = { customLatInput = it },
                                label = { Text(stringResource(R.string.latitude_label)) },
                                placeholder = { Text(stringResource(R.string.latitude_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            OutlinedTextField(
                                value = customLngInput,
                                onValueChange = { customLngInput = it },
                                label = { Text(stringResource(R.string.longitude_label)) },
                                placeholder = { Text(stringResource(R.string.longitude_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    
                    val isSystemDark = isSystemInDarkTheme()
                    if (isSystemDark) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.darkmode_map_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.darkmode_map_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            val darkMapEnabled by viewModel.darkMapEnabled.collectAsStateWithLifecycle()
                            Switch(
                                checked = darkMapEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateDarkMapEnabled(isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            viewModel.updatePhotographerName(photographerNameInput.trim())
                            viewModel.updateGeminiApiKey(geminiApiKeyInput.trim())
                            if (homeCityInput == "Custom") {
                                val lat = customLatInput.trim().toDoubleOrNull()
                                val lng = customLngInput.trim().toDoubleOrNull()
                                if (lat != null && lng != null) {
                                    viewModel.updateHomeCity("Custom: $lat, $lng")
                                    Toast.makeText(context, context.getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.invalid_coordinates_toast), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                viewModel.updateHomeCity(homeCityInput)
                                Toast.makeText(context, context.getString(R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.save_settings_btn),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }



            // AI Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.artist_id_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.artist_id_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.enable_recognition_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = artistRecognitionEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateArtistRecognitionEnabled(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }

                    if (artistRecognitionEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.ai_detection_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ai_detection_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = geminiApiKeyInput,
                            onValueChange = { geminiApiKeyInput = it },
                            label = { Text(stringResource(R.string.gemini_api_key_label)) },
                            placeholder = { Text(stringResource(R.string.gemini_api_key_placeholder)) },
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                val image = if (isApiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                val description = if (isApiKeyVisible) stringResource(R.string.content_desc_hide_api_key) else stringResource(R.string.content_desc_show_api_key)
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(imageVector = image, contentDescription = description)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            }
        }
    }
}
