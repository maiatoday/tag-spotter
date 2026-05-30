package com.example.tagspotter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.tagspotter.ui.screens.CameraScreen
import com.example.tagspotter.ui.screens.GalleryScreen
import com.example.tagspotter.ui.screens.MapScreen

enum class Tab {
    Gallery,
    Map,
    Camera
}

@Composable
fun MainContainer(
    onSpotClick: (Long) -> Unit,
    onPhotoCaptured: (String, Double, Double, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Gallery) }
    var currentCategory by rememberSaveable { mutableStateOf("All") }

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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                Tab.Gallery -> GalleryScreen(
                    selectedCategory = currentCategory,
                    onCategoryChange = { currentCategory = it },
                    onSpotClick = onSpotClick
                )
                Tab.Map -> MapScreen(
                    selectedCategory = currentCategory,
                    onSpotClick = onSpotClick
                )
                Tab.Camera -> CameraScreen(
                    onPhotoCaptured = { path, lat, lng, isFallback ->
                        onPhotoCaptured(path, lat, lng, isFallback, currentCategory)
                    }
                )
            }
        }
    }
}
