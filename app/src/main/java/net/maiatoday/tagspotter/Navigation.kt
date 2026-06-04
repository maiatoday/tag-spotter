package net.maiatoday.tagspotter

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import net.maiatoday.tagspotter.ui.DetailScreen
import net.maiatoday.tagspotter.ui.MainContainer
import net.maiatoday.tagspotter.ui.TaggingScreen

import androidx.compose.runtime.LaunchedEffect

@Composable
fun MainNavigation(
    initialSpotId: Long? = null,
    onNavigateToSpotHandled: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(Main)

    LaunchedEffect(initialSpotId) {
        if (initialSpotId != null) {
            if (backStack.none { it is DetailKey && it.spotId == initialSpotId }) {
                backStack.add(DetailKey(initialSpotId))
            }
            onNavigateToSpotHandled()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<Main> {
                MainContainer(
                    onSpotClick = { spotId -> backStack.add(DetailKey(spotId)) },
                    onPhotoCaptured = { imagePath, thumbnailPath, lat, lng, isFallback, defaultCategory, captureTime ->
                        backStack.add(TaggingKey(imagePath, thumbnailPath, lat, lng, isFallback, defaultCategory, captureTime))
                    }
                )
            }
            entry<DetailKey> { key ->
                DetailScreen(
                    spotId = key.spotId,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
            entry<TaggingKey> { key ->
                TaggingScreen(
                    imagePath = key.imagePath,
                    thumbnailPath = key.thumbnailPath,
                    latitude = key.latitude,
                    longitude = key.longitude,
                    isFallback = key.isFallback,
                    defaultCategory = key.defaultCategory,
                    captureTime = key.captureTime,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
        }
    )
}
