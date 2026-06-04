package net.maiatoday.tagspotter.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.utils.GeofenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val repository = (context.applicationContext as TagSpotterApplication).repository
            val geofenceManager = GeofenceManager(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val starredSpots = repository.getStarredSpots()
                    if (starredSpots.isNotEmpty()) {
                        geofenceManager.recreateAllGeofences(starredSpots) { success ->
                            Log.d("BootReceiver", "Re-registered ${starredSpots.size} geofences on boot: success=$success")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to restore geofences on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
