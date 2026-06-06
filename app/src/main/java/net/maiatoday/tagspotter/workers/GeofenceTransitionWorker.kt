package net.maiatoday.tagspotter.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import net.maiatoday.tagspotter.MainActivity
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceTransitionWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: SpotRepository by inject()
    private val settingsRepository: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        val spotIds = inputData.getStringArray("spot_ids") ?: return Result.failure()
        val triggeringLat = inputData.getDouble("latitude", Double.NaN)
        val triggeringLng = inputData.getDouble("longitude", Double.NaN)

        return try {
            val notificationsEnabled = settingsRepository.notificationsEnabled.first()
            if (!notificationsEnabled) {
                Log.d("GeofenceTransitionWorker", "Proximity notifications are disabled in settings. Skipping.")
                return Result.success()
            }

            for (idString in spotIds) {
                val spotId = idString.toLongOrNull() ?: continue
                val spotDetails = repository.getSpotById(spotId).first()
                if (spotDetails != null) {
                    val bitmap = loadThumbnail(context, spotDetails)
                    val distance = if (!triggeringLat.isNaN() && !triggeringLng.isNaN()) {
                        val currentLoc = Location("").apply {
                            latitude = triggeringLat
                            longitude = triggeringLng
                        }
                        val spotLoc = Location("").apply {
                            latitude = spotDetails.spot.latitude
                            longitude = spotDetails.spot.longitude
                        }
                        currentLoc.distanceTo(spotLoc).toInt()
                    } else {
                        100
                    }

                    showNotification(context, spotDetails, distance, bitmap)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("GeofenceTransitionWorker", "Error processing geofence transition", e)
            Result.failure()
        }
    }

    private suspend fun loadThumbnail(context: Context, details: SpotDetails): Bitmap? {
        val latestImage = details.images.firstOrNull { it.isMain } ?: details.images.maxByOrNull { it.timestamp } ?: return null
        val path = latestImage.thumbnailPath.ifEmpty { latestImage.imagePath }
        if (path.isEmpty()) return null

        return try {
            val request = ImageRequest.Builder(context)
                .data(path)
                .allowHardware(false) // software bitmap is required for notification large icon
                .build()
            val result = context.imageLoader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e("GeofenceTransitionWorker", "Error loading notification bitmap", e)
            null
        }
    }

    companion object {
        @androidx.annotation.VisibleForTesting
        internal fun showNotification(context: Context, details: SpotDetails, distance: Int, bitmap: Bitmap?) {
            val channelId = "starred_spots_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                channelId,
                "Starred Spots Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when you are walking near starred street art."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)

            // Tap notification to open spot in app
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_SPOT_ID", details.spot.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                details.spot.id.toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val artistText = if (details.spot.artists.isNotEmpty()) {
                "by ${details.spot.artists.joinToString(", ")}"
            } else {
                "Unknown artist"
            }

            // Wear OS extender
            val wearableExtender = NotificationCompat.WearableExtender()
                .setHintContentIntentLaunchesActivity(true)

            val contentText = "${details.spot.category.replace("_", " ").uppercase()} ($distance meters away) $artistText"

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_map) // Default system map icon, or clean outline pin icon
                .setContentTitle("Nearby Starred Spot!")
                .setContentText(contentText)
                .setSubText(details.spot.description.ifEmpty { "Starred Spot Nearby" })
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .extend(wearableExtender)

            if (bitmap != null) {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setBigContentTitle("Nearby Starred Spot!")
                        .setSummaryText(contentText)
                )
            }

            val notification = builder.build()
            notificationManager.notify(details.spot.id.toInt(), notification)
        }
    }
}
