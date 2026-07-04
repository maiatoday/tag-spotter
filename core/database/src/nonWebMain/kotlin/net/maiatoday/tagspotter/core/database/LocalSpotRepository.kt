package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import net.maiatoday.tagspotter.core.model.generateUuid

class LocalSpotRepository(
    private val spotDao: SpotDao,
    private val photoProcessor: PhotoProcessor
) : SpotRepository {

    override fun getAllSpots(): Flow<List<SpotDetails>> {
        return spotDao.getAllSpotsDetails().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> {
        if (category == "All") {
            return spotDao.getAllSpotsDetails().map { list ->
                list.map { it.toDomain() }
            }
        }
        return spotDao.getAllSpotsDetailsByCategory(category).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSpotById(id: Long): Flow<SpotDetails?> {
        return spotDao.getSpotDetails(id).map { it?.toDomain() }
    }

    override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Int, isMain: Boolean): Long {
        val spotId = spotDao.insertSpot(spot.toEntity())
        if (imagePath.isNotEmpty()) {
            spotDao.insertImage(
                SpotImageEntity(
                    spotId = spotId,
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    timestamp = spot.createdAt,
                    rating = rating,
                    isMain = isMain,
                    uuid = generateUuid(),
                    lastEditedAt = spot.createdAt
                )
            )
        }
        return spotId
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Int, isMain: Boolean): Long {
        val now = epochMillis()
        val imgId = spotDao.insertImage(
            SpotImageEntity(
                spotId = spotId,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                timestamp = timestamp,
                rating = rating,
                isMain = isMain,
                uuid = generateUuid(),
                lastEditedAt = now
            )
        )
        spotDao.touchSpot(spotId, now)
        return imgId
    }

    override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long {
        val now = epochMillis()
        val noteId = spotDao.insertNote(
            SpotNoteEntity(
                spotId = spotId,
                noteText = noteText,
                timestamp = timestamp,
                uuid = generateUuid(),
                lastEditedAt = now
            )
        )
        spotDao.touchSpot(spotId, now)
        return noteId
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        val now = epochMillis()
        spotDao.updateSpotStatus(spotId, status, now)
    }

    override suspend fun updateSpotCategory(spotId: Long, category: String) {
        val now = epochMillis()
        spotDao.updateSpotCategory(spotId, category, now)
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        val now = epochMillis()
        spotDao.updateSpotArtists(spotId, artists, now)
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        val now = epochMillis()
        spotDao.updateSpotPhotographer(spotId, photographer, now)
    }

    override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {
        val now = epochMillis()
        spotDao.updateSpotTags(spotId, tags, now)
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        val now = epochMillis()
        spotDao.updateSpotLocation(spotId, latitude, longitude, now)
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        val now = epochMillis()
        spotDao.updateSpotDescription(spotId, description, now)
    }

    override suspend fun deleteSpot(spotDetails: SpotDetails) {
        // Delete all local thumbnail and image files (original public gallery photos are NOT deleted)
        spotDetails.images.forEach { image ->
            photoProcessor.deleteFile(image.thumbnailPath)
            photoProcessor.deleteFile(image.imagePath)
        }
        // Room cascading delete will clean the images and notes in the database
        spotDao.deleteSpotById(spotDetails.spot.id)
    }

    override fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>> {
        return spotDao.getAllUsedTags().map { rawTagsList ->
            rawTagsList.asSequence().flatMap { rawTags ->
                Converters().toStringList(rawTags)
            }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !predefinedTags.contains(it) }
                .distinct()
                .take(15).toList() // Limit to top 15 suggestions
        }
    }

    override suspend fun loadTestData() {
        val now = epochMillis()
        val spot1 = Spot(
            id = 9001L,
            latitude = 45.4642,
            longitude = 9.1899,
            createdAt = now - 86400000 * 2, // 2 days ago
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
            imagePath = "https://picsum.photos/id/402/800/600",
            thumbnailPath = "https://picsum.photos/id/402/800/600",
            timestamp = now - 86400000 * 2
        )

        val spot2 = Spot(
            id = 9002L,
            latitude = 51.5074,
            longitude = -0.1278,
            createdAt = now - 86400000 * 1, // 1 day ago
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
            imagePath = "https://picsum.photos/id/445/800/600",
            thumbnailPath = "https://picsum.photos/id/445/800/600",
            timestamp = now - 86400000 * 1
        )

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
            imagePath = "https://picsum.photos/id/507/800/600",
            thumbnailPath = "https://picsum.photos/id/507/800/600",
            timestamp = now
        )

        spotDao.insertSpot(spot1.toEntity())
        spotDao.insertImage(image1.toEntity())
        spotDao.insertSpot(spot2.toEntity())
        spotDao.insertImage(image2.toEntity())
        spotDao.insertSpot(spot3.toEntity())
        spotDao.insertImage(image3.toEntity())
    }

    override suspend fun unloadTestData() {
        MOCK_SPOT_IDS.forEach { id ->
            spotDao.deleteSpotById(id)
        }
    }

    override suspend fun importPack(
        packFilePath: String,
        filesDir: String,
        cacheDir: String,
        currentPhotographerName: String,
        createThumbnail: suspend (String) -> String?
    ): Int {
        return MultiplatformPackImporter.importPack(
            repository = this,
            packFilePath = packFilePath,
            filesDir = filesDir,
            cacheDir = cacheDir,
            currentPhotographerName = currentPhotographerName,
            createThumbnail = createThumbnail
        )
    }

    override suspend fun saveSpotDetails(spotDetails: SpotDetails): Long {
        val spotId = spotDao.insertSpot(spotDetails.spot.toEntity())
        spotDetails.images.forEach { image ->
            spotDao.insertImage(image.copy(id = 0L, spotId = spotId).toEntity())
        }
        spotDetails.notes.forEach { note ->
            spotDao.insertNote(note.copy(id = 0L, spotId = spotId).toEntity())
        }
        return spotId
    }

    override suspend fun importSpots(spots: List<SpotDetails>): Int {
        val existingSpots = spotDao.getAllSpotsDetails().first()
        var importedCount = 0
        spots.forEach { importedDetail ->
            val importedSpot = importedDetail.spot
            val isDuplicate = existingSpots.any { existingDetail ->
                val e = existingDetail.spot
                e.createdAt == importedSpot.createdAt &&
                        e.latitude == importedSpot.latitude &&
                        e.longitude == importedSpot.longitude
            }
            if (!isDuplicate) {
                val newSpotId = spotDao.insertSpot(importedSpot.copy(id = 0L, isImported = true).toEntity())
                importedDetail.images.forEach { image ->
                    spotDao.insertImage(image.copy(id = 0L, spotId = newSpotId).toEntity())
                }
                importedDetail.notes.forEach { note ->
                    spotDao.insertNote(note.copy(id = 0L, spotId = newSpotId).toEntity())
                }
                importedCount++
            }
        }
        return importedCount
    }

    override suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean) {
        val now = epochMillis()
        spotDao.updateSpotStarred(spotId, isStarred, now)
    }

    override suspend fun getStarredSpots(): List<Spot> {
        return spotDao.getStarredSpots().map { it.toDomain() }
    }

    override suspend fun getStarredSpotsCount(): Int {
        return spotDao.getStarredSpotsCount()
    }

    override suspend fun setMainImage(spotId: Long, imageId: Long) {
        val now = epochMillis()
        spotDao.setMainImage(spotId, imageId)
        spotDao.touchSpot(spotId, now)
    }

    override suspend fun deleteImage(image: SpotImage) {
        photoProcessor.deleteFile(image.thumbnailPath)
        photoProcessor.deleteFile(image.imagePath)

        spotDao.deleteImageById(image.id)

        val now = epochMillis()
        spotDao.touchSpot(image.spotId, now)

        if (image.isMain) {
            val remainingImages = spotDao.getImagesForSpot(image.spotId)
            val nextMain = remainingImages.firstOrNull()
            if (nextMain != null) {
                spotDao.setMainImage(image.spotId, nextMain.id)
            }
        }
    }

    override suspend fun updateImageRating(imageId: Long, rating: Int) {
        val now = epochMillis()
        spotDao.updateImageRating(imageId, rating, now)
        // Retrieve spotId for image to touch the parent spot
        val images = spotDao.getAllSpotsDetails().first().flatMap { it.images }
        val matchingImage = images.find { it.id == imageId }
        if (matchingImage != null) {
            spotDao.touchSpot(matchingImage.spotId, now)
        }
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        val now = epochMillis()
        spotDao.updateSpotArtworkDate(spotId, artworkDate, now)
    }

    override suspend fun deleteNote(noteId: Long) {
        val notes = spotDao.getAllSpotsDetails().first().flatMap { it.notes }
        val matchingNote = notes.find { it.id == noteId }
        spotDao.deleteNoteById(noteId)
        if (matchingNote != null) {
            val now = epochMillis()
            spotDao.touchSpot(matchingNote.spotId, now)
        }
    }

    override suspend fun updateNote(noteId: Long, noteText: String) {
        val now = epochMillis()
        spotDao.updateNoteText(noteId, noteText, now)
        val notes = spotDao.getAllSpotsDetails().first().flatMap { it.notes }
        val matchingNote = notes.find { it.id == noteId }
        if (matchingNote != null) {
            spotDao.touchSpot(matchingNote.spotId, now)
        }
    }

    override suspend fun getUnsyncedSpots(): List<SpotDetails> {
        return spotDao.getUnsyncedSpotsDetails().map { it.toDomain() }
    }

    override suspend fun markSpotAsSynced(spotUuid: String) {
        spotDao.markSpotAsSynced(spotUuid)
    }

    override suspend fun saveSyncedSpot(spotDetails: SpotDetails) {
        val existingSpot = spotDao.getSpotByUuid(spotDetails.spot.uuid)
        val finalSpotId = if (existingSpot != null) {
            val spotWithLocalId = spotDetails.spot.copy(id = existingSpot.id, isSynced = true)
            spotDao.insertSpot(spotWithLocalId.toEntity())
            existingSpot.id
        } else {
            val spotToInsert = spotDetails.spot.copy(id = 0L, isSynced = true)
            spotDao.insertSpot(spotToInsert.toEntity())
        }

        spotDetails.images.forEach { image ->
            val existingImage = spotDao.getImageByUuid(image.uuid)
            val imageToInsert = if (existingImage != null) {
                image.copy(id = existingImage.id, spotId = finalSpotId)
            } else {
                image.copy(id = 0L, spotId = finalSpotId)
            }
            spotDao.insertImage(imageToInsert.toEntity())
        }

        spotDetails.notes.forEach { note ->
            val existingNote = spotDao.getNoteByUuid(note.uuid)
            val noteToInsert = if (existingNote != null) {
                note.copy(id = existingNote.id, spotId = finalSpotId)
            } else {
                note.copy(id = 0L, spotId = finalSpotId)
            }
            spotDao.insertNote(noteToInsert.toEntity())
        }
    }

    companion object {
        val MOCK_SPOT_IDS = listOf(9001L, 9002L, 9003L)
    }
}

