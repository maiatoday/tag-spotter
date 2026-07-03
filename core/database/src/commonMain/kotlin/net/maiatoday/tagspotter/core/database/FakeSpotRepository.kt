package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import kotlin.collections.plus

class FakeSpotRepository : SpotRepository {
    private val spotsMap = mutableMapOf<Long, SpotDetails>()
    private val spotsFlow = MutableStateFlow<List<SpotDetails>>(emptyList())

    private fun updateFlow() {
        spotsFlow.value = spotsMap.values.toList()
    }

    fun setSpots(spots: List<SpotDetails>) {
        spotsMap.clear()
        spots.forEach { spotsMap[it.spot.id] = it }
        updateFlow()
    }

    override fun getAllSpots(): Flow<List<SpotDetails>> {
        return spotsFlow
    }

    override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> {
        if (category == "All") {
            return spotsFlow
        }
        return spotsFlow.map { list ->
            list.filter { it.spot.category == category }
        }
    }

    override fun getSpotById(id: Long): Flow<SpotDetails?> {
        return spotsFlow.map { list ->
            list.find { it.spot.id == id }
        }
    }

    override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Int, isMain: Boolean): Long {
        val id = if (spot.id == 0L) (spotsMap.keys.maxOrNull() ?: 0L) + 1L else spot.id
        val newSpot = spot.copy(id = id)
        val images = listOf(
            SpotImage(
                id = 1L,
                spotId = id,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                timestamp = spot.createdAt,
                rating = rating,
                isMain = isMain
            )
        )
        val details = SpotDetails(newSpot, images, emptyList())
        spotsMap[id] = details
        updateFlow()
        return id
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Int, isMain: Boolean): Long {
        val details = spotsMap[spotId] ?: return -1L
        val nextImageId = (details.images.maxOfOrNull { it.id } ?: 0L) + 1L
        val updatedImages = details.images + SpotImage(
            id = nextImageId,
            spotId = spotId,
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            timestamp = timestamp,
            rating = rating,
            isMain = isMain
        )
        spotsMap[spotId] = details.copy(images = updatedImages)
        updateFlow()
        return nextImageId
    }

    override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long {
        val details = spotsMap[spotId] ?: return -1L
        val nextNoteId = (details.notes.maxOfOrNull { it.id } ?: 0L) + 1L
        val updatedNotes = details.notes + SpotNote(
            id = nextNoteId,
            spotId = spotId,
            noteText = noteText,
            timestamp = timestamp
        )
        spotsMap[spotId] = details.copy(notes = updatedNotes)
        updateFlow()
        return nextNoteId
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(status = status))
        updateFlow()
    }

    override suspend fun updateSpotCategory(spotId: Long, category: String) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(category = category))
        updateFlow()
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(artists = artists))
        updateFlow()
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(photographer = photographer))
        updateFlow()
    }

    override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(tags = tags))
        updateFlow()
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(latitude = latitude, longitude = longitude))
        updateFlow()
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(description = description))
        updateFlow()
    }

    override suspend fun deleteSpot(spotDetails: SpotDetails) {
        spotsMap.remove(spotDetails.spot.id)
        updateFlow()
    }

    override fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>> {
        return spotsFlow.map { list ->
            list.flatMap { it.spot.tags }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !predefinedTags.contains(it) }
                .distinct()
        }
    }

    override suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(isStarred = isStarred))
        updateFlow()
    }

    override suspend fun getStarredSpots(): List<Spot> {
        return spotsMap.values.map { it.spot }.filter { it.isStarred }
    }

    override suspend fun getStarredSpotsCount(): Int {
        return spotsMap.values.count { it.spot.isStarred }
    }

    override suspend fun loadTestData() {
        val now = epochMillis()
        val spot1 = Spot(
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
        )
        val image1 = SpotImage(
            id = 9001L,
            spotId = 9001L,
            imagePath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            thumbnailPath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            timestamp = now - 86400000 * 2
        )
        spotsMap[9001L] = SpotDetails(spot1, listOf(image1), emptyList())

        val spot2 = Spot(
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
        )
        val image2 = SpotImage(
            id = 9002L,
            spotId = 9002L,
            imagePath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            thumbnailPath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            timestamp = now - 86400000 * 1
        )
        spotsMap[9002L] = SpotDetails(spot2, listOf(image2), emptyList())

        val spot3 = Spot(
            id = 9003L,
            latitude = 40.7128,
            longitude = -74.0060,
            createdAt = now,
            description = "Modern architectural masterpiece in New York City.",
            tags = listOf("nyc", "modern", "design"),
            category = "architecture",
            status = "active",
            artists = listOf("Star Architect"),
            photographer = "Mock Photographer"
        )
        val image3 = SpotImage(
            id = 9003L,
            spotId = 9003L,
            imagePath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            thumbnailPath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            timestamp = now
        )
        spotsMap[9003L] = SpotDetails(spot3, listOf(image3), emptyList())

        updateFlow()
    }

    override suspend fun unloadTestData() {
        spotsMap.remove(9001L)
        spotsMap.remove(9002L)
        spotsMap.remove(9003L)
        updateFlow()
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
        val nextId = (spotsMap.keys.maxOrNull() ?: 0L) + 1L
        val spotCopy = spotDetails.spot.copy(id = nextId)
        val imagesCopy = spotDetails.images.mapIndexed { idx, img -> img.copy(id = idx + 1L, spotId = nextId) }
        val notesCopy = spotDetails.notes.mapIndexed { idx, note -> note.copy(id = idx + 1L, spotId = nextId) }
        spotsMap[nextId] = SpotDetails(spotCopy, imagesCopy, notesCopy)
        updateFlow()
        return nextId
    }

    override suspend fun importSpots(spots: List<SpotDetails>): Int {
        var importedCount = 0
        spots.forEach { importedDetail ->
            val importedSpot = importedDetail.spot
            val isDuplicate = spotsMap.values.any { existingDetail ->
                val e = existingDetail.spot
                e.createdAt == importedSpot.createdAt &&
                        e.latitude == importedSpot.latitude &&
                        e.longitude == importedSpot.longitude
            }
            if (!isDuplicate) {
                val nextId = (spotsMap.keys.maxOrNull() ?: 0L) + 1L
                val spotCopy = importedSpot.copy(id = nextId, isImported = true)
                val imagesCopy = importedDetail.images.mapIndexed { idx, img -> img.copy(id = idx + 1L, spotId = nextId) }
                val notesCopy = importedDetail.notes.mapIndexed { idx, note -> note.copy(id = idx + 1L, spotId = nextId) }
                spotsMap[nextId] = SpotDetails(spotCopy, imagesCopy, notesCopy)
                importedCount++
            }
        }
        updateFlow()
        return importedCount
    }

    override suspend fun setMainImage(spotId: Long, imageId: Long) {
        val details = spotsMap[spotId] ?: return
        val updatedImages = details.images.map { img ->
            img.copy(isMain = img.id == imageId)
        }
        spotsMap[spotId] = details.copy(images = updatedImages)
        updateFlow()
    }

    override suspend fun deleteImage(image: SpotImage) {
        val details = spotsMap[image.spotId] ?: return
        var updatedImages = details.images.filter { it.id != image.id }
        if (image.isMain && updatedImages.isNotEmpty()) {
            val nextMain = updatedImages.first()
            updatedImages = updatedImages.map { img ->
                img.copy(isMain = img.id == nextMain.id)
            }
        }
        spotsMap[image.spotId] = details.copy(images = updatedImages)
        updateFlow()
    }

    override suspend fun updateImageRating(imageId: Long, rating: Int) {
        val spotEntry = spotsMap.values.find { details -> details.images.any { it.id == imageId } } ?: return
        val updatedImages = spotEntry.images.map { img ->
            if (img.id == imageId) img.copy(rating = rating) else img
        }
        spotsMap[spotEntry.spot.id] = spotEntry.copy(images = updatedImages)
        updateFlow()
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(artworkDate = artworkDate))
        updateFlow()
    }

    override suspend fun deleteNote(noteId: Long) {
        val spotEntry = spotsMap.values.find { details -> details.notes.any { it.id == noteId } } ?: return
        val updatedNotes = spotEntry.notes.filter { it.id != noteId }
        spotsMap[spotEntry.spot.id] = spotEntry.copy(notes = updatedNotes)
        updateFlow()
    }

    override suspend fun updateNote(noteId: Long, noteText: String) {
        val spotEntry = spotsMap.values.find { details -> details.notes.any { it.id == noteId } } ?: return
        val updatedNotes = spotEntry.notes.map { note ->
            if (note.id == noteId) note.copy(noteText = noteText) else note
        }
        spotsMap[spotEntry.spot.id] = spotEntry.copy(notes = updatedNotes)
        updateFlow()
    }
}
