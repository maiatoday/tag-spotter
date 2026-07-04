package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails

class NonWebSyncManager(
    private val repository: SpotRepository
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private var realtimeJob: Job? = null
    private var activeUserId: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun syncNow() {
        val userId = activeUserId ?: return
        if (_isSyncing.value) return
        _isSyncing.value = true

        try {
            // 1. Push stage: find all unsynced local spots
            val unsyncedSpots = repository.getUnsyncedSpots()
            val spotsCollection = firestore.collection("users").document(userId).collection("spots")

            unsyncedSpots.forEach { localDetail ->
                val uuid = localDetail.spot.uuid
                
                // Upload local metadata to Firestore
                spotsCollection.document(uuid).set(SpotDetails.serializer(), localDetail)

                // Upload thumbnail attachments if any and if local
                localDetail.images.forEach { image ->
                    if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                        try {
                            val bytes = readBytesFromFile(image.thumbnailPath)
                            if (bytes != null) {
                                val storageRef = storage.reference("users/$userId/thumbnails/${image.uuid}.jpg")
                                storageRef.putData(bytes.toFirebaseStorageData())
                            }
                        } catch (e: Exception) {
                            println("Failed to upload thumbnail ${image.uuid}: ${e.message}")
                        }
                    }
                }

                // Mark as successfully synced locally
                repository.markSpotAsSynced(uuid)
            }

            // 2. Pull stage: fetch remote spots and apply Last-Write-Wins
            val querySnapshot = spotsCollection.get()
            val localSpots = repository.getAllSpots().first()

            querySnapshot.documents.forEach { doc ->
                val cloudDetail = doc.data(SpotDetails.serializer())
                val localMatch = localSpots.find { it.spot.uuid == cloudDetail.spot.uuid }

                if (localMatch == null) {
                    repository.saveSyncedSpot(cloudDetail)
                } else if (cloudDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt) {
                    repository.saveSyncedSpot(cloudDetail)
                }
            }

        } catch (e: Exception) {
            println("Error during syncNow: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    override fun startRealtimeSync(userId: String) {
        activeUserId = userId
        realtimeJob?.cancel()

        realtimeJob = coroutineScope.launch {
            try {
                firestore.collection("users").document(userId).collection("spots")
                    .snapshots
                    .collect { querySnapshot ->
                        val localSpots = repository.getAllSpots().first()
                        querySnapshot.documents.forEach { doc ->
                            try {
                                val cloudDetail = doc.data(SpotDetails.serializer())
                                val localMatch = localSpots.find { it.spot.uuid == cloudDetail.spot.uuid }

                                if (localMatch == null) {
                                    repository.saveSyncedSpot(cloudDetail)
                                } else if (cloudDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt) {
                                    repository.saveSyncedSpot(cloudDetail)
                                }
                            } catch (e: Exception) {
                                println("Error parsing real-time spot document: ${e.message}")
                            }
                        }
                    }
            } catch (e: Exception) {
                println("Error in realtime sync stream: ${e.message}")
            }
        }

        // Trigger an initial immediate incremental sync in background
        coroutineScope.launch {
            syncNow()
        }
    }

    override fun stopRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        activeUserId = null
    }
}
