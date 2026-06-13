package net.maiatoday.tagspotter.feature.gallery.res

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import net.maiatoday.tagspotter.core.database.PackManager
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.feature.gallery.KmlExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidGalleryPlatformHelper(
    private val context: Context
) : GalleryPlatformHelper {

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @Composable
    override fun rememberLauncher(
        onExportReady: (uriString: String) -> Unit
    ): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("*/*")
        ) { uri ->
            if (uri != null) {
                onExportReady(uri.toString())
            }
        }
        return remember(launcher) {
            { launcher.launch("spots_export.ts_pack") }
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
            val uri = uriString.toUri()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                PackManager.exportPack(
                    context,
                    spots,
                    outputStream,
                    minRating = minRating
                )
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun getRoute(spots: List<SpotDetails>) {
        if (spots.isNotEmpty()) {
            val destinationSpot = spots.last().spot
            val waypointSpots = spots.dropLast(1)
            val base = "https://www.google.com/maps/dir/?api=1"
            val destParam = "&destination=${destinationSpot.latitude},${destinationSpot.longitude}"
            val waypointsParam = if (waypointSpots.isNotEmpty()) {
                "&waypoints=" + waypointSpots.joinToString("|") { "${it.spot.latitude},${it.spot.longitude}" }
            } else {
                ""
            }
            val url = base + destParam + waypointsParam
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                showToast("No app available to open route.")
            }
        }
    }

    override fun shareKml(spots: List<SpotDetails>) {
        if (spots.isNotEmpty()) {
            val kmlString = KmlExporter.generateKml(spots)
            try {
                val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                val timestamp = sdf.format(Date())
                val filename = "spots_export_$timestamp.kml"
                val cacheFile = File(context.cacheDir, filename)
                cacheFile.writeText(kmlString)
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, cacheFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.google-earth.kml+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "TagSpotter KML Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share KML"))
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to share KML: ${e.localizedMessage}")
            }
        }
    }
}

@Composable
actual fun rememberGalleryPlatformHelper(): GalleryPlatformHelper {
    val context = LocalContext.current
    return remember(context) { AndroidGalleryPlatformHelper(context) }
}
