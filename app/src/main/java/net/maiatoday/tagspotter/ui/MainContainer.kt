package net.maiatoday.tagspotter.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import net.maiatoday.tagspotter.ui.screens.CaptureScreen
import net.maiatoday.tagspotter.ui.screens.GalleryScreen
import net.maiatoday.tagspotter.ui.screens.MapScreen
import net.maiatoday.tagspotter.ui.screens.SettingsScreen

enum class Tab {
    Gallery,
    Map,
    Camera,
    Settings
}

@Composable
fun MainContainer(
    onSpotClick: (Long) -> Unit,
    onPhotoCaptured: (String, String, Double, Double, Boolean, String, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Gallery) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                    selected = selectedTab == Tab.Gallery,
                    onClick = { selectedTab = Tab.Gallery },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("Gallery") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
                NavigationRailItem(
                    selected = selectedTab == Tab.Map,
                    onClick = { selectedTab = Tab.Map },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
                NavigationRailItem(
                    selected = selectedTab == Tab.Camera,
                    onClick = { selectedTab = Tab.Camera },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Capture") },
                    label = { Text("Capture") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
                NavigationRailItem(
                    selected = selectedTab == Tab.Settings,
                    onClick = { selectedTab = Tab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
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
                    onPhotoCaptured = { imagePath, thumbnailPath, lat, lng, isFallback, captureTime ->
                        onPhotoCaptured(imagePath, thumbnailPath, lat, lng, isFallback, "All", captureTime)
                    }
                )
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        selected = selectedTab == Tab.Gallery,
                        onClick = { selectedTab = Tab.Gallery },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
                        label = { Text("Gallery") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.Map,
                        onClick = { selectedTab = Tab.Map },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.Camera,
                        onClick = { selectedTab = Tab.Camera },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Capture") },
                        label = { Text("Capture") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.Settings,
                        onClick = { selectedTab = Tab.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
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
                    onPhotoCaptured = { imagePath, thumbnailPath, lat, lng, isFallback, captureTime ->
                        onPhotoCaptured(imagePath, thumbnailPath, lat, lng, isFallback, "All", captureTime)
                    }
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: Tab,
    onSpotClick: (Long) -> Unit,
    onPhotoCaptured: (String, String, Double, Double, Boolean, Long?) -> Unit
) {
    when (selectedTab) {
        Tab.Gallery -> GalleryScreen(
            onSpotClick = onSpotClick
        )
        Tab.Map -> MapScreen(
            onSpotClick = onSpotClick
        )
        Tab.Camera -> CaptureScreen(
            onPhotoCaptured = onPhotoCaptured
        )
        Tab.Settings -> SettingsScreen()
    }
}
