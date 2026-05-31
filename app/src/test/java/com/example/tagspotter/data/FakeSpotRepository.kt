package com.example.tagspotter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

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

    override suspend fun saveSpot(spot: Spot, imagePath: String): Long {
        val id = if (spot.id == 0L) (spotsMap.keys.maxOrNull() ?: 0L) + 1L else spot.id
        val newSpot = spot.copy(id = id)
        val images = listOf(SpotImage(id = 1L, spotId = id, imagePath = imagePath, timestamp = spot.createdAt))
        val details = SpotDetails(newSpot, images, emptyList())
        spotsMap[id] = details
        updateFlow()
        return id
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, timestamp: Long): Long {
        val details = spotsMap[spotId] ?: return -1L
        val nextImageId = (details.images.maxOfOrNull { it.id } ?: 0L) + 1L
        val updatedImages = details.images + SpotImage(id = nextImageId, spotId = spotId, imagePath = imagePath, timestamp = timestamp)
        spotsMap[spotId] = details.copy(images = updatedImages)
        updateFlow()
        return nextImageId
    }

    override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long {
        val details = spotsMap[spotId] ?: return -1L
        val nextNoteId = (details.notes.maxOfOrNull { it.id } ?: 0L) + 1L
        val updatedNotes = details.notes + SpotNote(id = nextNoteId, spotId = spotId, noteText = noteText, timestamp = timestamp)
        spotsMap[spotId] = details.copy(notes = updatedNotes)
        updateFlow()
        return nextNoteId
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        val details = spotsMap[spotId] ?: return
        spotsMap[spotId] = details.copy(spot = details.spot.copy(status = status))
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
}
