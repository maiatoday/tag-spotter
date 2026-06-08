package net.maiatoday.tagspotter.core.location

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import net.maiatoday.tagspotter.feature.main.MainActivity

class WearCompanionListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/query_nearby_spots" -> {
                // Start the foreground service to retrieve the location and reply to the watch
                val intent = Intent(this, WearLocationForegroundService::class.java).apply {
                    putExtra("EXTRA_WATCH_NODE_ID", messageEvent.sourceNodeId)
                }
                startForegroundService(intent)
            }
            "/open_on_phone" -> {
                try {
                    val payload = String(messageEvent.data, Charsets.UTF_8)
                    val spotId = payload.toLongOrNull()
                    if (spotId != null) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("EXTRA_SPOT_ID", spotId)
                        }
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}