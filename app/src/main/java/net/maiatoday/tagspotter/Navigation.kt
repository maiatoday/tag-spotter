package net.maiatoday.tagspotter

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import net.maiatoday.tagspotter.ui.DetailScreen
import net.maiatoday.tagspotter.ui.MainContainer
import net.maiatoday.tagspotter.ui.TaggingScreen

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
                    onPhotoCaptured = { imagePath, thumbnailPath, lat, lng, isFallback, defaultCategory, captureTime ->
                        backStack.add(TaggingKey(imagePath, thumbnailPath, lat, lng, isFallback, defaultCategory, captureTime))
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
                    thumbnailPath = key.thumbnailPath,
                    latitude = key.latitude,
                    longitude = key.longitude,
                    isFallback = key.isFallback,
                    defaultCategory = key.defaultCategory,
                    captureTime = key.captureTime,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
