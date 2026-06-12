package net.maiatoday.tagspotter.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.LocationUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WearLocationForegroundService : Service(), KoinComponent {

    private val repository: SpotRepository by inject()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val watchNodeId = intent?.getStringExtra("EXTRA_WATCH_NODE_ID") ?: ""
        Log.d("WearLocationService", "Watch Node ID: $watchNodeId") // Add this line

        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        serviceScope.launch {
            try {
                Log.d("WearLocationService", "Starting location query...")
                // 1. Get current location
                val location = LocationHelper.getCurrentLocation(applicationContext)
                Log.d("WearLocationService", "Location received: $location")
                val responseBytes = if (location != null) {
                    // 2. Fetch all spots and filter/sort by proximity
                    val spotDetailsList = repository.getAllSpots().first()
                    Log.d("WearLocationService", "Fetched all spots from repo: ${spotDetailsList.size}")
                    
                    // Sort by distance and filter those within 10 km (10000m) and are starred
                    val nearbySpots = spotDetailsList
                        .asSequence()
                        .filter { it.spot.isStarred }
                        .map { details ->
                            val dist = LocationUtils.calculateDistance(
                                location.latitude, location.longitude,
                                details.spot.latitude, details.spot.longitude
                            )
                            details to dist
                        }
                        .filter { it.second <= 10000.0 }
                        .sortedBy { it.second }
                        .map { it.first }
                        .take(10)
                        .toList() // Limit to top 10 closest spots

                    Log.d("WearLocationService", "Filtered nearby spots count: ${nearbySpots.size}")
                    val json = Json.encodeToString(nearbySpots)
                    json.toByteArray(Charsets.UTF_8)
                } else {
                    Log.d("WearLocationService", "Location was null, preparing empty list")
                    // Send empty list
                    "[]".toByteArray(Charsets.UTF_8)
                }

                // 3. Send back to Wear OS
                if (watchNodeId.isNotEmpty()) {
                    Log.d("WearLocationService", "Sending message back to watchNodeId: $watchNodeId")
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(watchNodeId, "/nearby_spots_response", responseBytes)
                    Log.d("WearLocationService", "Message sent back successfully")
                } else {
                    Log.d("WearLocationService", "Watch Node ID was empty, cannot send response")
                }
            } catch (e: Exception) {
                Log.e("WearLocationService", "Error querying nearby spots", e)
                e.printStackTrace()
            } finally {
                Log.d("WearLocationService", "Stopping service")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wear Watch Location Query",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TagSpotter Watch Query")
            .setContentText("Querying nearby street art spots for your watch...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "wear_location_channel"
        private const val NOTIFICATION_ID = 2001
    }
}
