package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.model.generateUuid

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun webSaveSpotsMetadata(jsonString: String): Unit = js("window.webSaveSpotsMetadata(jsonString)")
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun webLoadSpotsMetadata(): String = js("window.webLoadSpotsMetadata()")
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun webExportPackZip(spotsJson: String, imagesDataJson: String): Unit = js("window.webExportPackZip(spotsJson, imagesDataJson)")

class WasmSpotRepository : SpotRepository {
    override var activeUid: String? = null
    private val spotsFlow: MutableStateFlow<Map<Long, SpotDetails>> = MutableStateFlow(emptyMap<Long, SpotDetails>())
    private var nextSpotId = 1L
    private var nextImageId = 1L
    private var nextNoteId = 1L

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
    }

    init {
        try {
            val savedJson = webLoadSpotsMetadata()
            if (savedJson.isNotEmpty()) {
                val list = json.decodeFromString(ListSerializer(SpotDetails.serializer()), savedJson)
                val map = list.associateBy { it.spot.id }
                spotsFlow.value = map
                nextSpotId = (map.keys.maxOrNull() ?: 0L) + 1L
                nextImageId = (list.flatMap { it.images }.map { it.id }.maxOrNull() ?: 0L) + 1L
                nextNoteId = (list.flatMap { it.notes }.map { it.id }.maxOrNull() ?: 0L) + 1L
            }
        } catch (e: Exception) {
            println("Error loading spots from web localStorage: ${e.message}")
        }
    }

    private fun persistToLocalStorage() {
        try {
            val list = spotsFlow.value.values.toList()
            val jsonStr = json.encodeToString(ListSerializer(SpotDetails.serializer()), list)
            webSaveSpotsMetadata(jsonStr)
        } catch (e: Exception) {
            println("Error saving spots to web localStorage: ${e.message}")
        }
    }

    fun exportPackData() {
        try {
            val list = spotsFlow.value.values.toList()
            val backupWrapper = net.maiatoday.tagspotter.core.model.BackupWrapper(backupVersion = 2, spots = list)
            val jsonStr = json.encodeToString(net.maiatoday.tagspotter.core.model.BackupWrapper.serializer(), backupWrapper)
            val imagesMap = mutableMapOf<String, String>()
            list.forEach { detail ->
                detail.images.forEach { img ->
                    if (img.imagePath.isNotEmpty()) {
                        val imgFileName = img.imagePath.substringAfterLast('/')
                        if (imgFileName.isNotEmpty()) {
                            imagesMap["images/$imgFileName"] = img.imagePath
                        }
                    }
                    if (img.thumbnailPath.isNotEmpty()) {
                        val thumbFileName = img.thumbnailPath.substringAfterLast('/')
                        if (thumbFileName.isNotEmpty()) {
                            imagesMap["thumbnails/$thumbFileName"] = img.thumbnailPath
                        }
                    }
                }
            }
            val imagesJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), imagesMap)
            webExportPackZip(jsonStr, imagesJson)
        } catch (e: Exception) {
            println("Error exporting web backup pack: ${e.message}")
        }
    }

    fun importPackData(spotsJson: String, imagesJsonMap: String): Int {
        return try {
            val list: List<SpotDetails> = try {
                val wrapper = json.decodeFromString(net.maiatoday.tagspotter.core.model.BackupWrapper.serializer(), spotsJson)
                wrapper.spots
            } catch (e: Exception) {
                try {
                    json.decodeFromString(ListSerializer(SpotDetails.serializer()), spotsJson)
                } catch (ex: Exception) {
                    println("Failed to parse spots.json: ${ex.message}")
                    emptyList()
                }
            }

            val imagesMap: Map<String, String> = try {
                json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), imagesJsonMap)
            } catch (e: Exception) {
                emptyMap()
            }

            val current = spotsFlow.value.toMutableMap()
            var importedCount = 0

            list.forEach { detail ->
                val updatedImages = detail.images.map { img ->
                    val imgFileName = img.imagePath.substringAfterLast('/')
                    val thumbFileName = img.thumbnailPath.substringAfterLast('/')

                    // Lookup full image data URL
                    val resolvedImagePath = imagesMap["images/$imgFileName"]
                        ?: imagesMap.entries.find { it.key.startsWith("images/") && (it.key.endsWith(imgFileName) || (img.uuid.isNotEmpty() && it.key.contains(img.uuid))) }?.value
                        ?: if (img.imagePath.startsWith("data:") || img.imagePath.startsWith("http")) img.imagePath else ""

                    // Lookup thumbnail data URL
                    val resolvedThumbPath = imagesMap["thumbnails/$thumbFileName"]
                        ?: imagesMap.entries.find { it.key.startsWith("thumbnails/") && (it.key.endsWith(thumbFileName) || (img.uuid.isNotEmpty() && it.key.contains(img.uuid))) }?.value
                        ?: imagesMap["images/$imgFileName"]
                        ?: resolvedImagePath
                        ?: if (img.thumbnailPath.startsWith("data:") || img.thumbnailPath.startsWith("http")) img.thumbnailPath else ""

                    img.copy(
                        imagePath = resolvedImagePath,
                        thumbnailPath = resolvedThumbPath
                    )
                }
                val updatedDetail = detail.copy(images = updatedImages)

                val existing = current.values.find { it.spot.uuid == detail.spot.uuid }
                val spotId = existing?.spot?.id ?: nextSpotId++
                val finalDetail = updatedDetail.copy(spot = updatedDetail.spot.copy(id = spotId))
                current[spotId] = finalDetail
                importedCount++
            }

            spotsFlow.value = current
            persistToLocalStorage()
            importedCount
        } catch (e: Exception) {
            println("Error importing web backup pack: ${e.message}")
            0
        }
    }

    override fun getAllSpots(): Flow<List<SpotDetails>> {
        return spotsFlow.map { it.values.toList().sortedByDescending { detail -> detail.spot.createdAt } }
    }

    override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> {
        return getAllSpots().map { list ->
            if (category == "All") list else list.filter { it.spot.category == category }
        }
    }

    override fun getSpotById(id: Long): Flow<SpotDetails?> {
        return spotsFlow.map { it[id] }
    }

    override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Long, isMain: Boolean): Long {
        val now = epochMillis()
        val spotId = if (spot.id == 0L) nextSpotId++ else spot.id
        val newSpot = spot.copy(
            id = spotId,
            uuid = if (spot.uuid.isEmpty()) generateUuid() else spot.uuid,
            lastEditedAt = now,
            isSynced = false
        )
        val images = if (imagePath.isNotEmpty()) {
            listOf(
                SpotImage(
                    id = nextImageId++,
                    spotId = spotId,
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    timestamp = spot.createdAt,
                    rating = rating,
                    isMain = isMain,
                    uuid = generateUuid(),
                    lastEditedAt = now
                )
            )
        } else {
            emptyList()
        }
        val details = SpotDetails(newSpot, images, emptyList())
        spotsFlow.value = spotsFlow.value + (spotId to details)
        persistToLocalStorage()
        return spotId
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Long, isMain: Boolean): Long {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return 0L
        val imgId = nextImageId++
        val now = epochMillis()
        val newImage = SpotImage(
            id = imgId,
            spotId = spotId,
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            timestamp = timestamp,
            rating = rating,
            isMain = isMain,
            uuid = generateUuid(),
            lastEditedAt = now
        )
        val updatedImages = current.images + newImage
        val updatedSpot = current.spot.copy(lastEditedAt = now, isSynced = false)
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = updatedSpot, images = updatedImages))
        persistToLocalStorage()
        return imgId
    }

    override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return 0L
        val noteId = nextNoteId++
        val now = epochMillis()
        val newNote = SpotNote(
            id = noteId,
            spotId = spotId,
            noteText = noteText,
            timestamp = timestamp,
            uuid = generateUuid(),
            lastEditedAt = now
        )
        val updatedNotes = current.notes + newNote
        val updatedSpot = current.spot.copy(lastEditedAt = now, isSynced = false)
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = updatedSpot, notes = updatedNotes))
        persistToLocalStorage()
        return noteId
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(status = status, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun updateSpotCategory(spotId: Long, category: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(category = category, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(artists = artists, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(photographer = photographer, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(tags = tags, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(latitude = latitude, longitude = longitude, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(description = description, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun deleteSpot(spotDetails: SpotDetails) {
        spotsFlow.value = spotsFlow.value - spotDetails.spot.id
        persistToLocalStorage()
    }

    override fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>> {
        return getAllSpots().map { list ->
            list.asSequence().flatMap { it.spot.tags }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !predefinedTags.contains(it) }
                .distinct()
                .take(15).toList()
        }
    }

    override suspend fun loadTestData() {
        val now = epochMillis()
        saveSpot(
            Spot(
                id = 9001L,
                latitude = 45.4642,
                longitude = 9.1899,
                createdAt = now - 86400000 * 2,
                description = "Stunning street art stencil near the Duomo in Milan.",
                tags = listOf("milan", "stencil", "duomo"),
                category = "graffiti",
                status = "active",
                artists = listOf("Mr. Brainwash"),
                photographer = "Mock Photographer"
            ),
            imagePath = "fake_image_1",
            thumbnailPath = "fake_thumb_1"
        )
        saveSpot(
            Spot(
                id = 9002L,
                latitude = 51.5074,
                longitude = -0.1278,
                createdAt = now - 86400000 * 1,
                description = "Statue of a famous historical figure in central London.",
                tags = listOf("london", "statue", "history"),
                category = "sculpture",
                status = "active",
                artists = listOf("Famous Sculptor"),
                photographer = "Mock Photographer"
            ),
            imagePath = "fake_image_2",
            thumbnailPath = "fake_thumb_2"
        )
    }

    override suspend fun unloadTestData() {
        spotsFlow.value = spotsFlow.value - 9001L - 9002L - 9003L
        persistToLocalStorage()
    }

    override suspend fun importPack(
        packFilePath: String,
        filesDir: String,
        cacheDir: String,
        currentPhotographerName: String,
        createThumbnail: suspend (String) -> String?
    ): Int {
        return 0
    }

    override suspend fun saveSpotDetails(spotDetails: SpotDetails): Long {
        val spotId = if (spotDetails.spot.id == 0L) nextSpotId++ else spotDetails.spot.id
        val updatedSpot = spotDetails.spot.copy(id = spotId)
        val updatedImages = spotDetails.images.map { image ->
            image.copy(id = if (image.id == 0L) nextImageId++ else image.id, spotId = spotId)
        }
        val updatedNotes = spotDetails.notes.map { note ->
            note.copy(id = if (note.id == 0L) nextNoteId++ else note.id, spotId = spotId)
        }
        val finalDetails = SpotDetails(updatedSpot, updatedImages, updatedNotes)
        spotsFlow.value = spotsFlow.value + (spotId to finalDetails)
        persistToLocalStorage()
        return spotId
    }

    override suspend fun importSpots(spots: List<SpotDetails>): Int {
        var count = 0
        spots.forEach { detail ->
            saveSpot(detail.spot, detail.images.firstOrNull()?.imagePath ?: "", detail.images.firstOrNull()?.thumbnailPath ?: "")
            count++
        }
        return count
    }

    override suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(isStarred = isStarred, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun getStarredSpots(): List<Spot> {
        return spotsFlow.value.values.map { it.spot }.filter { it.isStarred }
    }

    override suspend fun getStarredSpotsCount(): Int {
        return getStarredSpots().size
    }

    override suspend fun setMainImage(spotId: Long, imageId: Long) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        val updatedImages = current.images.map { it.copy(isMain = it.id == imageId) }
        val updatedSpot = current.spot.copy(lastEditedAt = now, isSynced = false)
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = updatedSpot, images = updatedImages))
        persistToLocalStorage()
    }

    override suspend fun deleteImage(image: SpotImage) {
        val current: SpotDetails = spotsFlow.value[image.spotId] ?: return
        val now = epochMillis()
        val updatedImages = current.images.filter { it.id != image.id }
        val updatedSpot = current.spot.copy(lastEditedAt = now, isSynced = false)
        spotsFlow.value = spotsFlow.value + (image.spotId to current.copy(spot = updatedSpot, images = updatedImages))
        persistToLocalStorage()
    }

    override suspend fun updateImageRating(imageId: Long, rating: Long) {
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value.mapValues { entry ->
            val details = entry.value
            var ratingChanged = false
            val updatedImages = details.images.map {
                if (it.id == imageId) {
                    ratingChanged = true
                    it.copy(rating = rating, lastEditedAt = now)
                } else it
            }
            if (ratingChanged) {
                details.copy(spot = details.spot.copy(lastEditedAt = now, isSynced = false), images = updatedImages)
            } else {
                details
            }
        }
        persistToLocalStorage()
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(artworkDate = artworkDate, lastEditedAt = now, isSynced = false)
        ))
        persistToLocalStorage()
    }

    override suspend fun deleteNote(noteId: Long) {
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value.mapValues { entry ->
            val details = entry.value
            val hasNote = details.notes.any { it.id == noteId }
            if (hasNote) {
                details.copy(
                    spot = details.spot.copy(lastEditedAt = now, isSynced = false),
                    notes = details.notes.filter { it.id != noteId }
                )
            } else {
                details
            }
        }
        persistToLocalStorage()
    }

    override suspend fun updateNote(noteId: Long, noteText: String) {
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value.mapValues { entry ->
            val details = entry.value
            var noteChanged = false
            val updatedNotes = details.notes.map {
                if (it.id == noteId) {
                    noteChanged = true
                    it.copy(noteText = noteText, lastEditedAt = now)
                } else it
            }
            if (noteChanged) {
                details.copy(spot = details.spot.copy(lastEditedAt = now, isSynced = false), notes = updatedNotes)
            } else {
                details
            }
        }
        persistToLocalStorage()
    }

    override suspend fun getUnsyncedSpots(): List<SpotDetails> {
        return spotsFlow.value.values.filter { !it.spot.isSynced }
    }

    override suspend fun markSpotAsSynced(spotUuid: String) {
        spotsFlow.value = spotsFlow.value.mapValues { entry ->
            val details = entry.value
            if (details.spot.uuid == spotUuid) {
                details.copy(spot = details.spot.copy(isSynced = true))
            } else {
                details
            }
        }
        persistToLocalStorage()
    }

    override suspend fun saveSyncedSpot(spotDetails: SpotDetails) {
        val existingEntry = spotsFlow.value.entries.find { it.value.spot.uuid == spotDetails.spot.uuid }
        val finalSpotId = existingEntry?.key ?: nextSpotId++
        
        val spotToSave = spotDetails.spot.copy(id = finalSpotId, isSynced = true)
        
        val imagesToSave = spotDetails.images.map { image ->
            val existingImage = existingEntry?.value?.images?.find { it.uuid == image.uuid }
            val finalImgId = existingImage?.id ?: nextImageId++
            image.copy(id = finalImgId, spotId = finalSpotId)
        }
        
        val notesToSave = spotDetails.notes.map { note ->
            val existingNote = existingEntry?.value?.notes?.find { it.uuid == note.uuid }
            val finalNoteId = existingNote?.id ?: nextNoteId++
            note.copy(id = finalNoteId, spotId = finalSpotId)
        }
        
        val updatedDetails = SpotDetails(spotToSave, imagesToSave, notesToSave)
        spotsFlow.value = spotsFlow.value + (finalSpotId to updatedDetails)
        persistToLocalStorage()
    }

    override suspend fun adoptLocalSpots(userUid: String, backup: Boolean) {}
    override suspend fun clearUserCache(userUid: String) {}

    private val loadedPacksMap = mutableMapOf<String, net.maiatoday.tagspotter.core.model.LoadedPack>()
    private val loadedPacksFlow = MutableStateFlow<List<net.maiatoday.tagspotter.core.model.LoadedPack>>(emptyList())

    override fun getAllLoadedPacks(): Flow<List<net.maiatoday.tagspotter.core.model.LoadedPack>> = loadedPacksFlow

    override suspend fun saveLoadedPack(pack: net.maiatoday.tagspotter.core.model.LoadedPack) {
        loadedPacksMap[pack.packId] = pack
        loadedPacksFlow.value = loadedPacksMap.values.toList()
    }

    override suspend fun saveImportedSpot(spotDetails: SpotDetails) {
        val currentMap = spotsFlow.value.toMutableMap()
        val existing = currentMap.values.find { it.spot.uuid == spotDetails.spot.uuid }
        if (existing != null) {
            val spotId = existing.spot.id
            val updatedSpot = spotDetails.spot.copy(id = spotId)
            val updatedImages = spotDetails.images.map { img ->
                val existingImg = existing.images.find { it.uuid == img.uuid }
                img.copy(id = existingImg?.id ?: 0L, spotId = spotId)
            }
            val updatedNotes = spotDetails.notes.map { note ->
                val existingNote = existing.notes.find { it.uuid == note.uuid }
                note.copy(id = existingNote?.id ?: 0L, spotId = spotId)
            }
            currentMap[spotId] = SpotDetails(updatedSpot, updatedImages, updatedNotes)
        } else {
            val nextId = (currentMap.keys.maxOrNull() ?: 0L) + 1L
            val updatedSpot = spotDetails.spot.copy(id = nextId)
            val updatedImages = spotDetails.images.mapIndexed { idx, img ->
                img.copy(id = idx + 1L, spotId = nextId)
            }
            val updatedNotes = spotDetails.notes.mapIndexed { idx, note ->
                note.copy(id = idx + 1L, spotId = nextId)
            }
            currentMap[nextId] = SpotDetails(updatedSpot, updatedImages, updatedNotes)
        }
        spotsFlow.value = currentMap
        persistToLocalStorage()
    }

    override suspend fun deleteLoadedPack(packId: String) {
        loadedPacksMap.remove(packId)
        loadedPacksFlow.value = loadedPacksMap.values.toList()
        
        spotsFlow.value = spotsFlow.value.filterValues { it.spot.parentPackId != packId }
        persistToLocalStorage()
    }
}
