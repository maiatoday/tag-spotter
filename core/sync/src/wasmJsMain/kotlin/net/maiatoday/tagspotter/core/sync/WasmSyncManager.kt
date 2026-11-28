package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotOrigin
import kotlin.coroutines.resume

// External JS helper declarations for canonical /spots/{uuid} Firestore & Storage
external fun webFirestoreSaveSpot(
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

external fun webFirestoreFetchPublicSpots(
    onSuccess: (spotsJson: String) -> Unit,
    onFailure: (String) -> Unit
)

external fun webStorageUploadThumbnail(
    userId: String,
    spotUuid: String,
    imageUuid: String,
    base64Data: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
)

external fun webStorageGetDownloadUrl(
    userId: String,
    spotUuid: String,
    imageUuid: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
)

external fun webFirestoreDeleteSpot(
    spotUuid: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

class WasmSyncManager(
    private val repository: SpotRepository
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()

    override fun clearSyncError() {
        _syncError.value = null
    }

    private var activeUserId: String? = null
    private var isListening = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun syncNow() {
        val userId = activeUserId ?: run {
            syncPublicSpots()
            return
        }
        if (_isSyncing.value) return
        _isSyncing.value = true
        var runError: String? = null

        try {
            // 1. Push stage: find all unsynced OWN local spots
            val unsyncedSpots = repository.getUnsyncedSpots()
            unsyncedSpots.forEach { localDetail ->
                if (localDetail.spot.effectiveOrigin != SpotOrigin.OWN) return@forEach
                val uuid = localDetail.spot.uuid

                var allThumbnailsReady = true
                val remoteImages = localDetail.images.map { image ->
                    val sourcePath = image.thumbnailPath.ifBlank { image.imagePath }
                    if (sourcePath.isNotEmpty() && !sourcePath.startsWith("http")) {
                        val base64Data = if (sourcePath.startsWith("data:")) {
                            sourcePath.substringAfter(",")
                        } else {
                            sourcePath
                        }
                        val uploadedUrl = suspendCancellableCoroutine { continuation ->
                            webStorageUploadThumbnail(
                                userId = userId,
                                spotUuid = uuid,
                                imageUuid = image.uuid,
                                base64Data = base64Data,
                                onSuccess = { url -> if (continuation.isActive) continuation.resume(url) },
                                onFailure = { err ->
                                    println("Web Storage upload failed for ${image.uuid}: $err")
                                    if (continuation.isActive) continuation.resume("")
                                }
                            )
                        }
                        if (uploadedUrl.isEmpty()) {
                            allThumbnailsReady = false
                            image
                        } else {
                            if (image.id != 0L) {
                                repository.updateImageRemoteUrl(image.id, uploadedUrl)
                            }
                            image.copy(thumbnailPath = uploadedUrl, imagePath = uploadedUrl, remoteThumbnailUrl = uploadedUrl)
                        }
                    } else {
                        val existingUrl = image.remoteThumbnailUrl.ifBlank { sourcePath }
                        if (existingUrl.isBlank()) {
                            allThumbnailsReady = false
                            image
                        } else {
                            image.copy(thumbnailPath = existingUrl, imagePath = existingUrl, remoteThumbnailUrl = existingUrl)
                        }
                    }
                }

                if (!allThumbnailsReady) {
                    runError = "Photo thumbnail upload failed; some spots are waiting to sync."
                    println("Deferring Wasm sync for $uuid until thumbnail upload succeeds")
                    return@forEach
                }

                val detailToPush = localDetail.copy(
                    spot = localDetail.spot.copy(ownerUid = userId, origin = SpotOrigin.OWN, isImported = false),
                    images = remoteImages
                )
                val firestoreDoc = FirestoreSpotDocument.fromSpotDetails(userId, detailToPush)
                val jsonString = json.encodeToString(FirestoreSpotDocument.serializer(), firestoreDoc)

                val saveSuccess = suspendCancellableCoroutine { continuation ->
                    webFirestoreSaveSpot(
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
                    repository.markSpotAsSynced(uuid)
                } else {
                    runError = "Cloud sync failed while saving spot metadata."
                }
            }

            syncPublicSpots()
            _syncError.value = runError
        } catch (e: Exception) {
            _syncError.value = "Cloud sync failed: ${e.message ?: "unknown error"}"
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
                            val cloudDoc = json.decodeFromString(FirestoreSpotDocument.serializer(), spotJson)
                            val cloudDetail = cloudDoc.toSpotDetails()
                            val localSpots = repository.getAllSpots().first()
                            val localMatch = localSpots.find { it.spot.uuid == cloudDetail.spot.uuid }
                            val resolvedDetail = resolveRemoteThumbnails(userId, cloudDetail, localMatch)

                            if (localMatch == null) {
                                repository.saveSyncedSpot(resolvedDetail)
                            } else if (shouldApplyRemoteUpdate(localMatch, resolvedDetail)) {
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

        coroutineScope.launch {
            syncNow()
        }
    }

    override fun stopRealtimeSync() {
        activeUserId = null
        isListening = false
    }

    override suspend fun deleteSpot(uuid: String) {
        activeUserId ?: return
        suspendCancellableCoroutine { continuation ->
            webFirestoreDeleteSpot(
                spotUuid = uuid,
                onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                onFailure = { err ->
                    println("Web Firestore delete failed for $uuid: $err")
                    if (continuation.isActive) continuation.resume(Unit)
                }
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
                isImported = true,
                origin = SpotOrigin.IMPORTED,
                isSynced = true
            )
            val updatedDetail = detail.copy(
                spot = updatedSpot,
                images = detail.images.map { it.copy(id = 0L) },
                notes = detail.notes.map { it.copy(id = 0L) }
            )
            repository.saveImportedSpot(updatedDetail)
        }
    }

    override suspend fun fetchPublicSpots(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<SpotDetails> {
        return fetchPublicSpotsOrNull()?.filter { detail ->
            detail.spot.isPublic &&
                detail.spot.latitude in minLat..maxLat &&
                detail.spot.longitude in minLon..maxLon
        } ?: emptyList()
    }

    override suspend fun syncPublicSpots() {
        try {
            val remoteSpots = fetchPublicSpotsOrNull() ?: return
            val currentUid = activeUserId ?: repository.activeUid
            val activePublicUuids = remoteSpots.map { it.spot.uuid }.toSet()
            val otherUsersSpots = remoteSpots.filter { detail ->
                currentUid == null || detail.spot.ownerUid != currentUid
            }.map { detail ->
                val ownerUid = detail.spot.ownerUid
                if (!ownerUid.isNullOrBlank() && detail.hasUnresolvedRemoteThumbnails()) {
                    resolveRemoteThumbnails(ownerUid, detail)
                } else {
                    detail
                }
            }
            repository.savePublicSpots(otherUsersSpots)
            repository.pruneRemovedPublicSpots(activePublicUuids)
        } catch (e: Exception) {
            println("Error during Wasm syncPublicSpots: ${e.message}")
        }
    }

    private suspend fun fetchPublicSpotsOrNull(): List<SpotDetails>? {
        val spotsJson = suspendCancellableCoroutine<String?> { continuation ->
            webFirestoreFetchPublicSpots(
                onSuccess = { payload -> if (continuation.isActive) continuation.resume(payload) },
                onFailure = { err ->
                    println("Failed to fetch web public spots: $err")
                    if (continuation.isActive) continuation.resume(null)
                }
            )
        } ?: return null

        return try {
            json.decodeFromString(ListSerializer(FirestoreSpotDocument.serializer()), spotsJson)
                .map { it.toSpotDetails() }
                .filter { it.spot.isPublic }
        } catch (e: Exception) {
            println("Error decoding web public spots JSON: ${e.message}")
            null
        }
    }

    private suspend fun resolveRemoteThumbnails(
        userId: String,
        cloudDetail: SpotDetails,
        localMatch: SpotDetails? = null
    ): SpotDetails {
        val spotUuid = cloudDetail.spot.uuid
        val updatedImages = cloudDetail.images.map { image ->
            val existingLocalImage = localMatch?.images?.find { it.uuid == image.uuid }
            val existingHttpUrl = image.remoteThumbnailUrl.ifBlank {
                if (image.thumbnailPath.startsWith("http")) image.thumbnailPath
                else if (existingLocalImage?.remoteThumbnailUrl?.startsWith("http") == true) existingLocalImage.remoteThumbnailUrl
                else ""
            }

            if (existingHttpUrl.isNotEmpty()) {
                image.copy(
                    thumbnailPath = existingHttpUrl,
                    imagePath = existingHttpUrl,
                    remoteThumbnailUrl = existingHttpUrl
                )
            } else if (image.uuid.isNotEmpty()) {
                val downloadUrl = suspendCancellableCoroutine { continuation ->
                    webStorageGetDownloadUrl(
                        userId = userId,
                        spotUuid = spotUuid,
                        imageUuid = image.uuid,
                        onSuccess = { url -> if (continuation.isActive) continuation.resume(url) },
                        onFailure = { _ -> if (continuation.isActive) continuation.resume("") }
                    )
                }
                if (downloadUrl.isNotEmpty()) {
                    image.copy(thumbnailPath = downloadUrl, imagePath = downloadUrl, remoteThumbnailUrl = downloadUrl)
                } else {
                    image
                }
            } else {
                image
            }
        }
        return cloudDetail.copy(images = updatedImages)
    }
}
