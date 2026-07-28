package net.maiatoday.tagspotter.core.sync

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
import kotlinx.serialization.json.*
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.SpotDetails

fun JsonElement.toFirestoreValue(): JsonObject {
    val element = this
    return when (element) {
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
    private val client: JvmFirebaseClient
) : SyncManager {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

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

            unsyncedSpots.forEach { localDetail ->
                val uuid = localDetail.spot.uuid
                
                // Upload thumbnail attachments if any and if local FIRST
                val sanitizedImages = localDetail.images.map { image ->
                    val path = image.thumbnailPath.ifEmpty { image.imagePath }
                    var resolvedUrl = if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
                    
                    if (path.isNotEmpty() && !path.startsWith("http")) {
                        try {
                            val bytes = readBytesFromFile(path)
                            if (bytes != null) {
                                val objectPath = "users/$userId/thumbnails/${image.uuid}.jpg"
                                val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")
                                val storageUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o?uploadType=media&name=$encodedPath"
                                val storageResponse = client.authenticatedClient.post(storageUrl) {
                                    contentType(ContentType.Image.JPEG)
                                    setBody(bytes)
                                }
                                if (storageResponse.status.value in 200..299) {
                                    resolvedUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/users%2F$userId%2Fthumbnails%2F${image.uuid}.jpg?alt=media"
                                } else {
                                    println("Storage upload failed for ${image.uuid}: ${storageResponse.bodyAsText()}")
                                }
                            }
                        } catch (e: Exception) {
                            println("Failed to upload thumbnail ${image.uuid}: ${e.message}")
                        }
                    }
                    
                    val finalUrl = if (resolvedUrl.isNotEmpty()) resolvedUrl else "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/users%2F$userId%2Fthumbnails%2F${image.uuid}.jpg?alt=media"
                    image.copy(
                        thumbnailPath = if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else finalUrl,
                        imagePath = if (image.imagePath.startsWith("http")) image.imagePath else finalUrl
                    )
                }

                val detailToPush = localDetail.copy(images = sanitizedImages)
                
                // Convert SpotDetails to standard JSON, then to Firestore structured value format
                val jsonElement = client.jsonConfig.encodeToJsonElement(SpotDetails.serializer(), detailToPush)
                val firestoreFields = jsonElement.jsonObject.toFirestoreValue()["mapValue"]?.jsonObject?.get("fields")?.jsonObject 
                    ?: throw Exception("Invalid firestore serialization map fields")

                // Upload local metadata with sanitized HTTPS URLs to Firestore
                val docUrl = "https://firestore.googleapis.com/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/users/$userId/spots/$uuid"
                val patchResponse = client.authenticatedClient.patch(docUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("fields", firestoreFields)
                    })
                }

                if (patchResponse.status.value !in 200..299) {
                    println("Failed to push spot metadata for $uuid: ${patchResponse.bodyAsText()}")
                }

                // Mark as successfully synced locally
                repository.markSpotAsSynced(uuid)
            }

            // Ensure all local spots with local image files have uploaded their thumbnails/images to Firebase Storage
            val allLocalSpots = repository.getAllSpots().first()
            allLocalSpots.forEach { localDetail ->
                localDetail.images.forEach { image ->
                    val path = image.thumbnailPath.ifEmpty { image.imagePath }
                    if (path.isNotEmpty() && !path.startsWith("http")) {
                        try {
                            val bytes = readBytesFromFile(path)
                            if (bytes != null) {
                                val objectPath = "users/$userId/thumbnails/${image.uuid}.jpg"
                                val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")
                                val storageUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o?uploadType=media&name=$encodedPath"
                                val storageResponse = client.authenticatedClient.post(storageUrl) {
                                    contentType(ContentType.Image.JPEG)
                                    setBody(bytes)
                                }
                                if (storageResponse.status.value in 200..299) {
                                    println("Successfully uploaded thumbnail/image to Storage for ${image.uuid}")
                                } else {
                                    println("Storage upload failed for ${image.uuid}: ${storageResponse.bodyAsText()}")
                                }
                            }
                        } catch (e: Exception) {
                            println("Failed to upload thumbnail ${image.uuid}: ${e.message}")
                        }
                    }
                }
            }

            // 2. Pull stage: fetch remote spots and apply Last-Write-Wins
            val listUrl = "https://firestore.googleapis.com/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/users/$userId/spots"
            val getResponse = client.authenticatedClient.get(listUrl)

            if (getResponse.status.value == 200) {
                val getResponseText = getResponse.bodyAsText()
                val responseJson = client.jsonConfig.parseToJsonElement(getResponseText).jsonObject
                val documents = responseJson["documents"]?.jsonArray
                if (documents != null) {
                    val localSpots = repository.getAllSpots().first()
                    val remoteUuids = mutableSetOf<String>()
                    documents.forEach { docElement ->
                        try {
                            val docObj = docElement.jsonObject
                            val fields = docObj["fields"]?.jsonObject
                            if (fields != null) {
                                val standardFields = fields.mapValues { it.value.jsonObject.fromFirestoreValue() }
                                val standardJsonObj = JsonObject(standardFields)
                                val cloudDetail = client.jsonConfig.decodeFromJsonElement(SpotDetails.serializer(), standardJsonObj)
                                val updatedCloudDetail = resolveRemoteThumbnails(userId, cloudDetail)
                                remoteUuids.add(updatedCloudDetail.spot.uuid)
                                
                                val localMatch = localSpots.find { it.spot.uuid == updatedCloudDetail.spot.uuid }
                                if (localMatch == null) {
                                    repository.saveSyncedSpot(updatedCloudDetail)
                                } else if (updatedCloudDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt ||
                                           updatedCloudDetail.images.size != localMatch.images.size ||
                                           updatedCloudDetail.notes.size != localMatch.notes.size) {
                                    repository.saveSyncedSpot(updatedCloudDetail)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error parsing pulled spot: ${e.message}")
                        }
                    }

                    // Remove local synced spots that were deleted remotely
                    localSpots.filter { it.spot.ownerUid == userId && it.spot.isSynced }.forEach { localSpot ->
                        if (localSpot.spot.uuid !in remoteUuids) {
                            println("Spot ${localSpot.spot.uuid} was deleted remotely; removing locally.")
                            repository.deleteSpot(localSpot)
                        }
                    }
                }
            } else if (getResponse.status.value == 404) {
                println("No spots collection found in Firestore yet (404).")
            } else {
                println("Failed to pull spots: ${getResponse.bodyAsText()}")
            }

        } catch (e: Exception) {
            println("Error during JvmSyncManager syncNow: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    override suspend fun deleteSpot(uuid: String) {
        val userId = activeUserId ?: return
        try {
            val docUrl = "https://firestore.googleapis.com/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/users/$userId/spots/$uuid"
            val response = client.authenticatedClient.delete(docUrl)
            if (response.status.value in 200..299) {
                println("Successfully deleted remote spot $uuid from Firestore")
            } else {
                println("Failed to delete remote spot $uuid from Firestore: ${response.status.value}")
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
                delay(10000)
            }
        }
    }

    override fun stopRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        activeUserId = null
    }

    private suspend fun resolveRemoteThumbnails(userId: String, cloudDetail: SpotDetails): SpotDetails {
        val updatedImages = cloudDetail.images.map { image ->
            val userHome = System.getProperty("user.home")
            val localThumbnailFile = java.io.File("$userHome/Pictures/TagSpotter/thumbnails/$userId/${image.uuid}.jpg")
            
            if (localThumbnailFile.exists() && localThumbnailFile.isFile) {
                // If it exists locally on disk, save the local path inside the local database `thumbnailPath` field.
                image.copy(thumbnailPath = localThumbnailFile.absolutePath)
            } else {
                try {
                    val objectPath = "users/$userId/thumbnails/${image.uuid}.jpg"
                    val encodedPath = java.net.URLEncoder.encode(objectPath, "UTF-8")
                    val getUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath"
                    val getResponse = client.authenticatedClient.get(getUrl)
                    if (getResponse.status.value == 200) {
                        val getResponseText = getResponse.bodyAsText()
                        val responseJson = client.jsonConfig.parseToJsonElement(getResponseText).jsonObject
                        val downloadToken = responseJson["downloadTokens"]?.jsonPrimitive?.content
                        if (!downloadToken.isNullOrEmpty()) {
                            val secureUrl = "https://firebasestorage.googleapis.com/v0/b/${JvmFirebaseConfig.storageBucket}/o/$encodedPath?alt=media&token=$downloadToken"
                            
                            // Download the secure URL bytes
                            val downloadResponse = client.authenticatedClient.get(secureUrl)
                            if (downloadResponse.status.value == 200) {
                                val bytes = downloadResponse.readBytes()
                                localThumbnailFile.parentFile?.mkdirs()
                                localThumbnailFile.writeBytes(bytes)
                                println("Downloaded and persisted remote thumbnail for ${image.uuid} to ${localThumbnailFile.absolutePath}")
                            }
                            
                            // Save the remote HTTPS download URL directly inside the local database `thumbnailPath` field.
                            image.copy(thumbnailPath = secureUrl)
                        } else {
                            image
                        }
                    } else {
                        println("Failed to fetch storage metadata for ${image.uuid}: ${getResponse.bodyAsText()}")
                        image
                    }
                } catch (e: Exception) {
                    println("Error resolving remote thumbnail for ${image.uuid}: ${e.message}")
                    image
                }
            }
        }
        return cloudDetail.copy(images = updatedImages)
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

            val docUrl = "https://firestore.googleapis.com/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/shared_packs/$code"
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
            val docUrl = "https://firestore.googleapis.com/v1/projects/${JvmFirebaseConfig.projectId}/databases/(default)/documents/shared_packs/$code"
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
                isImported = true
            )
            val updatedDetail = resolvedDetail.copy(
                spot = updatedSpot,
                images = resolvedDetail.images.map { it.copy(id = 0L) },
                notes = resolvedDetail.notes.map { it.copy(id = 0L) }
            )
            repository.saveImportedSpot(updatedDetail)
        }
    }
}
