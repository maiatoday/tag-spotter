package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.model.generateUuid

class WasmSpotRepository : SpotRepository {
    private val spotsFlow: MutableStateFlow<Map<Long, SpotDetails>> = MutableStateFlow(emptyMap<Long, SpotDetails>())
    private var nextSpotId = 1L
    private var nextImageId = 1L
    private var nextNoteId = 1L

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

    override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Int, isMain: Boolean): Long {
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
        return spotId
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Int, isMain: Boolean): Long {
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
        return noteId
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(status = status, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun updateSpotCategory(spotId: Long, category: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(category = category, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(artists = artists, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(photographer = photographer, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(tags = tags, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(latitude = latitude, longitude = longitude, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(description = description, lastEditedAt = now, isSynced = false)
        ))
    }

    override suspend fun deleteSpot(spotDetails: SpotDetails) {
        spotsFlow.value = spotsFlow.value - spotDetails.spot.id
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
    }

    override suspend fun deleteImage(image: SpotImage) {
        val current: SpotDetails = spotsFlow.value[image.spotId] ?: return
        val now = epochMillis()
        val updatedImages = current.images.filter { it.id != image.id }
        val updatedSpot = current.spot.copy(lastEditedAt = now, isSynced = false)
        spotsFlow.value = spotsFlow.value + (image.spotId to current.copy(spot = updatedSpot, images = updatedImages))
    }

    override suspend fun updateImageRating(imageId: Long, rating: Int) {
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
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val now = epochMillis()
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(
            spot = current.spot.copy(artworkDate = artworkDate, lastEditedAt = now, isSynced = false)
        ))
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
    }
}

