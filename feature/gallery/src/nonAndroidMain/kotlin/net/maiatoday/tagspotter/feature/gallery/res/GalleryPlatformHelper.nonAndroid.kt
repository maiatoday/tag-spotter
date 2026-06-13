package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.maiatoday.tagspotter.core.model.SpotDetails

class NonAndroidGalleryPlatformHelper : GalleryPlatformHelper {
    override fun showToast(message: String) {
        println("Toast: $message")
    }

    @Composable
    override fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit {
        return remember {
            {
                onExportReady("dummy_uri")
            }
        }
    }

    override fun exportPack(
        uriString: String,
        spots: List<SpotDetails>,
        minRating: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        println("Exporting pack on non-Android platform. Spot count: ${spots.size}")
        onSuccess()
    }

    override fun getRoute(spots: List<SpotDetails>) {
        println("Routing on non-Android platform. Spot count: ${spots.size}")
    }

    override fun shareKml(spots: List<SpotDetails>) {
        println("Sharing KML on non-Android platform. Spot count: ${spots.size}")
    }
}

@Composable
actual fun rememberGalleryPlatformHelper(): GalleryPlatformHelper {
    return remember { NonAndroidGalleryPlatformHelper() }
}
