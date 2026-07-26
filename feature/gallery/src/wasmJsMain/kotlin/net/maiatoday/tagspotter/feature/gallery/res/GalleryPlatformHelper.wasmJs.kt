package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.maiatoday.tagspotter.core.model.SpotDetails

class WasmJsGalleryPlatformHelper : GalleryPlatformHelper {
    override fun showToast(message: String) {
        println("Toast: $message")
    }

    @Composable
    override fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit {
        return remember { { onExportReady("dummy") } }
    }

    override fun exportPack(
        uriString: String,
        spots: List<SpotDetails>,
        minRating: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        onSuccess()
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
        return remember { {} }
    }

    override fun getFilesDir(): String = ""
    override fun getCacheDir(): String = ""
}

@Composable
actual fun rememberGalleryPlatformHelper(): GalleryPlatformHelper {
    return remember { WasmJsGalleryPlatformHelper() }
}
