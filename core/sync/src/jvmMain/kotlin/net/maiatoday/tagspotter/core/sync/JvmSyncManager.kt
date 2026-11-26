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
    private val firestoreOrigin: String = "https://firestore.googleapis.com"
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val syncMutex = Mutex()
    private val publicSyncMutex = Mutex()

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
                    println("Failed to push spot metadata for $uuid: ${patchResponse.bodyAsText()}")
                }
            }

            // 2. Pull stage: fetch user's own spots via runQuery on /spots where ownerUid == userId
            val ownQueryResult = runSpotsQuery(
                httpClient = client.authenticatedClient,
                fieldPath = "ownerUid",
                value = buildJsonObject { put("stringValue", userId) }
            )
            if (ownQueryResult != null) {
                val localSpots = repository.getAllSpots().first()
                val remoteUuids = mutableSetOf<String>()

                ownQueryResult.spots.forEach { cloudDetail ->
                    val updatedCloudDetail = resolveRemoteThumbnails(userId, cloudDetail)
                    remoteUuids.add(updatedCloudDetail.spot.uuid)

                    val localMatch = localSpots.find { it.spot.uuid == updatedCloudDetail.spot.uuid }
                    if (localMatch == null) {
                        repository.saveSyncedSpot(updatedCloudDetail)
                    } else if (updatedCloudDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt ||
                        updatedCloudDetail.images.size != localMatch.images.size ||
                        updatedCloudDetail.notes.size != localMatch.notes.size ||
                        localMatch.images.any { localImg ->
                            val remoteImg = updatedCloudDetail.images.find { it.uuid == localImg.uuid }
                            remoteImg != null && remoteImg.remoteThumbnailUrl.isNotBlank() && localImg.remoteThumbnailUrl != remoteImg.remoteThumbnailUrl
                        }
                    ) {
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
            syncPublicSpots()
        } catch (e: Exception) {
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

    private suspend fun uploadOrResolveThumbnail(
        userId: String,
        spotUuid: String,
        image: net.maiatoday.tagspotter.core.model.SpotImage
    ): String? {
        val localPath = image.thumbnailPath.ifEmpty { image.imagePath }
        val objectPath = spotThumbnailStoragePath(userId, spotUuid, image.uuid)
        val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")

        if (localPath.isNotEmpty() && !localPath.startsWith("http")) {
            val bytes = readBytesFromFile(localPath)
            if (bytes != null) {
                return try {
                    val storageUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o?uploadType=media&name=$encodedPath"
                    val storageResponse = client.authenticatedClient.post(storageUrl) {
                        contentType(ContentType.Image.JPEG)
                        setBody(bytes)
                    }
                    if (storageResponse.status.value in 200..299) {
                        val token = try {
                            val responseJson = client.jsonConfig.parseToJsonElement(storageResponse.bodyAsText()).jsonObject
                            responseJson["downloadTokens"]?.jsonPrimitive?.content
                        } catch (_: Exception) {
                            null
                        }
                        if (!token.isNullOrEmpty()) {
                            "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media&token=$token"
                        } else {
                            "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media"
                        }
                    } else {
                        println("Storage upload failed for ${image.uuid}: ${storageResponse.bodyAsText()}")
                        null
                    }
                } catch (e: Exception) {
                    println("Failed to upload thumbnail ${image.uuid}: ${e.message}")
                    null
                }
            }
        }

        if (image.remoteThumbnailUrl.startsWith("http")) return image.remoteThumbnailUrl
        if (image.thumbnailPath.startsWith("http")) return image.thumbnailPath

        return try {
            val metadataUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath"
            val metadataResponse = client.authenticatedClient.get(metadataUrl)
            if (metadataResponse.status.value == 200) {
                val responseJson = client.jsonConfig.parseToJsonElement(metadataResponse.bodyAsText()).jsonObject
                val token = responseJson["downloadTokens"]?.jsonPrimitive?.content
                if (!token.isNullOrEmpty()) {
                    "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media&token=$token"
                } else {
                    "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media"
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveRemoteThumbnails(userId: String, cloudDetail: SpotDetails): SpotDetails {
        val spotUuid = cloudDetail.spot.uuid
        val updatedImages = cloudDetail.images.map { image ->
            val userHome = System.getProperty("user.home")
            val localThumbnailFile = java.io.File("$userHome/Pictures/TagSpotter/thumbnails/$userId/${image.uuid}.jpg")
            val existingRemote = image.remoteThumbnailUrl.ifBlank {
                if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
            }

            if (localThumbnailFile.exists() && localThumbnailFile.isFile) {
                image.copy(
                    thumbnailPath = localThumbnailFile.absolutePath,
                    remoteThumbnailUrl = existingRemote
                )
            } else if (existingRemote.isNotEmpty()) {
                try {
                    val downloadResponse = client.authClient.get(existingRemote)
                    if (downloadResponse.status.value == 200) {
                        val bytes = downloadResponse.readBytes()
                        localThumbnailFile.parentFile?.mkdirs()
                        localThumbnailFile.writeBytes(bytes)
                    }
                } catch (_: Exception) {
                    // Fallback to remote URL in UI
                }
                image.copy(
                    thumbnailPath = if (localThumbnailFile.exists()) localThumbnailFile.absolutePath else existingRemote,
                    imagePath = if (localThumbnailFile.exists()) localThumbnailFile.absolutePath else existingRemote,
                    remoteThumbnailUrl = existingRemote
                )
            } else {
                val canonicalPath = spotThumbnailStoragePath(userId, spotUuid, image.uuid)
                val resolvedUrl = fetchDownloadUrlFromStorage(canonicalPath)
                    ?: fetchDownloadUrlFromStorage("users/$userId/thumbnails/${image.uuid}.jpg")
                if (resolvedUrl != null) {
                    image.copy(thumbnailPath = resolvedUrl, imagePath = resolvedUrl, remoteThumbnailUrl = resolvedUrl)
                } else {
                    image
                }
            }
        }
        return cloudDetail.copy(images = updatedImages)
    }

    private suspend fun fetchDownloadUrlFromStorage(objectPath: String): String? {
        return try {
            val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")
            val getUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath"
            val getResponse = client.authenticatedClient.get(getUrl)
            if (getResponse.status.value == 200) {
                val responseJson = client.jsonConfig.parseToJsonElement(getResponse.bodyAsText()).jsonObject
                val downloadToken = responseJson["downloadTokens"]?.jsonPrimitive?.content
                if (!downloadToken.isNullOrEmpty()) {
                    "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media&token=$downloadToken"
                } else {
                    "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media"
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
        publicSyncMutex.withLock {
            try {
                val result = runSpotsQuery(
                    httpClient = client.authClient,
                    fieldPath = "isPublic",
                    value = buildJsonObject { put("booleanValue", true) }
                ) ?: return

                val currentUid = activeUserId ?: repository.activeUid
                val activePublicUuids = result.spots.filter { it.spot.isPublic }.map { it.spot.uuid }.toSet()

                result.spots.filter { it.spot.isPublic }.forEach { detail ->
                    if (currentUid != null && detail.spot.ownerUid == currentUid) {
                        return@forEach
                    }
                    repository.savePublicSpot(detail)
                }
                if (!result.hadDecodeFailure) {
                    repository.pruneRemovedPublicSpots(activePublicUuids)
                }
            } catch (e: Exception) {
                println("Error during JvmSyncManager syncPublicSpots: ${e.message}")
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
