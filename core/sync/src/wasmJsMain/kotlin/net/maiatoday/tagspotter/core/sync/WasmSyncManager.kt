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

external fun webStorageGetDownloadUrl(
    userId: String,
    imageUuid: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
)

external fun webFirestoreDeleteSpot(
    userId: String,
    spotUuid: String,
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
                            val resolvedDetail = resolveRemoteThumbnails(userId, cloudDetail, localMatch)

                            if (localMatch == null) {
                                repository.saveSyncedSpot(resolvedDetail)
                            } else if (resolvedDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt ||
                                       resolvedDetail.images.size != localMatch.images.size ||
                                       resolvedDetail.notes.size != localMatch.notes.size ||
                                       localMatch.images.any { it.thumbnailPath != resolvedDetail.images.find { r -> r.uuid == it.uuid }?.thumbnailPath }) {
                                repository.saveSyncedSpot(resolvedDetail)
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

    override suspend fun deleteSpot(uuid: String) {
        val userId = activeUserId ?: return
        suspendCancellableCoroutine { continuation ->
            webFirestoreDeleteSpot(
                userId = userId,
                spotUuid = uuid,
                onSuccess = { continuation.resume(Unit) },
                onFailure = { continuation.resume(Unit) }
            )
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
        println("Web shared pack code generated: $code")
        return code
    }

    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack {
        return net.maiatoday.tagspotter.core.model.SharedPack(
            packId = code,
            title = "Milano Tour (Mock)",
            authorName = "Alice",
            description = "Beautiful spots around Duomo",
            spots = emptyList()
        )
    }

    override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {
        val now = 0L
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

    private suspend fun resolveRemoteThumbnails(userId: String, cloudDetail: SpotDetails, localMatch: SpotDetails? = null): SpotDetails {
        val updatedImages = cloudDetail.images.map { image ->
            val existingLocalImage = localMatch?.images?.find { it.uuid == image.uuid }
            val existingHttpUrl = if (existingLocalImage?.thumbnailPath?.startsWith("http") == true) existingLocalImage.thumbnailPath else ""

            if (image.uuid.isNotEmpty() && !image.thumbnailPath.startsWith("http") && !image.thumbnailPath.startsWith("data:")) {
                try {
                    val downloadUrl = suspendCancellableCoroutine<String> { continuation ->
                        webStorageGetDownloadUrl(
                            userId = userId,
                            imageUuid = image.uuid,
                            onSuccess = { url -> if (continuation.isActive) continuation.resume(url) },
                            onFailure = { err ->
                                println("Failed to resolve WASM remote thumbnail for ${image.uuid}: $err")
                                if (continuation.isActive) continuation.resume("")
                            }
                        )
                    }
                    if (downloadUrl.isNotEmpty()) {
                        image.copy(
                            thumbnailPath = downloadUrl,
                            imagePath = if (image.imagePath.isEmpty() || (!image.imagePath.startsWith("http") && !image.imagePath.startsWith("data:"))) downloadUrl else image.imagePath
                        )
                    } else if (existingHttpUrl.isNotEmpty()) {
                        image.copy(
                            thumbnailPath = existingHttpUrl,
                            imagePath = if (image.imagePath.isEmpty() || (!image.imagePath.startsWith("http") && !image.imagePath.startsWith("data:"))) existingHttpUrl else image.imagePath
                        )
                    } else {
                        val fallbackUrl = "https://firebasestorage.googleapis.com/v0/b/tagspotter-d58b1.firebasestorage.app/o/users%2F$userId%2Fthumbnails%2F${image.uuid}.jpg?alt=media"
                        image.copy(
                            thumbnailPath = fallbackUrl,
                            imagePath = if (image.imagePath.isEmpty() || (!image.imagePath.startsWith("http") && !image.imagePath.startsWith("data:"))) fallbackUrl else image.imagePath
                        )
                    }
                } catch (e: Exception) {
                    println("Error resolving remote thumbnail for ${image.uuid}: ${e.message}")
                    if (existingHttpUrl.isNotEmpty()) {
                        image.copy(thumbnailPath = existingHttpUrl)
                    } else {
                        image
                    }
                }
            } else {
                image
            }
        }
        return cloudDetail.copy(images = updatedImages)
    }
}
