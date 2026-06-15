package net.maiatoday.tagspotter.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.feature.main.res.MainRes
import net.maiatoday.tagspotter.feature.main.res.stringResource
import net.maiatoday.tagspotter.feature.gallery.GalleryScreen
import net.maiatoday.tagspotter.feature.map.MapScreen
import org.koin.compose.viewmodel.koinViewModel
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes

enum class Tab {
    Gallery,
    Map
}

@Composable
fun MainContainer(
    onSpotClick: (Long) -> Unit,
    onPhotoCaptured: (String, String, Double, Double, Boolean, String, Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onTriggerFiles: (onPhotoPicked: (String) -> Unit) -> Unit,
    versionName: String,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Gallery) }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showTestData by viewModel.showTestData.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val picker = rememberImagePickerKMP()

    LaunchedEffect(picker.result) {
        val result = picker.result
        if (result is ImagePickerResult.Success) {
            val photo = result.photos.firstOrNull()
            if (photo != null) {
                scope.launch {
                    try {
                        val bytes = photo.loadBytes()
                        val tempUri = viewModel.prepareCameraCapture()
                        if (tempUri != null) {
                            val tempPath = viewModel.uiState.value.tempPhotoFilePath
                            if (tempPath != null) {
                                val written = viewModel.writePhotoBytes(bytes, tempPath)
                                if (written) {
                                    viewModel.handleCameraCaptureSuccess()
                                } else {
                                    showToast("Failed to write captured image to file.")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to load captured image bytes: ${e.message}")
                    }
                }
            }
        } else if (result is ImagePickerResult.Error) {
            showToast("Camera error: ${result.exception.message}")
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.PhotoProcessed -> {
                    onPhotoCaptured(
                        event.imagePath,
                        event.thumbnailPath,
                        event.latitude,
                        event.longitude,
                        event.isFallback,
                        event.category,
                        event.captureTime
                    )
                }
                is MainEvent.ShowError -> {
                    showToast(event.message)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedTab != Tab.Map,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TAGSPOTTER",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text(stringResource(MainRes.string.drawer_settings_label), fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                onNavigateToSettings()
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(MainRes.string.content_desc_settings),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(MainRes.string.drawer_mock_data_label),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = showTestData,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateShowTestData(isChecked)
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

                    Text(
                        text = stringResource(MainRes.string.drawer_version_format, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationRailItem(
                            selected = selectedTab == Tab.Gallery,
                            onClick = { selectedTab = Tab.Gallery },
                            icon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = stringResource(MainRes.string.content_desc_gallery_tab),
                                        tint = if (selectedTab == Tab.Gallery) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    if (selectedTab == Tab.Gallery) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(1.dp)
                                                )
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            },
                            label = { Text(stringResource(MainRes.string.tab_gallery_label)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationRailItem(
                            selected = selectedTab == Tab.Map,
                            onClick = { selectedTab = Tab.Map },
                            icon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = stringResource(MainRes.string.content_desc_maps_tab),
                                        tint = if (selectedTab == Tab.Map) MaterialTheme.colorScheme.secondary else Color.Gray
                                    )
                                    if (selectedTab == Tab.Map) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = RoundedCornerShape(1.dp)
                                                )
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            },
                            label = { Text(stringResource(MainRes.string.tab_maps_label)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        if (isCameraSupported) {
                            NavigationRailItem(
                                selected = false,
                                onClick = { picker.launchCamera() },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = stringResource(MainRes.string.content_desc_camera_tab),
                                        tint = Color.Gray
                                    )
                                },
                                label = { Text(stringResource(MainRes.string.tab_camera_label)) },
                                colors = NavigationRailItemDefaults.colors(
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                        NavigationRailItem(
                            selected = false,
                            onClick = { onTriggerFiles { uriString -> viewModel.handlePhotoPicked(uriString) } },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = stringResource(MainRes.string.content_desc_files_tab),
                                    tint = Color.Gray
                                )
                            },
                            label = { Text(stringResource(MainRes.string.tab_files_label)) },
                            colors = NavigationRailItemDefaults.colors(
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        TabContent(
                            selectedTab = selectedTab,
                            onSpotClick = onSpotClick,
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                }
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == Tab.Gallery,
                                onClick = { selectedTab = Tab.Gallery },
                                icon = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = stringResource(MainRes.string.content_desc_gallery_tab),
                                            tint = if (selectedTab == Tab.Gallery) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                        if (selectedTab == Tab.Gallery) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(2.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(1.dp)
                                                    )
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                },
                                label = { Text(stringResource(MainRes.string.tab_gallery_label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = Color.Gray,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == Tab.Map,
                                onClick = { selectedTab = Tab.Map },
                                icon = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = stringResource(MainRes.string.content_desc_maps_tab),
                                            tint = if (selectedTab == Tab.Map) MaterialTheme.colorScheme.secondary else Color.Gray
                                        )
                                        if (selectedTab == Tab.Map) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(2.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = RoundedCornerShape(1.dp)
                                                    )
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                },
                                label = { Text(stringResource(MainRes.string.tab_maps_label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = Color.Gray,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            if (isCameraSupported) {
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { picker.launchCamera() },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = stringResource(MainRes.string.content_desc_camera_tab),
                                            tint = Color.Gray
                                        )
                                    },
                                    label = { Text(stringResource(MainRes.string.tab_camera_label)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                            NavigationBarItem(
                                selected = false,
                                onClick = { onTriggerFiles { uriString -> viewModel.handlePhotoPicked(uriString) } },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = stringResource(MainRes.string.content_desc_files_tab),
                                        tint = Color.Gray
                                    )
                                },
                                label = { Text(stringResource(MainRes.string.tab_files_label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        TabContent(
                            selectedTab = selectedTab,
                            onSpotClick = onSpotClick,
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            enabled = true,
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(MainRes.string.loading_processing_image),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: Tab,
    onSpotClick: (Long) -> Unit,
    onMenuClick: () -> Unit
) {
    when (selectedTab) {
        Tab.Gallery -> GalleryScreen(
            onSpotClick = onSpotClick,
            onMenuClick = onMenuClick
        )
        Tab.Map -> MapScreen(
            onSpotClick = onSpotClick
        )
    }
}
