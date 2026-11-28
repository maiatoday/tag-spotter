package net.maiatoday.tagspotter.core.sync

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotOrigin
import kotlin.time.Duration.Companion.milliseconds

fun JsonElement.toFirestoreValue(): JsonObject {
    return when (val element = this) {
        is JsonNull -> buildJsonObject { put("nullValue", JsonNull) }
        is JsonPrimitive -> {
            if (element.isString) {
                buildJsonObject { put("stringValue", element.content) }
            } else {
                val booleanValue = element.booleanOrNull
                if (booleanValue != null) {
                    buildJsonObject { put("booleanValue", booleanValue) }
                } else {
                    val doubleValue = element.doubleOrNull
                    if (element.content.contains(".") || element.content.contains("e", ignoreCase = true)) {
                        buildJsonObject { put("doubleValue", doubleValue ?: 0.0) }
                    } else {
                        buildJsonObject { put("integerValue", element.content) }
                    }
                }
            }
        }
        is JsonArray -> {
            buildJsonObject {
                put("arrayValue", buildJsonObject {
                    putJsonArray("values") {
                        element.forEach { item ->
                            add(item.toFirestoreValue())
                        }
                    }
                })
            }
        }
        is JsonObject -> {
            buildJsonObject {
                put("mapValue", buildJsonObject {
                    putJsonObject("fields") {
                        element.forEach { (key, value) ->
                            put(key, value.toFirestoreValue())
                        }
                    }
                })
            }
        }
    }
}

fun JsonObject.fromFirestoreValue(): JsonElement {
    if (containsKey("nullValue")) return JsonNull
    val stringVal = this["stringValue"]?.jsonPrimitive
    if (stringVal != null) return JsonPrimitive(stringVal.content)
    val booleanVal = this["booleanValue"]?.jsonPrimitive
    if (booleanVal != null) return JsonPrimitive(booleanVal.boolean)

    val integerVal = this["integerValue"]?.jsonPrimitive
    if (integerVal != null) {
        val longVal = integerVal.content.toLongOrNull()
        if (longVal != null) return JsonPrimitive(longVal)
        return JsonPrimitive(integerVal.content)
    }

    val doubleVal = this["doubleValue"]?.jsonPrimitive
    if (doubleVal != null) {
        val dVal = doubleVal.content.toDoubleOrNull()
        if (dVal != null) return JsonPrimitive(dVal)
        return JsonPrimitive(doubleVal.content)
    }

    val arrayVal = this["arrayValue"]?.jsonObject
    if (arrayVal != null) {
        val values = arrayVal["values"]?.jsonArray ?: return JsonArray(emptyList())
        return JsonArray(values.map { it.jsonObject.fromFirestoreValue() })
    }
    val mapVal = this["mapValue"]?.jsonObject
    if (mapVal != null) {
        val fields = mapVal["fields"]?.jsonObject ?: return JsonObject(emptyMap())
        return JsonObject(fields.mapValues { it.value.jsonObject.fromFirestoreValue() })
    }
    return JsonNull
}

