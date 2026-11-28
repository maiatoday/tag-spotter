package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotOrigin
import kotlin.time.Duration.Companion.milliseconds

class NonWebSyncManager(
    private val repository: SpotRepository
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()
    private val syncMutex = Mutex()
    private val publicSyncMutex = Mutex()

    override fun clearSyncError() {
        _syncError.value = null
    }

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
            platformLog(TAG, "syncNow: activeUserId is null, syncing public spots only")
            syncPublicSpots()
            return
        }
        if (!syncMutex.tryLock()) {
            platformLog(TAG, "syncNow early return: sync or backup already in progress")
            return
        }
        _isSyncing.value = true
        var runError: String? = null

        try {
            val currentFirestore = firestore
            val currentStorage = storage
            if (currentFirestore == null || currentStorage == null) {
                platformLog(TAG, "Skipping real sync on Desktop JVM (Simulating successful mock sync)")
                delay(1000.milliseconds)
                return
            }

            val spotsCollection = currentFirestore.collection("spots")

            // 1. Push stage: find all unsynced OWN local spots (IMPORTED & PUBLIC_CACHE excluded by DAO)
            val unsyncedSpots = repository.getUnsyncedSpots()
            platformLog(TAG, "Push stage: Found ${unsyncedSpots.size} unsynced local spots")

            unsyncedSpots.forEach { localDetail ->
                if (localDetail.spot.effectiveOrigin != SpotOrigin.OWN) return@forEach
                val uuid = localDetail.spot.uuid
                platformLog(TAG, "Pushing spot: $uuid")

                var allThumbnailsReady = true
                val remoteImages = localDetail.images.map { image ->
                    val resolvedUrl = uploadOrResolveThumbnail(currentStorage, userId, uuid, image)
                    if (resolvedUrl == null) {
                        allThumbnailsReady = false
                        image
                    } else {
                        if (image.id != 0L && resolvedUrl != image.remoteThumbnailUrl) {
                            repository.updateImageRemoteUrl(image.id, resolvedUrl)
                        }
                        image.copy(
                            thumbnailPath = resolvedUrl,
                            imagePath = resolvedUrl,
                            remoteThumbnailUrl = resolvedUrl
                        )
                    }
                }

                if (!allThumbnailsReady) {
                    platformLog(TAG, "Deferring sync for $uuid until thumbnail upload succeeds")
                    runError = "Photo thumbnail upload failed; some spots are waiting to sync."
                    return@forEach
                }

                val detailToPush = localDetail.copy(
                    spot = localDetail.spot.copy(ownerUid = userId, origin = SpotOrigin.OWN, isImported = false),
                    images = remoteImages
                )
                val firestoreDoc = FirestoreSpotDocument.fromSpotDetails(userId, detailToPush)

                try {
                    spotsCollection.document(uuid).set(FirestoreSpotDocument.serializer(), firestoreDoc)
                    repository.markSpotAsSynced(uuid)
                    platformLog(TAG, "Pushed and marked spot synced: $uuid (isPublic=${firestoreDoc.isPublic})")
                } catch (e: Exception) {
                    runError = "Cloud sync failed: ${e.message ?: "unable to save spot"}"
                    platformLogError(TAG, "Failed to push spot $uuid to /spots", e)
                }
            }

            // 2. Pull stage: fetch user's own remote spots from /spots where ownerUid == userId
            platformLog(TAG, "Pull stage: Fetching own remote spots from /spots")
            val querySnapshot = spotsCollection.where { "ownerUid" equalTo userId }.get()
            val localSpots = repository.getAllSpots().first()
            platformLog(TAG, "Pull stage: Found ${querySnapshot.documents.size} remote spots and ${localSpots.size} local spots")

            querySnapshot.documents.forEach { doc ->
                try {
                    val cloudDoc = doc.data(FirestoreSpotDocument.serializer())
                    val cloudDetail = cloudDoc.toSpotDetails()
                    val resolvedDetail = resolveRemoteThumbnails(userId, cloudDetail)
                    val localMatch = localSpots.find { it.spot.uuid == resolvedDetail.spot.uuid }

                    if (localMatch == null) {
                        repository.saveSyncedSpot(resolvedDetail)
                    } else if (shouldApplyRemoteUpdate(localMatch, resolvedDetail)) {
                        repository.saveSyncedSpot(resolvedDetail)
                    }
                } catch (e: Exception) {
                    platformLogError(TAG, "Error parsing pulled spot document ${doc.id}", e)
                }
            }

            // 3. Pull public spots from other users
            val publicError = syncPublicSpotsInternal()
            _syncError.value = runError ?: publicError
        } catch (e: Exception) {
            _syncError.value = "Cloud sync failed: ${e.message ?: "unknown error"}"
            platformLogError(TAG, "Error during syncNow", e)
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
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
                currentFirestore.collection("spots")
                    .where { "ownerUid" equalTo userId }
                    .snapshots
                    .collect { querySnapshot ->
                        platformLog(TAG, "Received realtime snapshot update with ${querySnapshot.documents.size} documents")
                        val localSpots = repository.getAllSpots().first()
                        val remoteUuids = mutableSetOf<String>()
                        var hadDecodeFailure = false

                        querySnapshot.documents.forEach { doc ->
                            try {
                                val cloudDoc = doc.data(FirestoreSpotDocument.serializer())
                                val resolvedDetail = resolveRemoteThumbnails(userId, cloudDoc.toSpotDetails())
                                remoteUuids.add(resolvedDetail.spot.uuid)
                                val localMatch = localSpots.find { it.spot.uuid == resolvedDetail.spot.uuid }

                                if (localMatch == null) {
                                    repository.saveSyncedSpot(resolvedDetail)
                                } else if (shouldApplyRemoteUpdate(localMatch, resolvedDetail)) {
                                    repository.saveSyncedSpot(resolvedDetail)
                                }
                            } catch (e: Exception) {
                                hadDecodeFailure = true
                                platformLogError(TAG, "Error parsing real-time spot document ${doc.id}", e)
                            }
                        }

                        if (!hadDecodeFailure && !querySnapshot.metadata.isFromCache) {
                            localSpots.filter {
                                it.spot.effectiveOrigin == SpotOrigin.OWN &&
                                    it.spot.ownerUid == userId &&
                                    it.spot.isSynced
                            }.forEach { localSpot ->
                                if (localSpot.spot.uuid !in remoteUuids) {
                                    platformLog(TAG, "Spot ${localSpot.spot.uuid} deleted remotely; removing locally.")
                                    repository.deleteSpot(localSpot)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                platformLogError(TAG, "Error in realtime sync stream", e)
            }
        }

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
        activeUserId ?: return
        try {
            firestore?.collection("spots")?.document(uuid)?.delete()
            platformLog(TAG, "Deleted remote spot $uuid from /spots")
        } catch (e: Exception) {
            platformLogError(TAG, "Error deleting remote spot $uuid from /spots", e)
        }
    }

    override suspend fun forceBackupAllImages(): BackupResult {
        val userId = activeUserId ?: return BackupResult(attempted = false)
        val currentFirestore = firestore ?: return BackupResult(attempted = false)
        val currentStorage = storage ?: return BackupResult(attempted = false)
        if (!syncMutex.tryLock()) return BackupResult(attempted = false)
        _isSyncing.value = true
        var total = BackupResult()

        try {
            val allSpots = repository.getAllSpots().first()
                .filter { it.spot.effectiveOrigin == SpotOrigin.OWN && it.spot.isEditableBy(userId) }
            val spotsCollection = currentFirestore.collection("spots")

            allSpots.forEach { localDetail ->
                total += backupSpotImagesLocked(currentStorage, spotsCollection, userId, localDetail)
            }
        } catch (e: Exception) {
            total += BackupResult(failed = 1)
            platformLogError(TAG, "Error during forceBackupAllImages", e)
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
        return total
    }

    override suspend fun forceBackupSpot(uuid: String): BackupResult {
        val userId = activeUserId ?: return BackupResult(attempted = false)
        val currentFirestore = firestore ?: return BackupResult(attempted = false)
        val currentStorage = storage ?: return BackupResult(attempted = false)
        if (!syncMutex.tryLock()) return BackupResult(attempted = false)
        _isSyncing.value = true
        var result = BackupResult()

        try {
            val localDetail = repository.getAllSpots().first()
                .find { it.spot.uuid == uuid && it.spot.effectiveOrigin == SpotOrigin.OWN && it.spot.isEditableBy(userId) }
            if (localDetail != null) {
                val spotsCollection = currentFirestore.collection("spots")
                result = backupSpotImagesLocked(currentStorage, spotsCollection, userId, localDetail)
            }
        } catch (e: Exception) {
            result += BackupResult(failed = 1)
            platformLogError(TAG, "Error during forceBackupSpot", e)
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
        return result
    }

    private suspend fun backupSpotImagesLocked(
        currentStorage: dev.gitlive.firebase.storage.FirebaseStorage,
        spotsCollection: dev.gitlive.firebase.firestore.CollectionReference,
        userId: String,
        localDetail: SpotDetails
    ): BackupResult {
        val uuid = localDetail.spot.uuid
        var uploaded = 0
        var refreshedUrls = 0
        var failed = 0
        var skippedNoLocalFile = 0
        var spotModified = false
        var allThumbnailsReady = true
        val pendingRemoteUrlUpdates = mutableListOf<Pair<Long, String>>()
        val remoteImages = localDetail.images.map { image ->
            val bytes = readBytesFromFile(image.thumbnailPath) ?: readBytesFromFile(image.imagePath)
            val storageRef = currentStorage.reference(spotThumbnailStoragePath(userId, uuid, image.uuid))
            if (bytes != null) {
                try {
                    storageRef.putData(bytes.toFirebaseStorageData())
                    val downloadUrl = storageRef.getDownloadUrl()
                    if (image.id != 0L) {
                        pendingRemoteUrlUpdates.add(image.id to downloadUrl)
                    }
                    uploaded++
                    spotModified = true
                    image.copy(thumbnailPath = downloadUrl, imagePath = downloadUrl, remoteThumbnailUrl = downloadUrl)
                } catch (e: Exception) {
                    allThumbnailsReady = false
                    failed++
                    platformLogError(TAG, "Force backup failed for ${image.uuid}", e)
                    image
                }
            } else {
                val refreshedUrl = try {
                    storageRef.getDownloadUrl()
                } catch (_: Exception) {
                    null
                }
                if (!refreshedUrl.isNullOrBlank()) {
                    if (refreshedUrl != image.remoteThumbnailUrl) {
                        if (image.id != 0L) {
                            pendingRemoteUrlUpdates.add(image.id to refreshedUrl)
                        }
                        refreshedUrls++
                        spotModified = true
                    } else {
                        skippedNoLocalFile++
                    }
                    image.copy(thumbnailPath = refreshedUrl, imagePath = refreshedUrl, remoteThumbnailUrl = refreshedUrl)
                } else {
                    val fallbackUrl = image.remoteThumbnailUrl.ifBlank {
                        if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
                    }
                    if (fallbackUrl.isNotEmpty()) {
                        skippedNoLocalFile++
                        image.copy(thumbnailPath = fallbackUrl, imagePath = fallbackUrl, remoteThumbnailUrl = fallbackUrl)
                    } else {
                        allThumbnailsReady = false
                        failed++
                        image
                    }
                }
            }
        }

        if (spotModified && allThumbnailsReady) {
            try {
                val detailToPush = localDetail.copy(
                    spot = localDetail.spot.copy(ownerUid = userId, origin = SpotOrigin.OWN, isImported = false),
                    images = remoteImages
                )
                spotsCollection.document(uuid).set(
                    FirestoreSpotDocument.serializer(),
                    FirestoreSpotDocument.fromSpotDetails(userId, detailToPush)
                )
                pendingRemoteUrlUpdates.forEach { (imageId, url) ->
                    repository.updateImageRemoteUrl(imageId, url)
                }
                val latestLocal = repository.getAllSpots().first().find { it.spot.uuid == uuid }
                if (latestLocal == null || latestLocal.spot.lastEditedAt == localDetail.spot.lastEditedAt) {
                    repository.markSpotAsSynced(uuid)
                }
            } catch (e: Exception) {
                failed++
                platformLogError(TAG, "Failed to update Firestore metadata after force backup for $uuid", e)
            }
        }
        return BackupResult(
            uploaded = uploaded,
            refreshedUrls = refreshedUrls,
            failed = failed,
            skippedNoLocalFile = skippedNoLocalFile
        )
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
        val currentFirestore = firestore ?: return emptyList()
        return try {
            val snapshot = currentFirestore.collection("spots")
                .where { "isPublic" equalTo true }
                .get()
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.data(FirestoreSpotDocument.serializer()).toSpotDetails()
                } catch (e: Exception) {
                    platformLogError(TAG, "Error parsing public spot ${doc.id}", e)
                    null
                }
            }.filter { detail ->
                detail.spot.isPublic &&
                    detail.spot.latitude in minLat..maxLat &&
                    detail.spot.longitude in minLon..maxLon
            }
        } catch (e: Exception) {
            platformLogError(TAG, "Error fetching public spots", e)
            emptyList()
        }
    }

    override suspend fun syncPublicSpots() {
        val error = syncPublicSpotsInternal()
        if (error != null) {
            _syncError.value = error
        }
    }

    private suspend fun syncPublicSpotsInternal(): String? {
        val currentFirestore = firestore ?: return null
        return publicSyncMutex.withLock {
            try {
                val snapshot = currentFirestore.collection("spots")
                    .where { "isPublic" equalTo true }
                    .get()
                var hadDecodeFailure = false
                val remoteSpots = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.data(FirestoreSpotDocument.serializer()).toSpotDetails()
                    } catch (e: Exception) {
                        hadDecodeFailure = true
                        platformLogError(TAG, "Error parsing public spot ${doc.id}", e)
                        null
                    }
                }.filter { it.spot.isPublic }

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
                if (!hadDecodeFailure && !snapshot.metadata.isFromCache) {
                    repository.pruneRemovedPublicSpots(activePublicUuids)
                }
                platformLog(TAG, "syncPublicSpots completed: ${remoteSpots.size} remote public spots")
                null
            } catch (e: Exception) {
                platformLogError(TAG, "Error during syncPublicSpots", e)
                "Failed to sync public spots: ${e.message ?: "unknown error"}"
            }
        }
    }

    private suspend fun uploadOrResolveThumbnail(
        currentStorage: dev.gitlive.firebase.storage.FirebaseStorage,
        userId: String,
        spotUuid: String,
        image: SpotImage
    ): String? {
        val bytes = readBytesFromFile(image.thumbnailPath) ?: readBytesFromFile(image.imagePath)
        val canonicalRef = currentStorage.reference(spotThumbnailStoragePath(userId, spotUuid, image.uuid))
        if (bytes != null) {
            return try {
                canonicalRef.putData(bytes.toFirebaseStorageData())
                canonicalRef.getDownloadUrl()
            } catch (e: Exception) {
                platformLogError(TAG, "Failed to upload thumbnail ${image.uuid}", e)
                null
            }
        }
        if (image.remoteThumbnailUrl.startsWith("http")) {
            return image.remoteThumbnailUrl
        }
        if (image.thumbnailPath.startsWith("http")) {
            return image.thumbnailPath
        }
        return try {
            canonicalRef.getDownloadUrl()
        } catch (e: Exception) {
            platformLogError(TAG, "Failed to resolve canonical thumbnail URL for ${image.uuid}", e)
            null
        }
    }

    private suspend fun resolveRemoteThumbnails(userId: String, cloudDetail: SpotDetails): SpotDetails {
        val currentStorage = storage ?: return cloudDetail
        val spotUuid = cloudDetail.spot.uuid
        val updatedImages = coroutineScope {
            cloudDetail.images.map { image ->
                async {
                    val existingRemote = image.remoteThumbnailUrl.ifBlank {
                        if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
                    }
                    if (existingRemote.isNotEmpty()) {
                        image.copy(remoteThumbnailUrl = existingRemote)
                    } else {
                        try {
                            val canonicalRef = currentStorage.reference(spotThumbnailStoragePath(userId, spotUuid, image.uuid))
                            val downloadUrl = canonicalRef.getDownloadUrl()
                            image.copy(thumbnailPath = downloadUrl, imagePath = downloadUrl, remoteThumbnailUrl = downloadUrl)
                        } catch (e: Exception) {
                            try {
                                val legacyRef = currentStorage.reference("users/$userId/thumbnails/${image.uuid}.jpg")
                                val legacyUrl = legacyRef.getDownloadUrl()
                                image.copy(thumbnailPath = legacyUrl, imagePath = legacyUrl, remoteThumbnailUrl = legacyUrl)
                            } catch (legacyEx: Exception) {
                                platformLogError(TAG, "Failed to resolve remote thumbnail for ${image.uuid}", legacyEx)
                                image
                            }
                        }
                    }
                }
            }.awaitAll()
        }
        return cloudDetail.copy(images = updatedImages)
    }
}
