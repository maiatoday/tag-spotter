package net.maiatoday.tagspotter.core.location

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.feature.main.MainActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WearCompanionListenerService : WearableListenerService(), KoinComponent {

    private val repository: SpotRepository by inject()
    private val wearSyncManager: WearSyncManager by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("WearCompanionService", "onMessageReceived: path=${messageEvent.path}")

        when (messageEvent.path) {
            "/query_nearby_spots" -> {
                Log.d("WearCompanionService", "Starting WearLocationForegroundService for nearby spots query")
                // Start the foreground service to retrieve the location and reply to the watch
                val intent = Intent(this, WearLocationForegroundService::class.java).apply {
                    putExtra("EXTRA_WATCH_NODE_ID", messageEvent.sourceNodeId)
                }
                startForegroundService(intent)
            }
            "/open_on_phone" -> {
                try {
                    val payload = String(messageEvent.data, Charsets.UTF_8)
                    Log.d("WearCompanionService", "open_on_phone payload=$payload")
                    val spotId = payload.toLongOrNull()
                    if (spotId != null) {
                        Log.d("WearCompanionService", "Launching MainActivity with spotId=$spotId")
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("EXTRA_SPOT_ID", spotId)
                        }
                        startActivity(intent)
                        Log.d("WearCompanionService", "startActivity called successfully")
                    } else {
                        Log.w("WearCompanionService", "spotId could not be parsed to Long")
                    }
                } catch (e: Exception) {
                    Log.e("WearCompanionService", "Error starting MainActivity from WearCompanionListenerService", e)
                    e.printStackTrace()
                }
            }
            "/request_spot_photo" -> {
                try {
                    val payload = String(messageEvent.data, Charsets.UTF_8)
                    val spotId = payload.toLongOrNull()
                    if (spotId != null) {
                        Log.d("WearCompanionService", "Received request for spot photo, spotId=$spotId")
                        serviceScope.launch {
                            val details = repository.getSpotById(spotId).first()
                            if (details != null) {
                                val mainImage = details.images.firstOrNull { it.isMain } ?: details.images.firstOrNull()
                                if (mainImage != null) {
                                    wearSyncManager.sendSpotPhoto(spotId, mainImage.imagePath)
                                } else {
                                    Log.d("WearCompanionService", "No main image for spotId=$spotId")
                                }
                            } else {
                                Log.d("WearCompanionService", "No spot details found for spotId=$spotId")
                            }
                        }
                    } else {
                        Log.w("WearCompanionService", "Invalid spotId for request_spot_photo: $payload")
                    }
                } catch (e: Exception) {
                    Log.e("WearCompanionService", "Error handling /request_spot_photo", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}