class JvmSyncManager(
    private val repository: SpotRepository,
    private val client: JvmFirebaseClient,
    private val firestoreOrigin: String = "https://firestore.googleapis.com",
    private val storageOrigin: String = "https://firebasestorage.googleapis.com",
    private val thumbnailCacheRoot: java.io.File = java.io.File(System.getProperty("user.home"), "Pictures/TagSpotter/thumbnails")
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

    private var realtimeJob: Job? = null
    private var activeUserId: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun syncNow() {
        val userId = activeUserId ?: run {
            syncPublicSpots()
            return
        }
        if (!syncMutex.tryLock()) return
        _isSyncing.value = true
        var runError: String? = null

        try {
            // 1. Push stage: find all unsynced OWN local spots
            val unsyncedSpots = repository.getUnsyncedSpots()

            unsyncedSpots.forEach { localDetail ->
                if (localDetail.spot.effectiveOrigin != SpotOrigin.OWN) return@forEach
                val uuid = localDetail.spot.uuid

                var allThumbnailsReady = true
                val sanitizedImages = localDetail.images.map { image ->
                    val resolvedUrl = uploadOrResolveThumbnail(userId, uuid, image)
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
                    runError = "Photo thumbnail upload failed; some spots are waiting to sync."
                    println("Deferring JVM sync for $uuid until thumbnail upload succeeds")
                    return@forEach
                }

                val detailToPush = localDetail.copy(
                    spot = localDetail.spot.copy(ownerUid = userId, origin = SpotOrigin.OWN, isImported = false),
                    images = sanitizedImages
                )
                val firestoreDoc = FirestoreSpotDocument.fromSpotDetails(userId, detailToPush)
                val jsonElement = client.jsonConfig.encodeToJsonElement(FirestoreSpotDocument.serializer(), firestoreDoc)
                val firestoreFields = jsonElement.jsonObject.toFirestoreValue()["mapValue"]?.jsonObject?.get("fields")?.jsonObject
                    ?: throw Exception("Invalid firestore serialization map fields")

                val docUrl = "$firestoreOrigin/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/spots/$uuid"
                val patchResponse = client.authenticatedClient.patch(docUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("fields", firestoreFields)
                    })
                }

                if (patchResponse.status.value in 200..299) {
                    repository.markSpotAsSynced(uuid)
                } else {
                    runError = "Cloud sync failed (HTTP ${patchResponse.status.value})"
                    println("Failed to push spot metadata for $uuid: ${patchResponse.bodyAsText()}")
                }
            }

            // 2. Pull stage: fetch user's own spots via runQuery on /spots where ownerUid == userId
            val ownQueryResult = runSpotsQuery(
                httpClient = client.authenticatedClient,
                fieldPath = "ownerUid",
                value = buildJsonObject { put("stringValue", userId) }
            )
            if (ownQueryResult == null) {
                runError = runError ?: "Failed to fetch spots from cloud."
            } else {
                val localSpots = repository.getAllSpots().first()
                val remoteUuids = mutableSetOf<String>()

                ownQueryResult.spots.forEach { cloudDetail ->
                    val updatedCloudDetail = resolveRemoteThumbnails(userId, cloudDetail)
                    remoteUuids.add(updatedCloudDetail.spot.uuid)

                    val localMatch = localSpots.find { it.spot.uuid == updatedCloudDetail.spot.uuid }
                    if (localMatch == null) {
                        repository.saveSyncedSpot(updatedCloudDetail)
                    } else if (shouldApplyRemoteUpdate(localMatch, updatedCloudDetail)) {
                        repository.saveSyncedSpot(updatedCloudDetail)
                    }
                }

                if (!ownQueryResult.hadDecodeFailure) {
                    localSpots.filter {
                        it.spot.effectiveOrigin == SpotOrigin.OWN &&
                            it.spot.ownerUid == userId &&
                            it.spot.isSynced
                    }.forEach { localSpot ->
                        if (localSpot.spot.uuid !in remoteUuids) {
                            println("Spot ${localSpot.spot.uuid} was deleted remotely; removing locally.")
                            repository.deleteSpot(localSpot)
                        }
                    }
                }
            }

            // 3. Pull public spots from other users
            val publicError = syncPublicSpotsInternal()
            _syncError.value = runError ?: publicError
        } catch (e: Exception) {
            _syncError.value = "Cloud sync failed: ${e.message ?: "unknown error"}"
            println("Error during JvmSyncManager syncNow: ${e.message}")
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
    }

    override suspend fun deleteSpot(uuid: String) {
        activeUserId ?: return
        try {
            val docUrl = "$firestoreOrigin/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/spots/$uuid"
            val response = client.authenticatedClient.delete(docUrl)
            if (response.status.value in 200..299) {
                println("Successfully deleted remote spot $uuid from /spots")
            } else {
                println("Failed to delete remote spot $uuid from /spots: ${response.status.value}")
            }
        } catch (e: Exception) {
            println("Error deleting remote spot $uuid: ${e.message}")
        }
    }

    override fun startRealtimeSync(userId: String) {
        activeUserId = userId
        realtimeJob?.cancel()
        realtimeJob = coroutineScope.launch {
            while (isActive) {
                try {
                    syncNow()
                } catch (e: Exception) {
                    println("Realtime polling sync error: ${e.message}")
                }
                delay(10000.milliseconds)
            }
        }
    }

    override fun stopRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        activeUserId = null
    }

    override suspend fun forceBackupAllImages(): BackupResult {
        val userId = activeUserId ?: return BackupResult(attempted = false)
        return syncMutex.withLock {
            _isSyncing.value = true
            var total = BackupResult()
            try {
                val allSpots = repository.getAllSpots().first()
                    .filter { it.spot.effectiveOrigin == SpotOrigin.OWN && it.spot.isEditableBy(userId) }
                allSpots.forEach { localDetail ->
                    total += backupSpotImagesLocked(userId, localDetail)
                }
            } catch (e: Exception) {
                total += BackupResult(failed = 1)
                println("Error during JvmSyncManager forceBackupAllImages: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
            total
        }
    }

    override suspend fun forceBackupSpot(uuid: String): BackupResult {
        val userId = activeUserId ?: return BackupResult(attempted = false)
        return syncMutex.withLock {
            _isSyncing.value = true
            var result = BackupResult()
            try {
                val localDetail = repository.getAllSpots().first()
                    .find { it.spot.uuid == uuid && it.spot.effectiveOrigin == SpotOrigin.OWN && it.spot.isEditableBy(userId) }
                if (localDetail != null) {
                    result = backupSpotImagesLocked(userId, localDetail)
                }
            } catch (e: Exception) {
                result += BackupResult(failed = 1)
                println("Error during JvmSyncManager forceBackupSpot: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
            result
        }
    }

    private suspend fun backupSpotImagesLocked(userId: String, localDetail: SpotDetails): BackupResult {
        val uuid = localDetail.spot.uuid
        var uploaded = 0
        var refreshedUrls = 0
        var failed = 0
        var skippedNoLocalFile = 0
        var spotModified = false
        var allThumbnailsReady = true
        val pendingRemoteUrlUpdates = mutableListOf<Pair<Long, String>>()

        val remoteImages = localDetail.images.map { image ->
            val objectPath = spotThumbnailStoragePath(userId, uuid, image.uuid)
            val bytes = readBytesFromFile(image.thumbnailPath) ?: readBytesFromFile(image.imagePath)
            if (bytes != null) {
                val downloadUrl = uploadBytesToStorage(objectPath, bytes)
                if (downloadUrl != null) {
                    if (image.id != 0L) {
                        pendingRemoteUrlUpdates.add(image.id to downloadUrl)
                    }
                    uploaded++
                    spotModified = true
                    image.copy(thumbnailPath = downloadUrl, imagePath = downloadUrl, remoteThumbnailUrl = downloadUrl)
                } else {
                    allThumbnailsReady = false
                    failed++
                    image
                }
            } else {
                val refreshedUrl = fetchDownloadUrlFromStorage(objectPath)
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
                val firestoreDoc = FirestoreSpotDocument.fromSpotDetails(userId, detailToPush)
                val jsonElement = client.jsonConfig.encodeToJsonElement(FirestoreSpotDocument.serializer(), firestoreDoc)
                val firestoreFields = jsonElement.jsonObject.toFirestoreValue()["mapValue"]?.jsonObject?.get("fields")?.jsonObject
                if (firestoreFields != null) {
                    val docUrl = "$firestoreOrigin/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/spots/$uuid"
                    val patchResponse = client.authenticatedClient.patch(docUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(buildJsonObject {
                            put("fields", firestoreFields)
                        })
                    }
                    if (patchResponse.status.value in 200..299) {
                        pendingRemoteUrlUpdates.forEach { (imageId, url) ->
                            repository.updateImageRemoteUrl(imageId, url)
                        }
                        val latestLocal = repository.getAllSpots().first().find { it.spot.uuid == uuid }
                        if (latestLocal == null || latestLocal.spot.lastEditedAt == localDetail.spot.lastEditedAt) {
                            repository.markSpotAsSynced(uuid)
                        }
                    } else {
                        failed++
                        println("Failed to push spot metadata after force backup for $uuid: ${patchResponse.bodyAsText()}")
                    }
                } else {
                    failed++
                }
            } catch (e: Exception) {
                failed++
                println("Error pushing spot metadata after force backup for $uuid: ${e.message}")
            }
        }

        return BackupResult(
            uploaded = uploaded,
            refreshedUrls = refreshedUrls,
            failed = failed,
            skippedNoLocalFile = skippedNoLocalFile
        )
    }

    private fun extractFirstDownloadToken(responseBody: String): String? {
        return try {
            val responseJson = client.jsonConfig.parseToJsonElement(responseBody).jsonObject
            responseJson["downloadTokens"]?.jsonPrimitive?.content
                ?.substringBefore(",")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun uploadBytesToStorage(objectPath: String, bytes: ByteArray): String? {
        val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")
        return try {
            val storageUrl = "$storageOrigin/v0/b/${JvmFirebaseConfig.storageBucket}/o?uploadType=media&name=$encodedPath"
            val storageResponse = client.authenticatedClient.post(storageUrl) {
                contentType(ContentType.Image.JPEG)
                setBody(bytes)
            }
            if (storageResponse.status.value in 200..299) {
                val token = extractFirstDownloadToken(storageResponse.bodyAsText())
                if (token != null) {
                    "$storageOrigin/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media&token=$token"
                } else {
                    println("Storage upload for $objectPath succeeded without downloadTokens; refusing to persist tokenless URL")
                    null
                }
            } else {
                println("Storage upload failed for $objectPath: ${storageResponse.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            println("Failed to upload thumbnail $objectPath: ${e.message}")
            null
        }
    }

    private suspend fun uploadOrResolveThumbnail(
        userId: String,
        spotUuid: String,
        image: net.maiatoday.tagspotter.core.model.SpotImage
    ): String? {
        val localPath = image.thumbnailPath.ifEmpty { image.imagePath }
        val objectPath = spotThumbnailStoragePath(userId, spotUuid, image.uuid)

        if (localPath.isNotEmpty() && !localPath.startsWith("http")) {
            val bytes = readBytesFromFile(localPath)
            if (bytes != null) {
                return uploadBytesToStorage(objectPath, bytes)
            }
        }

        if (image.remoteThumbnailUrl.startsWith("http")) return image.remoteThumbnailUrl
        if (image.thumbnailPath.startsWith("http")) return image.thumbnailPath

        return fetchDownloadUrlFromStorage(objectPath)
    }

    private fun isSafePathSegment(value: String): Boolean =
        value.isNotEmpty() && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun isApprovedStorageUrl(url: String): Boolean =
        url.startsWith("$storageOrigin/v0/b/${JvmFirebaseConfig.storageBucket}/")

    private fun cacheFileFor(
        cacheRoot: java.io.File,
        userId: String,
        spotUuid: String,
        imageUuid: String,
        remoteUrl: String
    ): java.io.File? {
        if (!isSafePathSegment(userId) || !isSafePathSegment(spotUuid) || !isSafePathSegment(imageUuid)) {
            return null
        }
        val urlHash = remoteUrl.hashCode().toUInt().toString(16)
        val candidate = java.io.File(cacheRoot, "$userId/$spotUuid/${imageUuid}_$urlHash.jpg").canonicalFile
        return if (candidate.path.startsWith(cacheRoot.path + java.io.File.separator)) candidate else null
    }

    private fun cleanupStaleCachedThumbnails(spotCacheDir: java.io.File, imageUuid: String, keepFileName: String) {
        spotCacheDir.listFiles()?.forEach { file ->
            if (file.isFile &&
                file.name != keepFileName &&
                (file.name == "$imageUuid.jpg" || file.name.startsWith("${imageUuid}_"))
            ) {
                runCatching { file.delete() }
            }
        }
    }

    private suspend fun resolveRemoteThumbnails(userId: String, cloudDetail: SpotDetails): SpotDetails {
        val spotUuid = cloudDetail.spot.uuid
        val cacheRoot = thumbnailCacheRoot.canonicalFile
        val updatedImages = coroutineScope {
            cloudDetail.images.map { image ->
                async {
                    val existingRemote = image.remoteThumbnailUrl.ifBlank {
                        if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
                    }
                    val resolvedRemoteUrl = existingRemote.ifEmpty {
                        val canonicalPath = spotThumbnailStoragePath(userId, spotUuid, image.uuid)
                        fetchDownloadUrlFromStorage(canonicalPath)
                            ?: fetchDownloadUrlFromStorage("users/$userId/thumbnails/${image.uuid}.jpg")
                            ?: ""
                    }

                    if (resolvedRemoteUrl.isEmpty()) {
                        return@async image
                    }

                    val localThumbnailFile = cacheFileFor(cacheRoot, userId, spotUuid, image.uuid, resolvedRemoteUrl)
                    if (localThumbnailFile != null &&
                        localThumbnailFile.exists() &&
                        localThumbnailFile.isFile &&
                        localThumbnailFile.length() > 0L
                    ) {
                        return@async image.copy(
                            thumbnailPath = localThumbnailFile.absolutePath,
                            imagePath = localThumbnailFile.absolutePath,
                            remoteThumbnailUrl = resolvedRemoteUrl
                        )
                    }

                    if (localThumbnailFile != null && isApprovedStorageUrl(resolvedRemoteUrl)) {
                        try {
                            val downloadResponse = client.authClient.get(resolvedRemoteUrl)
                            if (downloadResponse.status.value == 200) {
                                val bytes = downloadResponse.readBytes()
                                if (bytes.isNotEmpty()) {
                                    localThumbnailFile.parentFile?.mkdirs()
                                    localThumbnailFile.writeBytes(bytes)
                                    localThumbnailFile.parentFile?.let { dir ->
                                        cleanupStaleCachedThumbnails(dir, image.uuid, localThumbnailFile.name)
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Fallback to remote URL in UI
                        }
                    }

                    val resolvedPath = if (localThumbnailFile != null &&
                        localThumbnailFile.exists() &&
                        localThumbnailFile.length() > 0L
                    ) {
                        localThumbnailFile.absolutePath
                    } else {
                        resolvedRemoteUrl
                    }
                    image.copy(
                        thumbnailPath = resolvedPath,
                        imagePath = resolvedPath,
                        remoteThumbnailUrl = resolvedRemoteUrl
                    )
                }
            }.awaitAll()
        }
        return cloudDetail.copy(images = updatedImages)
    }

    private suspend fun fetchDownloadUrlFromStorage(objectPath: String): String? {
        return try {
            val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")
            val getUrl = "$storageOrigin/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath"
            val getResponse = client.authenticatedClient.get(getUrl)
            if (getResponse.status.value == 200) {
                val downloadToken = extractFirstDownloadToken(getResponse.bodyAsText())
                if (downloadToken != null) {
                    "$storageOrigin/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media&token=$downloadToken"
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
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
        try {
            val pack = net.maiatoday.tagspotter.core.model.SharedPack(
                packId = code,
                title = title,
                authorName = authorName,
                description = description,
                spots = spots
            )
            val jsonElement = client.jsonConfig.encodeToJsonElement(net.maiatoday.tagspotter.core.model.SharedPack.serializer(), pack)
            val firestoreFields = jsonElement.jsonObject.toFirestoreValue()["mapValue"]?.jsonObject?.get("fields")?.jsonObject
                ?: throw Exception("Invalid firestore serialization map fields")

            val docUrl = "$firestoreOrigin/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/shared_packs/$code"
            val patchResponse = client.authenticatedClient.patch(docUrl) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("fields", firestoreFields)
                })
            }
            if (patchResponse.status.value !in 200..299) {
                println("Failed to upload shared pack document for $code: ${patchResponse.bodyAsText()}")
            }
        } catch (e: Exception) {
            println("Error sharing pack via JVM REST client: ${e.message}")
        }
        return code
    }

    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack {
        try {
            val docUrl = "$firestoreOrigin/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/shared_packs/$code"
            val getResponse = client.authenticatedClient.get(docUrl)
            if (getResponse.status.value == 200) {
                val responseJson = client.jsonConfig.parseToJsonElement(getResponse.bodyAsText()).jsonObject
                val fields = responseJson["fields"]?.jsonObject ?: throw Exception("Invalid shared_pack fields response")
                val standardFields = fields.mapValues { it.value.jsonObject.fromFirestoreValue() }
                val standardJson = JsonObject(standardFields)
                return client.jsonConfig.decodeFromJsonElement(net.maiatoday.tagspotter.core.model.SharedPack.serializer(), standardJson)
            } else {
                println("Failed to import shared pack $code: ${getResponse.bodyAsText()}")
            }
        } catch (e: Exception) {
            println("Error importing pack via JVM REST client: ${e.message}")
        }
        return net.maiatoday.tagspotter.core.model.SharedPack(
            packId = code,
            title = "Milano Tour (Mock)",
            authorName = "Alice",
            description = "Beautiful spots around Duomo (Mock)",
            spots = emptyList()
        )
    }

    override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {
        val now = System.currentTimeMillis()
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
            val creatorUid = detail.spot.ownerUid ?: ""
            val resolvedDetail = if (creatorUid.isNotEmpty()) {
                resolveRemoteThumbnails(creatorUid, detail)
            } else {
                detail
            }
            val updatedSpot = resolvedDetail.spot.copy(
                id = 0L,
                parentPackId = sharedPack.packId,
                isImported = true,
                origin = SpotOrigin.IMPORTED,
                isSynced = true
            )
            val updatedDetail = resolvedDetail.copy(
                spot = updatedSpot,
                images = resolvedDetail.images.map { it.copy(id = 0L) },
                notes = resolvedDetail.notes.map { it.copy(id = 0L) }
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
        val result = runSpotsQuery(
            httpClient = client.authClient,
            fieldPath = "isPublic",
            value = buildJsonObject { put("booleanValue", true) }
        ) ?: return emptyList()
        return result.spots.filter { detail ->
            detail.spot.isPublic &&
                detail.spot.latitude in minLat..maxLat &&
                detail.spot.longitude in minLon..maxLon
        }
    }

    override suspend fun syncPublicSpots() {
        val error = syncPublicSpotsInternal()
        if (error != null) {
            _syncError.value = error
        }
    }

    private suspend fun syncPublicSpotsInternal(): String? {
        return publicSyncMutex.withLock {
            try {
                val result = runSpotsQuery(
                    httpClient = client.authClient,
                    fieldPath = "isPublic",
                    value = buildJsonObject { put("booleanValue", true) }
                ) ?: return@withLock "Failed to fetch public spots from cloud."

                val currentUid = activeUserId ?: repository.activeUid
                val publicSpots = result.spots.filter { it.spot.isPublic }
                val activePublicUuids = publicSpots.map { it.spot.uuid }.toSet()
                val otherUsersSpots = publicSpots.filter { detail ->
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
                if (!result.hadDecodeFailure) {
                    repository.pruneRemovedPublicSpots(activePublicUuids)
                }
                null
            } catch (e: Exception) {
                println("Error during JvmSyncManager syncPublicSpots: ${e.message}")
                "Failed to sync public spots: ${e.message ?: "unknown error"}"
            }
        }
    }

    private data class QueryResult(
        val spots: List<SpotDetails>,
        val hadDecodeFailure: Boolean
    )

    private suspend fun runSpotsQuery(
        httpClient: HttpClient,
        fieldPath: String,
        value: JsonObject
    ): QueryResult? {
        return try {
            val queryUrl = "$firestoreOrigin/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents:runQuery"
            val requestBody = buildJsonObject {
                putJsonObject("structuredQuery") {
                    putJsonArray("from") {
                        addJsonObject { put("collectionId", "spots") }
                    }
                    putJsonObject("where") {
                        putJsonObject("fieldFilter") {
                            putJsonObject("field") { put("fieldPath", fieldPath) }
                            put("op", "EQUAL")
                            put("value", value)
                        }
                    }
                }
            }
            val response = httpClient.post(queryUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status.value != 200) {
                println("Firestore runQuery failed ($fieldPath): ${response.status.value} ${response.bodyAsText()}")
                return null
            }
            val rows = client.jsonConfig.parseToJsonElement(response.bodyAsText()).jsonArray
            var hadDecodeFailure = false
            val spots = rows.mapNotNull { rowElement ->
                val document = rowElement.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
                val fields = document["fields"]?.jsonObject ?: return@mapNotNull null
                try {
                    val standardFields = fields.mapValues { it.value.jsonObject.fromFirestoreValue() }
                    val standardJsonObj = JsonObject(standardFields)
                    client.jsonConfig.decodeFromJsonElement(FirestoreSpotDocument.serializer(), standardJsonObj).toSpotDetails()
                } catch (e: Exception) {
                    hadDecodeFailure = true
                    println("Error decoding spot document from runQuery: ${e.message}")
                    null
                }
            }
            QueryResult(spots = spots, hadDecodeFailure = hadDecodeFailure)
        } catch (e: Exception) {
            println("Error running Firestore query ($fieldPath): ${e.message}")
            null
        }
    }
}
