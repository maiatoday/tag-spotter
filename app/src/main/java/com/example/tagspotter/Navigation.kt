package com.example.tagspotter

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tagspotter.ui.DetailScreen
import com.example.tagspotter.ui.MainContainer
import com.example.tagspotter.ui.TaggingScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainContainer(
                    onSpotClick = { spotId -> backStack.add(DetailKey(spotId)) },
                    onPhotoCaptured = { path, lat, lng, isFallback, defaultCategory ->
                        backStack.add(TaggingKey(path, lat, lng, isFallback, defaultCategory))
                    }
                )
            }
            entry<DetailKey> { key ->
                DetailScreen(
                    spotId = key.spotId,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<TaggingKey> { key ->
                TaggingScreen(
                    imagePath = key.imagePath,
                    latitude = key.latitude,
                    longitude = key.longitude,
                    isFallback = key.isFallback,
                    defaultCategory = key.defaultCategory,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
