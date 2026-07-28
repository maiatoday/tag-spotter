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

    private val firestore by lazy {
        try {
            Firebase.firestore
        } catch (e: Exception) {
            println("Firebase Firestore not available (expected on Desktop JVM): ${e.message}")
            null
        }
    }
    private val storage by lazy {
        try {
            Firebase.storage
        } catch (e: Exception) {
            println("Firebase Storage not available (expected on Desktop JVM): ${e.message}")
            null
        }
    }
    private var realtimeJob: Job? = null
    private var activeUserId: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val TAG = "NonWebSyncManager"

    override suspend fun syncNow() {
        platformLog(TAG, "syncNow called. activeUserId: $activeUserId, isSyncing: ${_isSyncing.value}")
        val userId = activeUserId ?: run {
            platformLog(TAG, "syncNow early return: activeUserId is null")
            return
        }
        if (_isSyncing.value) {
            platformLog(TAG, "syncNow early return: already syncing")
            return
        }
        val currentFirestore = firestore
        val currentStorage = storage
        if (currentFirestore == null || currentStorage == null) {
            platformLog(TAG, "Skipping real sync on Desktop JVM (Simulating successful mock sync)")
            _isSyncing.value = true
            delay(1000)
            _isSyncing.value = false
            return
        }
        _isSyncing.value = true

        try {
            // 1. Push stage: find all unsynced local spots
            val unsyncedSpots = repository.getUnsyncedSpots()
            platformLog(TAG, "Push stage: Found ${unsyncedSpots.size} unsynced local spots")
            val spotsCollection = currentFirestore.collection("users").document(userId).collection("spots")

            unsyncedSpots.forEach { localDetail ->
                val uuid = localDetail.spot.uuid
                platformLog(TAG, "Pushing spot: $uuid")
                
                // Upload local metadata to Firestore
                spotsCollection.document(uuid).set(SpotDetails.serializer(), localDetail)

                // Upload thumbnail attachments if any and if local
                localDetail.images.forEach { image ->
                    if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                        try {
                            val bytes = readBytesFromFile(image.thumbnailPath)
                            if (bytes != null) {
                                platformLog(TAG, "Uploading thumbnail for image: ${image.uuid}")
                                val storageRef = currentStorage.reference("users/$userId/thumbnails/${image.uuid}.jpg")
                                storageRef.putData(bytes.toFirebaseStorageData())
                            }
                        } catch (e: Exception) {
                            platformLogError(TAG, "Failed to upload thumbnail ${image.uuid}", e)
                        }
                    }
                }

                // Mark as successfully synced locally
                repository.markSpotAsSynced(uuid)
                platformLog(TAG, "Marked spot as synced locally: $uuid")
            }

            // 2. Pull stage: fetch remote spots and apply Last-Write-Wins
            platformLog(TAG, "Pull stage: Fetching remote spots from Firestore")
            val querySnapshot = spotsCollection.get()
            val localSpots = repository.getAllSpots().first()
            platformLog(TAG, "Pull stage: Found ${querySnapshot.documents.size} remote spots and ${localSpots.size} local spots")

            querySnapshot.documents.forEach { doc ->
                try {
                    val cloudDetail = doc.data(SpotDetails.serializer())
                    val resolvedDetail = resolveRemoteThumbnails(userId, cloudDetail)
                    platformLog(TAG, "Processing remote spot document: ${resolvedDetail.spot.uuid}")
                    val localMatch = localSpots.find { it.spot.uuid == resolvedDetail.spot.uuid }

                    if (localMatch == null) {
                        platformLog(TAG, "Saving new remote spot locally: ${resolvedDetail.spot.uuid}")
                        repository.saveSyncedSpot(resolvedDetail)
                    } else if (resolvedDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt ||
                               resolvedDetail.images.size != localMatch.images.size ||
                               resolvedDetail.notes.size != localMatch.notes.size) {
                        platformLog(TAG, "Updating existing spot with newer remote edits or to heal missing items: ${resolvedDetail.spot.uuid}")
                        repository.saveSyncedSpot(resolvedDetail)
                    } else {
                        platformLog(TAG, "Local spot is up to date or newer: ${resolvedDetail.spot.uuid}")
                    }
                } catch (e: Exception) {
                    platformLogError(TAG, "Error parsing pulled spot document from doc ID: ${doc.id}", e)
                }
            }

        } catch (e: Exception) {
            platformLogError(TAG, "Error during syncNow", e)
        } finally {
            _isSyncing.value = false
            platformLog(TAG, "syncNow completed")
        }
    }

    override fun startRealtimeSync(userId: String) {
        platformLog(TAG, "startRealtimeSync called with userId: $userId")
        activeUserId = userId
        realtimeJob?.cancel()
        val currentFirestore = firestore
        if (currentFirestore == null) {
            platformLog(TAG, "Skipping realtime sync on Desktop JVM (Firestore unavailable)")
            return
        }

        realtimeJob = coroutineScope.launch {
            try {
                platformLog(TAG, "Subscribing to realtime snapshots for user: $userId")
                currentFirestore.collection("users").document(userId).collection("spots")
                    .snapshots
                    .collect { querySnapshot ->
                        platformLog(TAG, "Received realtime snapshot update with ${querySnapshot.documents.size} documents")
                        val localSpots = repository.getAllSpots().first()
                        val remoteUuids = mutableSetOf<String>()
                        querySnapshot.documents.forEach { doc ->
                            try {
                                val cloudDetail = doc.data(SpotDetails.serializer())
                                val resolvedDetail = resolveRemoteThumbnails(userId, cloudDetail)
                                remoteUuids.add(resolvedDetail.spot.uuid)
                                val localMatch = localSpots.find { it.spot.uuid == resolvedDetail.spot.uuid }

                                if (localMatch == null) {
                                    platformLog(TAG, "Realtime: Saving new remote spot locally: ${resolvedDetail.spot.uuid}")
                                    repository.saveSyncedSpot(resolvedDetail)
                                } else if (resolvedDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt ||
                                           resolvedDetail.images.size != localMatch.images.size ||
                                           resolvedDetail.notes.size != localMatch.notes.size ||
                                           localMatch.images.any { it.thumbnailPath != resolvedDetail.images.find { r -> r.uuid == it.uuid }?.thumbnailPath }) {
                                    platformLog(TAG, "Realtime: Updating existing spot to sync edits or heal missing items: ${resolvedDetail.spot.uuid}")
                                    repository.saveSyncedSpot(resolvedDetail)
                                }
                            } catch (e: Exception) {
                                platformLogError(TAG, "Error parsing real-time spot document from doc ID: ${doc.id}", e)
                            }
                        }

                        // Remove local synced spots that were deleted remotely
                        localSpots.filter { it.spot.ownerUid == userId && it.spot.isSynced }.forEach { localSpot ->
                            if (localSpot.spot.uuid !in remoteUuids) {
                                platformLog(TAG, "Spot ${localSpot.spot.uuid} deleted remotely; removing locally.")
                                repository.deleteSpot(localSpot)
                            }
                        }
                    }
            } catch (e: Exception) {
                platformLogError(TAG, "Error in realtime sync stream", e)
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

    override suspend fun deleteSpot(uuid: String) {
        val userId = activeUserId ?: return
        try {
            firestore?.collection("users")?.document(userId)?.collection("spots")?.document(uuid)?.delete()
            platformLog(TAG, "Deleted remote spot $uuid from Firestore")
        } catch (e: Exception) {
            platformLogError(TAG, "Error deleting remote spot $uuid from Firestore", e)
        }
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHJKMNPQRSTVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    override suspend fun sharePack(
        title: String,
        description: String,
        authorName: String,
        spots: List<SpotDetails>
    ): String {
        val code = generateShareCode()
        val currentFirestore = firestore
        if (currentFirestore != null) {
            val pack = net.maiatoday.tagspotter.core.model.SharedPack(
                packId = code,
                title = title,
                authorName = authorName,
                description = description,
                spots = spots
            )
            currentFirestore.collection("shared_packs").document(code).set(net.maiatoday.tagspotter.core.model.SharedPack.serializer(), pack)
        } else {
            println("Firestore not available, simulated sharing pack code: $code")
        }
        return code
    }

    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack {
        val currentFirestore = firestore
        if (currentFirestore != null) {
            val doc = currentFirestore.collection("shared_packs").document(code).get()
            return doc.data(net.maiatoday.tagspotter.core.model.SharedPack.serializer())
        } else {
            return net.maiatoday.tagspotter.core.model.SharedPack(
                packId = code,
                title = "Milano Tour (Mock)",
                authorName = "Alice",
                description = "Beautiful spots around Duomo",
                spots = emptyList()
            )
        }
    }

    override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {
        val now = net.maiatoday.tagspotter.core.database.epochMillis()
        val loadedPack = net.maiatoday.tagspotter.core.model.LoadedPack(
            packId = sharedPack.packId,
            title = sharedPack.title,
            authorName = sharedPack.authorName,
            description = sharedPack.description,
            importedAt = now,
            lastRefreshedAt = now
        )
        repository.saveLoadedPack(loadedPack)

        sharedPack.spots.forEach { detail ->
            val updatedSpot = detail.spot.copy(
                id = 0L,
                parentPackId = sharedPack.packId,
                isImported = true
            )
            val updatedDetail = detail.copy(
                spot = updatedSpot,
                images = detail.images.map { it.copy(id = 0L) },
                notes = detail.notes.map { it.copy(id = 0L) }
            )
            repository.saveSpotDetails(updatedDetail)
        }
    }

    private suspend fun resolveRemoteThumbnails(userId: String, cloudDetail: SpotDetails): SpotDetails {
        val currentStorage = storage ?: return cloudDetail
        val updatedImages = cloudDetail.images.map { image ->
            if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                try {
                    val storageRef = currentStorage.reference("users/$userId/thumbnails/${image.uuid}.jpg")
                    val downloadUrl = storageRef.getDownloadUrl()
                    platformLog(TAG, "Resolved remote thumbnail URL for ${image.uuid}: $downloadUrl")
                    image.copy(thumbnailPath = downloadUrl)
                } catch (e: Exception) {
                    platformLogError(TAG, "Failed to resolve remote thumbnail for ${image.uuid}", e)
                    image
                }
            } else {
                image
            }
        }
        return cloudDetail.copy(images = updatedImages)
    }
}
