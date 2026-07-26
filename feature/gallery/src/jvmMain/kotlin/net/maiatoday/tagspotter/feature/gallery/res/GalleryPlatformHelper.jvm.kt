package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.maiatoday.tagspotter.core.database.PackManager
import net.maiatoday.tagspotter.core.model.SpotDetails
import java.io.File

class JvmGalleryPlatformHelper : GalleryPlatformHelper {
    override fun showToast(message: String) {
        println("Toast: $message")
    }

    @Composable
    override fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit {
        return remember {
            {
                val fileDialog = java.awt.FileDialog(
                    null as java.awt.Frame?,
                    "Save TagSpotter Pack",
                    java.awt.FileDialog.SAVE
                )
                fileDialog.file = "spots_export.ts_pack"
                fileDialog.isVisible = true
                val directory = fileDialog.directory
                val file = fileDialog.file
                if (directory != null && file != null) {
                    val destinationFile = File(directory, file)
                    onExportReady(destinationFile.absolutePath)
                }
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
        try {
            val file = File(uriString)
            file.outputStream().use { fos ->
                PackManager.exportPack(spots, fos, minRating)
            }
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
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
            }
        } catch (e: Exception) {
            println("Failed to open browser for route: ${e.message}")
        }
    }

    override fun shareKml(spots: List<SpotDetails>) {
        println("Sharing KML on JVM. Spot count: ${spots.size}")
    }

    @Composable
    override fun rememberImportLauncher(
        onPackPicked: (pathString: String) -> Unit
    ): () -> Unit {
        return remember {
            {
                val fileDialog = java.awt.FileDialog(
                    null as java.awt.Frame?,
                    "Select TagSpotter Pack",
                    java.awt.FileDialog.LOAD
                )
                fileDialog.setFilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".ts_pack") || lower.endsWith(".zip")
                }
                fileDialog.isVisible = true
                val directory = fileDialog.directory
                val file = fileDialog.file
                if (directory != null && file != null) {
                    val selectedFile = File(directory, file)
                    onPackPicked(selectedFile.absolutePath)
                }
            }
        }
    }

    override fun getFilesDir(): String {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".tagspotter")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir.absolutePath
    }

    override fun getCacheDir(): String {
        return System.getProperty("java.io.tmpdir")
    }
}

@Composable
actual fun rememberGalleryPlatformHelper(): GalleryPlatformHelper {
    return remember { JvmGalleryPlatformHelper() }
}
