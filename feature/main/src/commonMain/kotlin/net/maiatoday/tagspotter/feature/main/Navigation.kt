package net.maiatoday.tagspotter.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    initialTab: Tab? = null,
    initialTabRequestId: Long = 0L,
    onNavigateToSpotHandled: () -> Unit = {},
    onInitialTabHandled: () -> Unit = {},
    onTriggerFiles: (onPhotoPicked: (String) -> Unit) -> Unit,
    versionName: String,
    showToast: (String) -> Unit,
    onGoogleSignInClick: (() -> Unit)? = null,
    tagAlongContent: (@Composable (
        onOpenSpotDetail: (Long) -> Unit,
        onViewOnMainMap: (Double, Double) -> Unit,
        onMenuClick: () -> Unit
    ) -> Unit)? = null,
    tagAlongDrawerContent: (@Composable (closeDrawer: () -> Unit) -> Unit)? = null
) {
    val defaultTab = if (tagAlongContent != null) Tab.TagAlong else Tab.Gallery
    val resolvedInitialTab = when {
        initialTab == Tab.TagAlong && tagAlongContent == null -> Tab.Gallery
        initialTab != null -> initialTab
        else -> defaultTab
    }
    var selectedTab by rememberSaveable { mutableStateOf(resolvedInitialTab) }

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

    LaunchedEffect(initialTab, initialTabRequestId) {
        if (initialTab != null) {
            selectedTab = if (initialTab == Tab.TagAlong && tagAlongContent == null) {
                Tab.Gallery
            } else {
                initialTab
            }
            while (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
            onInitialTabHandled()
        }
    }

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
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onSpotClick = { spotId -> backStack.add(DetailKey(spotId)) },
                    onPhotoCaptured = { imagePath, thumbnailPath, lat, lng, isFallback, defaultCategory, captureTime ->
                        backStack.add(TaggingKey(imagePath, thumbnailPath, lat, lng, isFallback, defaultCategory, captureTime))
                    },
                    onNavigateToSettings = { backStack.add(SettingsKey) },
                    onTriggerFiles = onTriggerFiles,
                    versionName = versionName,
                    showToast = showToast,
                    tagAlongContent = tagAlongContent,
                    tagAlongDrawerContent = tagAlongDrawerContent
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
