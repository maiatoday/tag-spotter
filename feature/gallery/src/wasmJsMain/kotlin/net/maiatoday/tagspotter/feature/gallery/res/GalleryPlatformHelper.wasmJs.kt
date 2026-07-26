package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.database.WasmSpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun webTriggerFileInput(
    onSuccess: (String, String) -> Unit,
    onError: (String) -> Unit
): Unit = js("window.webTriggerFileInput(onSuccess, onError)")

class WasmJsGalleryPlatformHelper : GalleryPlatformHelper, KoinComponent {
    private val repository: SpotRepository by inject()

    override fun showToast(message: String) {
        println("Toast: $message")
    }

    @Composable
    override fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit {
        return remember { { onExportReady("web_export") } }
    }

    override fun exportPack(
        uriString: String,
        spots: List<SpotDetails>,
        minRating: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            (repository as? WasmSpotRepository)?.exportPackData()
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun getRoute(spots: List<SpotDetails>) {
        if (spots.isEmpty()) return
        val destinationSpot = spots.last().spot
        val waypointSpots = spots.dropLast(1)
        val base = "https://www.google.com/maps/dir/?api=1"
        val destParam = "&destination=${destinationSpot.latitude},${destinationSpot.longitude}"
        val waypointsParam = if (waypointSpots.isNotEmpty()) {
            "&waypoints=" + waypointSpots.joinToString("|") { "${it.spot.latitude},${it.spot.longitude}" }
        } else {
            ""
        }
        val travelModeParam = "&travelmode=walking"
        val url = base + destParam + waypointsParam + travelModeParam
        kotlinx.browser.window.open(url, "_blank")
    }

    override fun shareKml(spots: List<SpotDetails>) {}

    @Composable
    override fun rememberImportLauncher(
        onPackPicked: (pathString: String) -> Unit
    ): () -> Unit {
        return remember {
            {
                webTriggerFileInput(
                    onSuccess = { spotsJson, imagesJsonMap ->
                        (repository as? WasmSpotRepository)?.importPackData(spotsJson, imagesJsonMap)
                        onPackPicked("web_imported")
                    },
                    onError = { errMsg ->
                        println("Import error: $errMsg")
                    }
                )
            }
        }
    }

    override fun getFilesDir(): String = ""
    override fun getCacheDir(): String = ""
}

@Composable
actual fun rememberGalleryPlatformHelper(): GalleryPlatformHelper {
    return remember { WasmJsGalleryPlatformHelper() }
}
