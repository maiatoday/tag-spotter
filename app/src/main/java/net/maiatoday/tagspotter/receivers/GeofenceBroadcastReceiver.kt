package net.maiatoday.tagspotter.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.MainActivity
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.data.SpotDetails

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent has error or is null")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val triggeringLocation = geofencingEvent.triggeringLocation

            if (!triggeringGeofences.isNullOrEmpty()) {
                val pendingResult = goAsync()
                val repository = (context.applicationContext as TagSpotterApplication).repository
                val settingsRepository = (context.applicationContext as TagSpotterApplication).settingsRepository

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val notificationsEnabled = settingsRepository.notificationsEnabled.first()
                        if (!notificationsEnabled) {
                            Log.d("GeofenceReceiver", "Proximity notifications are disabled in settings. Skipping.")
                            return@launch
                        }
                        triggeringGeofences.forEach { geofence ->
                            val spotId = geofence.requestId.toLongOrNull()
                            if (spotId != null) {
                                val spotDetails = repository.getSpotById(spotId).first()
                                if (spotDetails != null) {
                                    val bitmap = loadThumbnail(context, spotDetails)
                                    val distance = triggeringLocation?.let { currentLoc ->
                                        val spotLoc = Location("").apply {
                                            latitude = spotDetails.spot.latitude
                                            longitude = spotDetails.spot.longitude
                                        }
                                        currentLoc.distanceTo(spotLoc).toInt()
                                    } ?: 100

                                    showNotification(context, spotDetails, distance, bitmap)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("GeofenceReceiver", "Error processing geofence trigger", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
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
            Log.e("GeofenceReceiver", "Error loading notification bitmap", e)
            null
        }
    }

    private fun showNotification(context: Context, details: SpotDetails, distance: Int, bitmap: Bitmap?) {
        val channelId = "starred_spots_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Starred Spots Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when you are walking near starred street art."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

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

        val notification = NotificationCompat.Builder(context, channelId)
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
            .build()

        notificationManager.notify(details.spot.id.toInt(), notification)
    }
}
