package net.maiatoday.tagspotter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maiatoday.tagspotter.ui.screens.GalleryScreen
import net.maiatoday.tagspotter.ui.screens.MapScreen
import net.maiatoday.tagspotter.ui.viewmodel.SettingsViewModel
import net.maiatoday.tagspotter.TagSpotterApplication
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.maiatoday.tagspotter.utils.ExifLocationExtractor
import net.maiatoday.tagspotter.utils.ImageOptimizer
import net.maiatoday.tagspotter.utils.LocationHelper
import net.maiatoday.tagspotter.utils.MediaStorageHelper
import java.io.File
import java.util.UUID

enum class Tab {
    Gallery,
    Map
}

@Composable
fun MainContainer(
    onSpotClick: (Long) -> Unit,
    onPhotoCaptured: (String, String, Double, Double, Boolean, String, Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Gallery) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            (LocalContext.current.applicationContext as TagSpotterApplication).settingsRepository,
            (LocalContext.current.applicationContext as TagSpotterApplication).repository
        )
    )

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isLoading by rememberSaveable { mutableStateOf(false) }

    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launcher for GPS Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission) ||
                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
    }

    var tempPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var tempPhotoFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    // Take Picture Launcher (Native Camera Intent)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uriStr = tempPhotoUri
            val pathStr = tempPhotoFilePath
            if (uriStr != null && pathStr != null) {
                val file = File(pathStr)
                isLoading = true
                scope.launch(Dispatchers.Default) {
                    var lat = 0.0
                    var lng = 0.0
                    var isFallback = true

                    if (hasLocationPermission) {
                        val currentLoc = LocationHelper.getCurrentLocation(context)
                        if (currentLoc != null) {
                            lat = currentLoc.latitude
                            lng = currentLoc.longitude
                            isFallback = currentLoc.isFallback
                        }
                    }

                    // Save original to public MediaStore gallery
                    val publicUri = MediaStorageHelper.saveImageToPublicGallery(context, file)
                    // Create thumbnail
                    val thumbnailPath = ImageOptimizer.createThumbnail(context, file)
                    
                    // Clean temp cache file
                    try { file.delete() } catch (_: Exception) {}

                    if (publicUri != null && thumbnailPath != null) {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            onPhotoCaptured(publicUri.toString(), thumbnailPath, lat, lng, isFallback, "All", null)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, "Error saving captured photo.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Photo Gallery Picker Launcher (Native Visual Media Contract)
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            scope.launch(Dispatchers.Default) {
                val exifMeta = ExifLocationExtractor.getPhotoMetadata(context, uri)
                var lat = 0.0
                var lng = 0.0
                var isFallback = true
                var captureTime: Long? = null

                if (exifMeta != null) {
                    if (exifMeta.latitude != null && exifMeta.longitude != null) {
                        lat = exifMeta.latitude
                        lng = exifMeta.longitude
                        isFallback = false
                    } else if (hasLocationPermission) {
                        val currentLoc = LocationHelper.getCurrentLocation(context)
                        if (currentLoc != null) {
                            lat = currentLoc.latitude
                            lng = currentLoc.longitude
                            isFallback = currentLoc.isFallback
                        }
                    }
                    captureTime = exifMeta.timestamp
                } else if (hasLocationPermission) {
                    val currentLoc = LocationHelper.getCurrentLocation(context)
                    if (currentLoc != null) {
                        lat = currentLoc.latitude
                        lng = currentLoc.longitude
                        isFallback = currentLoc.isFallback
                    }
                }

                // Create thumbnail
                val thumbnailPath = ImageOptimizer.createThumbnail(context, uri)
                if (thumbnailPath != null) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        onPhotoCaptured(uri.toString(), thumbnailPath, lat, lng, isFallback, "All", captureTime)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Toast.makeText(context, "Error processing gallery image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Permission Launcher for ACCESS_MEDIA_LOCATION (Android 10+)
    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        scope.launch {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    val triggerCamera = {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        val file = File(context.cacheDir, "cam_${UUID.randomUUID()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        try {
            val uri = FileProvider.getUriForFile(context, authority, file)
            tempPhotoUri = uri.toString()
            tempPhotoFilePath = file.absolutePath
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to launch device camera.", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerFiles = {
        val permission = Manifest.permission.ACCESS_MEDIA_LOCATION
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            mediaLocationPermissionLauncher.launch(permission)
        }
    }

    val showTestData by settingsViewModel.showTestData.collectAsStateWithLifecycle()

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
                        // Title TAGSPOTTER
                        Text(
                            text = "TAGSPOTTER",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Drawer Item: Settings
                        NavigationDrawerItem(
                            label = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Drawer Switch Item: Load Mock Test Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Load Mock Test Data",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = showTestData,
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.updateShowTestData(isChecked)
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

                    // Drawer Footer: Version
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = selectedTab == Tab.Gallery,
                        onClick = { selectedTab = Tab.Gallery },
                        icon = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Gallery",
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
                        label = { Text("Gallery") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                                    contentDescription = "Maps",
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
                        label = { Text("Maps") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { triggerCamera() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color.Gray
                            )
                        },
                        label = { Text("Camera") },
                        colors = NavigationRailItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { triggerFiles() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Files",
                                tint = Color.Gray
                            )
                        },
                        label = { Text("Files") },
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
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == Tab.Gallery,
                            onClick = { selectedTab = Tab.Gallery },
                            icon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery",
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
                            label = { Text("Gallery") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                                        contentDescription = "Map",
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
                            label = { Text("Maps") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { triggerCamera() },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = Color.Gray
                                )
                            },
                            label = { Text("Camera") },
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { triggerFiles() },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Files",
                                    tint = Color.Gray
                                )
                            },
                            label = { Text("Files") },
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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        enabled = true,
                        onClick = {} // intercept clicks
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
                        text = "Processing image...",
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
