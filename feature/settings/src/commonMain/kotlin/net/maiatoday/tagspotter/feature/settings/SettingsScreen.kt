package net.maiatoday.tagspotter.feature.settings

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.maiatoday.tagspotter.feature.settings.res.SettingsRes
import net.maiatoday.tagspotter.feature.settings.res.stringResource
import net.maiatoday.tagspotter.feature.settings.res.rememberToastLauncher
import org.koin.compose.viewmodel.koinViewModel

class SettingsStrings(
    val settingsSaved: String,
    val invalidCoordinates: String
)

@Composable
fun rememberSettingsStrings() = SettingsStrings(
    settingsSaved = stringResource(SettingsRes.string.settings_saved_toast),
    invalidCoordinates = stringResource(SettingsRes.string.invalid_coordinates_toast)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val toastLauncher = rememberToastLauncher()
    val strings = rememberSettingsStrings()

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
                title = { Text(stringResource(SettingsRes.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(SettingsRes.string.content_desc_back))
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
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                var emailInput by remember { mutableStateOf("") }
                var passwordInput by remember { mutableStateOf("") }
                var showEmailForm by remember { mutableStateOf(false) }
                var isSignUpMode by remember { mutableStateOf(false) }

                // Backup & Sync Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                            text = "Cloud Sync & Backup",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Backup your local art spots, thumbnails, and notes to secure them across all your devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        val user = currentUser
                        if (user != null) {
                            // Authenticated UI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle with initials
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = (user.displayName ?: user.email ?: "?").take(2).uppercase()
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName ?: "Authenticated User",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = user.email ?: "No Email",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Sync status section
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSyncing) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSyncing) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Syncing with Cloud...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cloud Backup Active",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.signOut() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sign Out", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Unauthenticated UI
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // "Sign In with Google" (Standard button)
                                Button(
                                    onClick = { viewModel.signInWithGoogle() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Sign In with Google",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Or fallback email/password toggle
                                Text(
                                    text = if (showEmailForm) "Cancel Email Sign In" else "Use Email / Password instead",
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .clickable { showEmailForm = !showEmailForm }
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )

                                if (showEmailForm) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        label = { Text("Email Address") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            if (isSignUpMode) {
                                                viewModel.signUpWithEmailAndPassword(emailInput, passwordInput) { res ->
                                                    if (res.isFailure) {
                                                        toastLauncher.showToast(res.exceptionOrNull()?.message ?: "Sign up failed")
                                                    }
                                                }
                                            } else {
                                                viewModel.signInWithEmailAndPassword(emailInput, passwordInput) { res ->
                                                    if (res.isFailure) {
                                                        toastLauncher.showToast(res.exceptionOrNull()?.message ?: "Sign in failed")
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isSignUpMode) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable { isSignUpMode = !isSignUpMode }
                                            .padding(4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

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
                            text = stringResource(SettingsRes.string.profile_section_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(SettingsRes.string.profile_section_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = photographerNameInput,
                            onValueChange = { photographerNameInput = it },
                            label = { Text(stringResource(SettingsRes.string.photographer_name_label)) },
                            placeholder = { Text(stringResource(SettingsRes.string.photographer_name_placeholder)) },
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
                            text = stringResource(SettingsRes.string.map_preferences_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(SettingsRes.string.map_preferences_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (homeCityInput == "Custom") stringResource(SettingsRes.string.city_custom) else homeCityInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(SettingsRes.string.home_city_label)) },
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
                                        text = { Text(if (city == "Custom") stringResource(SettingsRes.string.city_custom) else city) },
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
                                    label = { Text(stringResource(SettingsRes.string.latitude_label)) },
                                    placeholder = { Text(stringResource(SettingsRes.string.latitude_placeholder)) },
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
                                    label = { Text(stringResource(SettingsRes.string.longitude_label)) },
                                    placeholder = { Text(stringResource(SettingsRes.string.longitude_placeholder)) },
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
                                        text = stringResource(SettingsRes.string.darkmode_map_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(SettingsRes.string.darkmode_map_desc),
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
                                        toastLauncher.showToast(strings.settingsSaved)
                                    } else {
                                        toastLauncher.showToast(strings.invalidCoordinates)
                                    }
                                } else {
                                    viewModel.updateHomeCity(homeCityInput)
                                    toastLauncher.showToast(strings.settingsSaved)
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
                                text = stringResource(SettingsRes.string.save_settings_btn),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            text = stringResource(SettingsRes.string.artist_id_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(SettingsRes.string.artist_id_desc),
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
                                text = stringResource(SettingsRes.string.enable_recognition_title),
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
                                text = stringResource(SettingsRes.string.ai_detection_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(SettingsRes.string.ai_detection_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = geminiApiKeyInput,
                                onValueChange = { geminiApiKeyInput = it },
                                label = { Text(stringResource(SettingsRes.string.gemini_api_key_label)) },
                                placeholder = { Text(stringResource(SettingsRes.string.gemini_api_key_placeholder)) },
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
                                    val description = if (isApiKeyVisible) stringResource(SettingsRes.string.content_desc_hide_api_key) else stringResource(SettingsRes.string.content_desc_show_api_key)
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
