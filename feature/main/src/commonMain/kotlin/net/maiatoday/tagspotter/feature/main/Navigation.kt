package net.maiatoday.tagspotter.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import net.maiatoday.tagspotter.feature.detail.DetailScreen
import net.maiatoday.tagspotter.feature.settings.SettingsScreen

@Composable
fun MainNavigation(
    initialSpotId: Long? = null,
    onNavigateToSpotHandled: () -> Unit = {},
    onTriggerFiles: (onPhotoPicked: (String) -> Unit) -> Unit,
    versionName: String,
    showToast: (String) -> Unit,
    onGoogleSignInClick: (() -> Unit)? = null
) {
    val config = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Main::class, Main.serializer())
                    subclass(DetailKey::class, DetailKey.serializer())
                    subclass(TaggingKey::class, TaggingKey.serializer())
                    subclass(SettingsKey::class, SettingsKey.serializer())
                }
            }
        }
    }

    val backStack = rememberNavBackStack(
        config,
        Main
    )

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
                    },
                    onNavigateToSettings = { backStack.add(SettingsKey) },
                    onTriggerFiles = onTriggerFiles,
                    versionName = versionName,
                    showToast = showToast
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
                DetailScreen(
                    spotId = -1L,
                    draftImagePath = key.imagePath,
                    draftThumbnailPath = key.thumbnailPath,
                    draftLatitude = key.latitude,
                    draftLongitude = key.longitude,
                    draftIsFallback = key.isFallback,
                    draftDefaultCategory = key.defaultCategory,
                    draftCaptureTime = key.captureTime,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
            entry<SettingsKey> {
                SettingsScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    },
                    onGoogleSignInClick = onGoogleSignInClick
                )
            }
        }
    )
}
