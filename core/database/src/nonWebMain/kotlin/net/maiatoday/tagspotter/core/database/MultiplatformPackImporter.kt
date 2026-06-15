package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import okio.FileSystem
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
            val spots = Json.decodeFromString<List<SpotDetails>>(jsonText)

            var importedCount = 0

            // Ensure destination directories exist
            val thumbnailsDestDir = "$filesDir/thumbnails".toPath()
            val imagesDestDir = "$filesDir/images".toPath()
            fileSystem.createDirectories(thumbnailsDestDir)
            fileSystem.createDirectories(imagesDestDir)

            val existingSpots = repository.getAllSpots().first()

            // 3. Process each spot
            spots.forEach { importedDetail ->
                val importedSpot = importedDetail.spot
                val isDuplicate = existingSpots.any { existingDetail ->
                    val e = existingDetail.spot
                    e.createdAt == importedSpot.createdAt &&
                            e.latitude == importedSpot.latitude &&
                            e.longitude == importedSpot.longitude
                }

                if (!isDuplicate) {
                    val isOwnSpot = currentPhotographerName.isNotEmpty() &&
                            importedSpot.photographer.trim().equals(currentPhotographerName.trim(), ignoreCase = true)
                    val markImported = !isOwnSpot

                    var isFirstImage = true
                    var firstImageNewPath = ""
                    var firstImageNewThumbnailPath = ""
                    var firstImageRating = 0
                    var firstImageIsMain = false
                    val extraImages = mutableListOf<SpotImage>()

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
                                // If thumbnail is missing in ZIP, use createThumbnail to generate/fallback
                                if (newImagePath.isNotEmpty() && !newImagePath.startsWith("android.resource://")) {
                                    val generatedThumbPath = createThumbnail(newImagePath)
                                    if (generatedThumbPath != null) {
                                        newThumbnailPath = generatedThumbPath
                                    }
                                }
                            }
                        }

                        if (isFirstImage) {
                            firstImageNewPath = newImagePath
                            firstImageNewThumbnailPath = newThumbnailPath
                            firstImageRating = image.rating
                            firstImageIsMain = image.isMain
                            isFirstImage = false
                        } else {
                            extraImages.add(
                                SpotImage(
                                    spotId = 0L,
                                    imagePath = newImagePath,
                                    thumbnailPath = newThumbnailPath,
                                    timestamp = image.timestamp,
                                    rating = image.rating,
                                    isMain = image.isMain
                                )
                            )
                        }
                    }

                    // Insert Spot
                    val newSpotId = repository.saveSpot(
                        spot = importedSpot.copy(id = 0L, isImported = markImported),
                        imagePath = firstImageNewPath,
                        thumbnailPath = firstImageNewThumbnailPath,
                        rating = firstImageRating,
                        isMain = firstImageIsMain
                    )

                    // Insert extra images (if any)
                    extraImages.forEach { extraImage ->
                        repository.addImageToSpot(
                            spotId = newSpotId,
                            imagePath = extraImage.imagePath,
                            thumbnailPath = extraImage.thumbnailPath,
                            timestamp = extraImage.timestamp,
                            rating = extraImage.rating,
                            isMain = extraImage.isMain
                        )
                    }

                    // Insert notes
                    importedDetail.notes.forEach { note ->
                        repository.addNoteToSpot(
                            spotId = newSpotId,
                            noteText = note.noteText,
                            timestamp = note.timestamp
                        )
                    }

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
}
