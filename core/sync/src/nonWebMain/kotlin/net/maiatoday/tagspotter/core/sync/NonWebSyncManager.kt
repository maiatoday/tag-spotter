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

    override suspend fun syncNow() {
        val userId = activeUserId ?: return
        if (_isSyncing.value) return
        val currentFirestore = firestore
        val currentStorage = storage
        if (currentFirestore == null || currentStorage == null) {
            println("Skipping real sync on Desktop JVM (Simulating successful mock sync)")
            _isSyncing.value = true
            delay(1000)
            _isSyncing.value = false
            return
        }
        _isSyncing.value = true

        try {
            // 1. Push stage: find all unsynced local spots
            val unsyncedSpots = repository.getUnsyncedSpots()
            val spotsCollection = currentFirestore.collection("users").document(userId).collection("spots")

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
                                val storageRef = currentStorage.reference("users/$userId/thumbnails/${image.uuid}.jpg")
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
        val currentFirestore = firestore
        if (currentFirestore == null) {
            println("Skipping realtime sync on Desktop JVM (Firestore unavailable)")
            return
        }

        realtimeJob = coroutineScope.launch {
            try {
                currentFirestore.collection("users").document(userId).collection("spots")
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
}
