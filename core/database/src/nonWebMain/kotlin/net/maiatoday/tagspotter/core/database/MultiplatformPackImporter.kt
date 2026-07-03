package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.BackupWrapper
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import okio.Path
import okio.Path.Companion.toPath

object MultiplatformPackImporter {

    fun getImageFileName(imagePath: String): String {
        return imagePath.substringAfterLast('/')
    }

    fun getThumbnailFileName(thumbnailPath: String): String {
        return thumbnailPath.substringAfterLast('/')
    }

    suspend fun importPack(
        repository: SpotRepository,
        packFilePath: String,
        filesDir: String,
        cacheDir: String,
        currentPhotographerName: String,
        createThumbnail: suspend (String) -> String?
    ): Int {
        val zipPath = packFilePath.toPath()
        val tempDir = "$cacheDir/import_temp_${generateUuid()}".toPath()

        // Ensure tempDir does not exist, then create it
        fileSystem.createDirectories(tempDir)

        try {
            // 1. Extract all ZIP entries using our platform-agnostic unzip helper
            unzip(packFilePath, tempDir.toString())

            // 2. Read spots.json
            val jsonFile = tempDir / "spots.json"
            if (!fileSystem.exists(jsonFile)) {
                throw Exception("Pack does not contain spots.json")
            }

            val jsonText = fileSystem.read(jsonFile) { readUtf8() }
            val backupWrapper = try {
                Json.decodeFromString<BackupWrapper>(jsonText)
            } catch (e: Exception) {
                // Fallback to legacy V1 (direct List<SpotDetails>)
                try {
                    val legacySpots = Json.decodeFromString<List<SpotDetails>>(jsonText)
                    BackupWrapper(backupVersion = 1, spots = legacySpots)
                } catch (ex: Exception) {
                    throw Exception("Failed to parse spots.json: ${ex.message}")
                }
            }

            var importedCount = 0

            // Ensure destination directories exist
            val thumbnailsDestDir = "$filesDir/thumbnails".toPath()
            val imagesDestDir = "$filesDir/images".toPath()
            fileSystem.createDirectories(thumbnailsDestDir)
            fileSystem.createDirectories(imagesDestDir)

            val existingSpots = repository.getAllSpots().first()

            // 3. Process each spot
            backupWrapper.spots.forEach { importedDetail ->
                val importedSpot = importedDetail.spot

                // Find a duplicate matching local spot
                val matchingLocalDetail = if (backupWrapper.backupVersion >= 2) {
                    // V2: Match strictly by UUID
                    existingSpots.find { it.spot.uuid == importedSpot.uuid }
                } else {
                    // V1 Legacy: Match by exact session timestamp (createdAt)
                    existingSpots.find { it.spot.createdAt == importedSpot.createdAt }
                }

                if (matchingLocalDetail != null) {
                    // We found a duplicate!
                    if (backupWrapper.backupVersion >= 2) {
                        // V2 LWW conflict resolution:
                        if (importedSpot.lastEditedAt > matchingLocalDetail.spot.lastEditedAt) {
                            // The imported version is newer. Overwrite local!
                            // First, delete old images and thumbnails of the matching local spot from disk
                            matchingLocalDetail.images.forEach { image ->
                                if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                                    try { fileSystem.delete(image.thumbnailPath.toPath()) } catch (_: Exception) {}
                                }
                                if (image.imagePath.isNotEmpty() && !image.imagePath.startsWith("http")) {
                                    try { fileSystem.delete(image.imagePath.toPath()) } catch (_: Exception) {}
                                }
                            }
                            // Delete local spot from database (this will cascade delete note and image rows too)
                            repository.deleteSpot(matchingLocalDetail)

                            // Now copy new assets and save the imported details
                            importAndSaveSpot(
                                repository = repository,
                                importedDetail = importedDetail,
                                tempDir = tempDir,
                                imagesDestDir = imagesDestDir,
                                thumbnailsDestDir = thumbnailsDestDir,
                                currentPhotographerName = currentPhotographerName,
                                createThumbnail = createThumbnail,
                                backupVersion = backupWrapper.backupVersion
                            )
                            importedCount++
                        }
                    } else {
                        // V1 legacy duplicate: Skip (no-op)
                    }
                } else {
                    // No duplicate found. Process and save!
                    importAndSaveSpot(
                        repository = repository,
                        importedDetail = importedDetail,
                        tempDir = tempDir,
                        imagesDestDir = imagesDestDir,
                        thumbnailsDestDir = thumbnailsDestDir,
                        currentPhotographerName = currentPhotographerName,
                        createThumbnail = createThumbnail,
                        backupVersion = backupWrapper.backupVersion
                    )
                    importedCount++
                }
            }

            return importedCount

        } finally {
            // Clean up temporary files
            try {
                fileSystem.deleteRecursively(tempDir)
            } catch (e: Exception) {
                println("Error deleting temp dir: ${e.message}")
            }
        }
    }

    private suspend fun importAndSaveSpot(
        repository: SpotRepository,
        importedDetail: SpotDetails,
        tempDir: Path,
        imagesDestDir: Path,
        thumbnailsDestDir: Path,
        currentPhotographerName: String,
        createThumbnail: suspend (String) -> String?,
        backupVersion: Int
    ) {
        val importedSpot = importedDetail.spot

        // Determine whether this spot belongs to another photographer
        val isOwnSpot = currentPhotographerName.isNotEmpty() &&
                importedSpot.photographer.trim().equals(currentPhotographerName.trim(), ignoreCase = true)
        val markImported = !isOwnSpot

        // UUID and lastEditedAt determination (hydrate if V1)
        val finalSpotUuid = if (backupVersion >= 2 && importedSpot.uuid.isNotEmpty()) {
            importedSpot.uuid
        } else {
            generateUuid()
        }
        val finalSpotLastEdited = if (backupVersion >= 2) {
            importedSpot.lastEditedAt
        } else {
            importedSpot.createdAt
        }

        val processedImages = mutableListOf<SpotImage>()

        importedDetail.images.forEach { image ->
            var newImagePath = image.imagePath
            var newThumbnailPath = image.thumbnailPath

            // Copy original image from temp if it's local
            if (image.imagePath.isNotEmpty() &&
                !image.imagePath.startsWith("android.resource://") &&
                !image.imagePath.startsWith("http")
            ) {
                val filename = getImageFileName(image.imagePath)
                val tempImageFile = tempDir / "images" / filename
                if (fileSystem.exists(tempImageFile)) {
                    val destImageFile = imagesDestDir / "img_${generateUuid()}.jpg"
                    try {
                        fileSystem.write(destImageFile) {
                            writeAll(fileSystem.source(tempImageFile))
                        }
                        newImagePath = destImageFile.toString()
                    } catch (e: Exception) {
                        println("Error copying image: ${e.message}")
                    }
                }
            }

            // Copy thumbnail from temp if it exists
            if (image.thumbnailPath.isNotEmpty() &&
                !image.thumbnailPath.startsWith("android.resource://") &&
                !image.thumbnailPath.startsWith("http")
            ) {
                val filename = getThumbnailFileName(image.thumbnailPath)
                val tempThumbFile = tempDir / "thumbnails" / filename
                if (fileSystem.exists(tempThumbFile)) {
                    val destThumbFile = thumbnailsDestDir / "thumb_${generateUuid()}.jpg"
                    try {
                        fileSystem.write(destThumbFile) {
                            writeAll(fileSystem.source(tempThumbFile))
                        }
                        newThumbnailPath = destThumbFile.toString()
                    } catch (e: Exception) {
                        println("Error copying thumbnail: ${e.message}")
                    }
                } else {
                    // Fallback to thumbnail generation
                    if (newImagePath.isNotEmpty() && !newImagePath.startsWith("android.resource://")) {
                        val generatedThumbPath = createThumbnail(newImagePath)
                        if (generatedThumbPath != null) {
                            newThumbnailPath = generatedThumbPath
                        }
                    }
                }
            }

            val finalImageUuid = if (backupVersion >= 2 && image.uuid.isNotEmpty()) {
                image.uuid
            } else {
                generateUuid()
            }
            val finalImageLastEdited = if (backupVersion >= 2) {
                image.lastEditedAt
            } else {
                image.timestamp
            }

            processedImages.add(
                image.copy(
                    id = 0L,
                    spotId = 0L,
                    imagePath = newImagePath,
                    thumbnailPath = newThumbnailPath,
                    uuid = finalImageUuid,
                    lastEditedAt = finalImageLastEdited
                )
            )
        }

        val processedNotes = importedDetail.notes.map { note ->
            val finalNoteUuid = if (backupVersion >= 2 && note.uuid.isNotEmpty()) {
                note.uuid
            } else {
                generateUuid()
            }
            val finalNoteLastEdited = if (backupVersion >= 2) {
                note.lastEditedAt
            } else {
                note.timestamp
            }
            note.copy(
                id = 0L,
                spotId = 0L,
                uuid = finalNoteUuid,
                lastEditedAt = finalNoteLastEdited
            )
        }

        val finalSpot = importedSpot.copy(
            id = 0L,
            isImported = markImported,
            uuid = finalSpotUuid,
            lastEditedAt = finalSpotLastEdited,
            isSynced = false // triggers Cloud Sync push
        )

        val finalDetail = SpotDetails(
            spot = finalSpot,
            images = processedImages,
            notes = processedNotes
        )

        repository.saveSpotDetails(finalDetail)
    }
}
