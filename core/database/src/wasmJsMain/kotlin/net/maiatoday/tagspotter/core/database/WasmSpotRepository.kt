package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote

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
        val spotId = if (spot.id == 0L) nextSpotId++ else spot.id
        val newSpot = spot.copy(id = spotId)
        val images = if (imagePath.isNotEmpty()) {
            listOf(SpotImage(id = nextImageId++, spotId = spotId, imagePath = imagePath, thumbnailPath = thumbnailPath, timestamp = spot.createdAt, rating = rating, isMain = isMain))
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
        val newImage = SpotImage(id = imgId, spotId = spotId, imagePath = imagePath, thumbnailPath = thumbnailPath, timestamp = timestamp, rating = rating, isMain = isMain)
        val updatedImages = current.images + newImage
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(images = updatedImages))
        return imgId
    }

    override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return 0L
        val noteId = nextNoteId++
        val newNote = SpotNote(id = noteId, spotId = spotId, noteText = noteText, timestamp = timestamp)
        val updatedNotes = current.notes + newNote
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(notes = updatedNotes))
        return noteId
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(status = status)))
    }

    override suspend fun updateSpotCategory(spotId: Long, category: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(category = category)))
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(artists = artists)))
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(photographer = photographer)))
    }

    override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(tags = tags)))
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(latitude = latitude, longitude = longitude)))
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(description = description)))
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
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(isStarred = isStarred)))
    }

    override suspend fun getStarredSpots(): List<Spot> {
        return spotsFlow.value.values.map { it.spot }.filter { it.isStarred }
    }

    override suspend fun getStarredSpotsCount(): Int {
        return getStarredSpots().size
    }

    override suspend fun setMainImage(spotId: Long, imageId: Long) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        val updatedImages = current.images.map { it.copy(isMain = it.id == imageId) }
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(images = updatedImages))
    }

    override suspend fun deleteImage(image: SpotImage) {
        val current: SpotDetails = spotsFlow.value[image.spotId] ?: return
        val updatedImages = current.images.filter { it.id != image.id }
        spotsFlow.value = spotsFlow.value + (image.spotId to current.copy(images = updatedImages))
    }

    override suspend fun updateImageRating(imageId: Long, rating: Int) {
        spotsFlow.value = spotsFlow.value.mapValues<Long, SpotDetails, SpotDetails> { entry ->
            val details = entry.value
            details.copy(images = details.images.map { if (it.id == imageId) it.copy(rating = rating) else it })
        }
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        val current: SpotDetails = spotsFlow.value[spotId] ?: return
        spotsFlow.value = spotsFlow.value + (spotId to current.copy(spot = current.spot.copy(artworkDate = artworkDate)))
    }

    override suspend fun deleteNote(noteId: Long) {
        spotsFlow.value = spotsFlow.value.mapValues<Long, SpotDetails, SpotDetails> { entry ->
            val details = entry.value
            details.copy(notes = details.notes.filter { it.id != noteId })
        }
    }

    override suspend fun updateNote(noteId: Long, noteText: String) {
        spotsFlow.value = spotsFlow.value.mapValues<Long, SpotDetails, SpotDetails> { entry ->
            val details = entry.value
            details.copy(notes = details.notes.map { if (it.id == noteId) it.copy(noteText = noteText) else it })
        }
    }
}
