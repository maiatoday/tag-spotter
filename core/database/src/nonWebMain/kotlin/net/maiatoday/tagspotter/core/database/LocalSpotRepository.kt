package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotOrigin
import net.maiatoday.tagspotter.core.model.generateUuid
import net.maiatoday.tagspotter.core.photo.PhotoProcessor

@OptIn(ExperimentalCoroutinesApi::class)
class LocalSpotRepository(
    private val spotDao: SpotDao,
    private val photoProcessor: PhotoProcessor
) : SpotRepository {

    private val _activeUidFlow = MutableStateFlow<String?>(null)
    private val syncWriteMutex = Mutex()

    override var activeUid: String?
        get() = _activeUidFlow.value
        set(value) {
            _activeUidFlow.value = value
        }

    override fun getAllSpots(): Flow<List<SpotDetails>> {
        return _activeUidFlow.flatMapLatest { uid ->
            spotDao.getAllSpotsDetails(uid)
        }.map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> {
        return _activeUidFlow.flatMapLatest { uid ->
            if (category == "All") {
                spotDao.getAllSpotsDetails(uid)
            } else {
                spotDao.getAllSpotsDetailsByCategory(category, uid)
            }
        }.map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSpotById(id: Long): Flow<SpotDetails?> {
        return spotDao.getSpotDetails(id).map { it?.toDomain() }
    }

    override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Long, isMain: Boolean): Long {
        val spotId = spotDao.insertSpot(
            spot.copy(
                ownerUid = activeUid,
                origin = SpotOrigin.OWN,
                isImported = false
            ).toEntity()
        )
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
                    lastEditedAt = spot.createdAt,
                    ownerUid = activeUid
                )
            )
        }
        return spotId
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Long, isMain: Boolean): Long {
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
                lastEditedAt = now,
                ownerUid = activeUid
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
                lastEditedAt = now,
                ownerUid = activeUid
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
        return _activeUidFlow.flatMapLatest { uid ->
            spotDao.getAllUsedTags(uid)
        }.map { rawTagsList ->
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
        unloadTestData()
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
        val existing = if (spotDetails.spot.uuid.isNotBlank()) {
            spotDao.getSpotByUuid(spotDetails.spot.uuid)
        } else {
            null
        }
        val spotId = if (existing != null) {
            spotDao.updateSpot(spotDetails.spot.copy(id = existing.id).toEntity())
            existing.id
        } else {
            spotDao.insertSpot(spotDetails.spot.toEntity())
        }
        spotDetails.images.forEach { image ->
            val existingImg = if (image.uuid.isNotBlank()) {
                spotDao.getImageBySpotAndUuid(spotId, image.uuid)
            } else {
                null
            }
            if (existingImg != null) {
                spotDao.updateImage(image.copy(id = existingImg.id, spotId = spotId).toEntity())
            } else {
                spotDao.insertImage(image.copy(id = 0L, spotId = spotId).toEntity())
            }
        }
        spotDetails.notes.forEach { note ->
            val existingNote = if (note.uuid.isNotBlank()) {
                spotDao.getNoteBySpotAndUuid(spotId, note.uuid)
            } else {
                null
            }
            if (existingNote != null) {
                spotDao.updateNote(note.copy(id = existingNote.id, spotId = spotId).toEntity())
            } else {
                spotDao.insertNote(note.copy(id = 0L, spotId = spotId).toEntity())
            }
        }
        return spotId
    }

    override suspend fun importSpots(spots: List<SpotDetails>): Int {
        val existingSpots = spotDao.getAllSpotsDetails(activeUid).first()
        val seenUuids = mutableSetOf<String>()
        var importedCount = 0
        spots.forEach { importedDetail ->
            val importedSpot = importedDetail.spot
            val uuid = importedSpot.uuid
            val uuidExistsInDb = uuid.isNotBlank() && (uuid in seenUuids || spotDao.getSpotByUuid(uuid) != null)
            val isDuplicate = uuidExistsInDb || existingSpots.any { existingDetail ->
                val e = existingDetail.spot
                e.createdAt == importedSpot.createdAt &&
                    e.latitude == importedSpot.latitude &&
                    e.longitude == importedSpot.longitude
            }
            if (!isDuplicate) {
                val finalUuid = uuid.ifBlank { generateUuid() }
                seenUuids.add(finalUuid)
                val newSpotId = spotDao.insertSpot(
                    importedSpot.copy(
                        id = 0L,
                        uuid = finalUuid,
                        isImported = true,
                        origin = SpotOrigin.IMPORTED,
                        isSynced = true,
                        ownerUid = activeUid
                    ).toEntity()
                )
                importedDetail.images.forEach { image ->
                    spotDao.insertImage(image.copy(id = 0L, spotId = newSpotId, ownerUid = activeUid).toEntity())
                }
                importedDetail.notes.forEach { note ->
                    spotDao.insertNote(note.copy(id = 0L, spotId = newSpotId, ownerUid = activeUid).toEntity())
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

    override suspend fun updateSpotPublic(spotId: Long, isPublic: Boolean) {
        val now = epochMillis()
        spotDao.updateSpotPublic(spotId, isPublic, now)
    }

    override suspend fun getStarredSpots(): List<Spot> {
        return spotDao.getStarredSpots(activeUid).map { it.toDomain() }
    }

    override suspend fun getStarredSpotsCount(): Int {
        return spotDao.getStarredSpotsCount(activeUid)
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

    override suspend fun updateImageRating(imageId: Long, rating: Long) {
        val now = epochMillis()
        spotDao.updateImageRating(imageId, rating, now)
        val spotId = spotDao.getSpotIdForImage(imageId)
        if (spotId != null) {
            spotDao.touchSpot(spotId, now)
        }
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        val now = epochMillis()
        spotDao.updateSpotArtworkDate(spotId, artworkDate, now)
    }

    override suspend fun deleteNote(noteId: Long) {
        val spotId = spotDao.getSpotIdForNote(noteId)
        spotDao.deleteNoteById(noteId)
        if (spotId != null) {
            val now = epochMillis()
            spotDao.touchSpot(spotId, now)
        }
    }

    override suspend fun updateNote(noteId: Long, noteText: String) {
        val now = epochMillis()
        spotDao.updateNoteText(noteId, noteText, now)
        val spotId = spotDao.getSpotIdForNote(noteId)
        if (spotId != null) {
            spotDao.touchSpot(spotId, now)
        }
    }

    override suspend fun getUnsyncedSpots(): List<SpotDetails> {
        return spotDao.getUnsyncedSpotsDetails(activeUid ?: "").map { it.toDomain() }
    }

    override suspend fun markSpotAsSynced(spotUuid: String) {
        spotDao.markSpotAsSynced(spotUuid)
    }

    override suspend fun updateImageRemoteUrl(imageId: Long, remoteUrl: String) {
        spotDao.updateImageRemoteUrl(imageId, remoteUrl)
    }

    override suspend fun saveSyncedSpot(spotDetails: SpotDetails) = syncWriteMutex.withLock {
        val existingSpot = spotDao.getSpotByUuid(spotDetails.spot.uuid)
        if (existingSpot != null && (
                existingSpot.origin == SpotOrigin.IMPORTED.name ||
                (existingSpot.origin == SpotOrigin.OWN.name && !existingSpot.isSynced && spotDetails.spot.lastEditedAt <= existingSpot.lastEditedAt)
            )
        ) {
            return@withLock
        }
        val finalSpotId = if (existingSpot != null) {
            val spotWithLocalId = spotDetails.spot.copy(
                id = existingSpot.id,
                isSynced = true,
                ownerUid = activeUid ?: spotDetails.spot.ownerUid,
                origin = SpotOrigin.OWN,
                isImported = false
            )
            spotDao.updateSpot(spotWithLocalId.toEntity())
            existingSpot.id
        } else {
            val spotToInsert = spotDetails.spot.copy(
                id = 0L,
                isSynced = true,
                ownerUid = activeUid ?: spotDetails.spot.ownerUid,
                origin = SpotOrigin.OWN,
                isImported = false
            )
            spotDao.insertSpot(spotToInsert.toEntity())
        }

        val remoteImageUuids = spotDetails.images.map { it.uuid }.toSet()
        spotDao.getImagesForSpot(finalSpotId).forEach { existingImg ->
            if (existingImg.uuid !in remoteImageUuids) {
                spotDao.deleteImageById(existingImg.id)
            }
        }
        spotDetails.images.forEach { image ->
            val existingImage = spotDao.getImageBySpotAndUuid(finalSpotId, image.uuid)
            val remoteUrl = image.remoteThumbnailUrl.ifBlank {
                if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
            }
            if (existingImage != null) {
                val localThumbExists = existingImage.thumbnailPath.isNotBlank() &&
                    !existingImage.thumbnailPath.startsWith("http") &&
                    photoProcessor.fileExists(existingImage.thumbnailPath)
                val localImageExists = existingImage.imagePath.isNotBlank() &&
                    !existingImage.imagePath.startsWith("http") &&
                    photoProcessor.fileExists(existingImage.imagePath)
                val resolvedRemoteUrl = remoteUrl.ifBlank { existingImage.remoteThumbnailUrl }
                val updated = image.copy(
                    id = existingImage.id,
                    spotId = finalSpotId,
                    ownerUid = activeUid ?: image.ownerUid,
                    thumbnailPath = if (localThumbExists) existingImage.thumbnailPath else image.thumbnailPath.ifBlank { resolvedRemoteUrl },
                    imagePath = if (localImageExists) existingImage.imagePath else image.imagePath.ifBlank { resolvedRemoteUrl },
                    remoteThumbnailUrl = resolvedRemoteUrl
                )
                spotDao.updateImage(updated.toEntity())
            } else {
                val inserted = image.copy(
                    id = 0L,
                    spotId = finalSpotId,
                    ownerUid = activeUid ?: image.ownerUid,
                    thumbnailPath = image.thumbnailPath.ifBlank { remoteUrl },
                    imagePath = image.imagePath.ifBlank { remoteUrl },
                    remoteThumbnailUrl = remoteUrl
                )
                spotDao.insertImage(inserted.toEntity())
            }
        }

        val remoteNoteUuids = spotDetails.notes.map { it.uuid }.toSet()
        spotDao.getNotesForSpot(finalSpotId).forEach { existingNote ->
            if (existingNote.uuid !in remoteNoteUuids) {
                spotDao.deleteNoteById(existingNote.id)
            }
        }
        spotDetails.notes.forEach { note ->
            val existingNote = spotDao.getNoteBySpotAndUuid(finalSpotId, note.uuid)
            if (existingNote != null) {
                spotDao.updateNote(note.copy(id = existingNote.id, spotId = finalSpotId, ownerUid = activeUid ?: note.ownerUid).toEntity())
            } else {
                spotDao.insertNote(note.copy(id = 0L, spotId = finalSpotId, ownerUid = activeUid ?: note.ownerUid).toEntity())
            }
        }
    }

    override suspend fun savePublicSpot(spotDetails: SpotDetails) = syncWriteMutex.withLock {
        val remoteOwnerUid = spotDetails.spot.ownerUid
        if (activeUid != null && remoteOwnerUid == activeUid) return@withLock

        val existingSpot = spotDao.getSpotByUuid(spotDetails.spot.uuid)
        // Never overwrite OWN or IMPORTED spots with a public cache entry (even when signed out)
        if (existingSpot != null && existingSpot.origin != SpotOrigin.PUBLIC_CACHE.name) {
            return@withLock
        }

        val finalSpotId = if (existingSpot != null) {
            val spotWithLocalId = spotDetails.spot.copy(
                id = existingSpot.id,
                ownerUid = remoteOwnerUid ?: existingSpot.ownerUid,
                isPublic = true,
                isSynced = true,
                isImported = false,
                parentPackId = null,
                isStarred = existingSpot.isStarred,
                origin = SpotOrigin.PUBLIC_CACHE
            )
            spotDao.updateSpot(spotWithLocalId.toEntity())
            existingSpot.id
        } else {
            val spotToInsert = spotDetails.spot.copy(
                id = 0L,
                ownerUid = remoteOwnerUid,
                isPublic = true,
                isSynced = true,
                isImported = false,
                parentPackId = null,
                isStarred = false,
                origin = SpotOrigin.PUBLIC_CACHE
            )
            spotDao.insertSpot(spotToInsert.toEntity())
        }

        val existingImages = spotDao.getImagesForSpot(finalSpotId)
        val remoteImageUuids = spotDetails.images.map { it.uuid }.toSet()
        existingImages.forEach { img ->
            if (img.uuid !in remoteImageUuids) {
                spotDao.deleteImageById(img.id)
            }
        }
        spotDetails.images.forEach { image ->
            val existingImage = spotDao.getImageBySpotAndUuid(finalSpotId, image.uuid)
            val remoteUrl = image.remoteThumbnailUrl.ifBlank {
                if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
            }
            val resolvedThumb = remoteUrl.ifBlank { image.thumbnailPath }
            val resolvedImg = remoteUrl.ifBlank { image.imagePath }
            if (existingImage != null) {
                spotDao.updateImage(
                    image.copy(
                        id = existingImage.id,
                        spotId = finalSpotId,
                        ownerUid = remoteOwnerUid ?: image.ownerUid,
                        thumbnailPath = resolvedThumb,
                        imagePath = resolvedImg,
                        remoteThumbnailUrl = remoteUrl
                    ).toEntity()
                )
            } else {
                spotDao.insertImage(
                    image.copy(
                        id = 0L,
                        spotId = finalSpotId,
                        ownerUid = remoteOwnerUid ?: image.ownerUid,
                        thumbnailPath = resolvedThumb,
                        imagePath = resolvedImg,
                        remoteThumbnailUrl = remoteUrl
                    ).toEntity()
                )
            }
        }

        val existingNotes = spotDao.getNotesForSpot(finalSpotId)
        val remoteNoteUuids = spotDetails.notes.map { it.uuid }.toSet()
        existingNotes.forEach { note ->
            if (note.uuid !in remoteNoteUuids) {
                spotDao.deleteNoteById(note.id)
            }
        }
        spotDetails.notes.forEach { note ->
            val existingNote = spotDao.getNoteBySpotAndUuid(finalSpotId, note.uuid)
            if (existingNote != null) {
                spotDao.updateNote(
                    note.copy(
                        id = existingNote.id,
                        spotId = finalSpotId,
                        ownerUid = remoteOwnerUid ?: note.ownerUid
                    ).toEntity()
                )
            } else {
                spotDao.insertNote(
                    note.copy(
                        id = 0L,
                        spotId = finalSpotId,
                        ownerUid = remoteOwnerUid ?: note.ownerUid
                    ).toEntity()
                )
            }
        }
    }

    override suspend fun pruneRemovedPublicSpots(activePublicUuids: Set<String>) {
        val cachedPublicSpots = spotDao.getCachedPublicSpots()
        cachedPublicSpots.forEach { spotEntity ->
            if (spotEntity.uuid !in activePublicUuids) {
                spotDao.deletePublicCacheSpotById(spotEntity.id)
            }
        }
    }

    override fun getPublicSpotsByAuthor(ownerUid: String): Flow<List<SpotDetails>> {
        return spotDao.getPublicSpotsByAuthor(ownerUid).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun adoptLocalSpots(userUid: String, backup: Boolean) {
        val targetOwner = if (backup) userUid else Spot.LOCAL_ONLY_UID
        val allSpots = spotDao.getAllSpotsDetails(null).first()
        allSpots.forEach { detail ->
            if (detail.spot.origin == SpotOrigin.OWN.name && detail.spot.ownerUid == null) {
                val updatedSpot = detail.spot.copy(ownerUid = targetOwner, isSynced = !backup)
                spotDao.updateSpot(updatedSpot)
                detail.images.forEach { img ->
                    spotDao.updateImage(img.copy(ownerUid = targetOwner))
                }
                detail.notes.forEach { note ->
                    spotDao.updateNote(note.copy(ownerUid = targetOwner))
                }
            }
        }
    }

    override suspend fun clearUserCache(userUid: String) {
        val allSpots = spotDao.getAllSpotsDetails(userUid).first()
        allSpots.forEach { detail ->
            if (detail.spot.origin == SpotOrigin.OWN.name && detail.spot.ownerUid == userUid) {
                detail.images.forEach { image ->
                    photoProcessor.deleteFile(image.thumbnailPath)
                    photoProcessor.deleteFile(image.imagePath)
                }
                spotDao.deleteSpotById(detail.spot.id)
            }
        }
    }

    override fun getAllLoadedPacks(): Flow<List<net.maiatoday.tagspotter.core.model.LoadedPack>> {
        return spotDao.getAllLoadedPacks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveLoadedPack(pack: net.maiatoday.tagspotter.core.model.LoadedPack) {
        spotDao.insertLoadedPack(pack.toEntity())
    }

    override suspend fun saveImportedSpot(spotDetails: SpotDetails) = syncWriteMutex.withLock {
        val existingSpot = spotDao.getSpotByUuid(spotDetails.spot.uuid)
        // Never overwrite an OWN spot with an imported pack copy
        if (existingSpot != null && existingSpot.origin == SpotOrigin.OWN.name) {
            return@withLock
        }
        val savedSpotId = if (existingSpot != null) {
            val updatedSpot = spotDetails.spot.copy(
                id = existingSpot.id,
                isImported = true,
                origin = SpotOrigin.IMPORTED,
                isSynced = true
            )
            spotDao.updateSpot(updatedSpot.toEntity())
            existingSpot.id
        } else {
            val newSpot = spotDetails.spot.copy(
                id = 0L,
                isImported = true,
                origin = SpotOrigin.IMPORTED,
                isSynced = true
            )
            spotDao.insertSpot(newSpot.toEntity())
        }

        spotDetails.images.forEach { image ->
            val existingImg = spotDao.getImageBySpotAndUuid(savedSpotId, image.uuid)
            if (existingImg != null) {
                spotDao.updateImage(image.copy(id = existingImg.id, spotId = savedSpotId).toEntity())
            } else {
                spotDao.insertImage(image.copy(id = 0L, spotId = savedSpotId).toEntity())
            }
        }

        spotDetails.notes.forEach { note ->
            val existingNote = spotDao.getNoteBySpotAndUuid(savedSpotId, note.uuid)
            if (existingNote != null) {
                spotDao.updateNote(note.copy(id = existingNote.id, spotId = savedSpotId).toEntity())
            } else {
                spotDao.insertNote(note.copy(id = 0L, spotId = savedSpotId).toEntity())
            }
        }
    }

    override suspend fun deleteLoadedPack(packId: String) {
        spotDao.deleteSpotsByPackId(packId)
        spotDao.deleteLoadedPack(packId)
    }

    companion object {
        val MOCK_SPOT_IDS = listOf(9001L, 9002L, 9003L)
    }
}

