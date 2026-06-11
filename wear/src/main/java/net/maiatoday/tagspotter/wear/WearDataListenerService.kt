package net.maiatoday.tagspotter.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService

class WearDataListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("WearDataListenerService", "onMessageReceived: path=${messageEvent.path}")

        if (messageEvent.path == "/nearby_spots_response") {
            try {
                val json = String(messageEvent.data, Charsets.UTF_8)
                Log.d("WearDataListenerService", "Received nearby spots response: $json")
                
                // Cache the spots JSON in SharedPreferences
                val sharedPref = applicationContext.getSharedPreferences("tagspotter_wear_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("cached_spots_json", json)
                    apply()
                }
                
                // Trigger a Tile refresh
                Log.d("WearDataListenerService", "Requesting tile update for StarredSpotsTileService")
                TileService.getUpdater(applicationContext)
                    .requestUpdate(StarredSpotsTileService::class.java)
                    
            } catch (e: Exception) {
                Log.e("WearDataListenerService", "Error caching spots and updating tile", e)
                e.printStackTrace()
            }
        }
    }
}
