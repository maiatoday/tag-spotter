package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import net.maiatoday.tagspotter.core.model.SpotDetails

interface GalleryPlatformHelper {
    fun showToast(message: String)
    @Composable
    fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit // Returns a lambda to launch the document creation / export flow
    
    fun exportPack(
        uriString: String,
        spots: List<SpotDetails>,
        minRating: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    )
    
    fun getRoute(spots: List<SpotDetails>)
    
    fun shareKml(spots: List<SpotDetails>)

    @Composable
    fun rememberImportLauncher(
        onPackPicked: (pathString: String) -> Unit
    ): () -> Unit

    fun getFilesDir(): String
    fun getCacheDir(): String
}

@Composable
expect fun rememberGalleryPlatformHelper(): GalleryPlatformHelper
