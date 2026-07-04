package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails
import kotlin.coroutines.resume

// External JS helper declarations for Firestore & Storage
external fun webFirestoreSaveSpot(
    userId: String,
    spotUuid: String,
    spotJson: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

external fun webFirestoreListenSpots(
    userId: String,
    onUpdate: (spotJson: String) -> Unit,
    onNoChange: () -> Unit
)

external fun webStorageUploadThumbnail(
    userId: String,
    imageUuid: String,
    base64Data: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

class WasmSyncManager(
    private val repository: SpotRepository
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var activeUserId: String? = null
    private var isListening = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun syncNow() {
        val userId = activeUserId ?: return
        if (_isSyncing.value) return
        _isSyncing.value = true

        try {
            // 1. Push stage: find all unsynced local spots
            val unsyncedSpots = repository.getUnsyncedSpots()
            unsyncedSpots.forEach { localDetail ->
                val uuid = localDetail.spot.uuid
                val jsonString = Json.encodeToString(SpotDetails.serializer(), localDetail)

                // Save metadata to Firestore
                val saveSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                    webFirestoreSaveSpot(
                        userId = userId,
                        spotUuid = uuid,
                        spotJson = jsonString,
                        onSuccess = { if (continuation.isActive) continuation.resume(true) },
                        onFailure = { err ->
                            println("Web Firestore save failed for $uuid: $err")
                            if (continuation.isActive) continuation.resume(false)
                        }
                    )
                }

                if (saveSuccess) {
                    // Upload thumbnails if any and local
                    localDetail.images.forEach { image ->
                        if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                            val base64Data = if (image.thumbnailPath.startsWith("data:")) {
                                image.thumbnailPath.substringAfter(",")
                            } else {
                                image.thumbnailPath
                            }
                            suspendCancellableCoroutine<Unit> { continuation ->
                                webStorageUploadThumbnail(
                                    userId = userId,
                                    imageUuid = image.uuid,
                                    base64Data = base64Data,
                                    onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                                    onFailure = { err ->
                                        println("Web Storage upload failed for ${image.uuid}: $err")
                                        if (continuation.isActive) continuation.resume(Unit)
                                    }
                                )
                            }
                        }
                    }

                    // Mark as successfully synced locally
                    repository.markSpotAsSynced(uuid)
                }
            }

        } catch (e: Exception) {
            println("Error during Wasm syncNow: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    override fun startRealtimeSync(userId: String) {
        activeUserId = userId
        if (isListening) return
        isListening = true

        try {
            webFirestoreListenSpots(
                userId = userId,
                onUpdate = { spotJson ->
                    coroutineScope.launch {
                        try {
                            val cloudDetail = Json.decodeFromString(SpotDetails.serializer(), spotJson)
                            val localSpots = repository.getAllSpots().first()
                            val localMatch = localSpots.find { it.spot.uuid == cloudDetail.spot.uuid }

                            if (localMatch == null) {
                                repository.saveSyncedSpot(cloudDetail)
                            } else if (cloudDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt) {
                                repository.saveSyncedSpot(cloudDetail)
                            }
                        } catch (e: Exception) {
                            println("Error parsing received web spot JSON: ${e.message}")
                        }
                    }
                },
                onNoChange = {}
            )
        } catch (e: Exception) {
            println("Error starting web realtime sync: ${e.message}")
        }

        // Trigger immediate incremental background sync
        coroutineScope.launch {
            syncNow()
        }
    }

    override fun stopRealtimeSync() {
        activeUserId = null
        isListening = false
    }
}
