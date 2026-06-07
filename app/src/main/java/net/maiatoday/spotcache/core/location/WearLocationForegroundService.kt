package net.maiatoday.spotcache.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.maiatoday.spotcache.core.database.SpotRepository
import net.maiatoday.spotcache.core.model.LocationUtils
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
        
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )

        serviceScope.launch {
            try {
                // 1. Get current location
                val location = LocationHelper.getCurrentLocation(applicationContext)
                val responseBytes = if (location != null) {
                    // 2. Fetch all spots and filter/sort by proximity
                    val spotDetailsList = repository.getAllSpots().first()
                    
                    // Sort by distance and filter those within 10 km (10000m)
                    val nearbySpots = spotDetailsList
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
                        .take(10) // Limit to top 10 closest spots

                    val json = Json.encodeToString(nearbySpots)
                    json.toByteArray(Charsets.UTF_8)
                } else {
                    // Send empty list
                    "[]".toByteArray(Charsets.UTF_8)
                }

                // 3. Send back to Wear OS
                if (watchNodeId.isNotEmpty()) {
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(watchNodeId, "/nearby_spots_response", responseBytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wear Watch Location Query",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SpotCache Watch Query")
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
