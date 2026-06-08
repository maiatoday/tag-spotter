package net.maiatoday.tagspotter.core.database

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.photo.ImageOptimizer
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object PackManager {

    fun getImageFileName(context: Context, image: SpotImage): String {
        val path = image.imagePath
        return if (path.startsWith("content://")) {
            val uri = path.toUri()
            var name: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) name = cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {
                // Ignore query failure
            }
            name ?: ((uri.lastPathSegment ?: "image_${image.id}") + ".jpg")
        } else {
            File(path).name
        }
    }

    fun getThumbnailFileName(image: SpotImage): String {
        return File(image.thumbnailPath).name
    }

    fun exportPack(context: Context, spots: List<SpotDetails>, outputStream: OutputStream, minRating: Int = 0) {
        val filteredSpots = spots.map { spotDetails ->
            val heroImage = spotDetails.images.firstOrNull { it.isMain } ?: spotDetails.images.maxByOrNull { it.timestamp }
            spotDetails.copy(images = spotDetails.images.filter { it.rating >= minRating || it == heroImage })
        }

        ZipOutputStream(outputStream.buffered()).use { zos ->
            // 1. Write spots.json
            val jsonString = Json.encodeToString(filteredSpots)
            zos.putNextEntry(ZipEntry("spots.json"))
            zos.write(jsonString.toByteArray())
            zos.closeEntry()

            // 2. Write images and thumbnails
            filteredSpots.forEach { spotDetails ->
                spotDetails.images.forEach { image ->
                    // Write thumbnail if it's local and exists
                    if (image.thumbnailPath.isNotEmpty() &&
                        !image.thumbnailPath.startsWith("android.resource://") &&
                        !image.thumbnailPath.startsWith("http")
                    ) {
                        val thumbFile = File(image.thumbnailPath)
                        if (thumbFile.exists() && thumbFile.isFile) {
                            try {
                                zos.putNextEntry(ZipEntry("thumbnails/${getThumbnailFileName(image)}"))
                                thumbFile.inputStream().use { input ->
                                    input.copyTo(zos)
                                }
                                zos.closeEntry()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // Write original image if it's local or content://
                    if (image.imagePath.isNotEmpty() &&
                        !image.imagePath.startsWith("android.resource://") &&
                        !image.imagePath.startsWith("http")
                    ) {
                        val path = image.imagePath
                        try {
                            if (path.startsWith("content://")) {
                                val uri = path.toUri()
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    zos.putNextEntry(
                                        ZipEntry(
                                            "images/${
                                                getImageFileName(
                                                    context,
                                                    image
                                                )
                                            }"
                                        )
                                    )
                                    input.copyTo(zos)
                                    zos.closeEntry()
                                }
                            } else {
                                val file = File(path)
                                if (file.exists() && file.isFile) {
                                    zos.putNextEntry(
                                        ZipEntry(
                                            "images/${
                                                getImageFileName(
                                                    context,
                                                    image
                                                )
                                            }"
                                        )
                                    )
                                    file.inputStream().use { input ->
                                        input.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    suspend fun importPack(
        context: Context,
        repository: SpotRepository,
        inputStream: InputStream,
        currentPhotographerName: String
    ): Int {
        val tempDir = File(context.cacheDir, "import_temp_${UUID.randomUUID()}")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        try {
            // 1. Extract all ZIP entries to tempDir
            ZipInputStream(inputStream.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(tempDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output ->
                            zis.copyTo(output)
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            // 2. Read spots.json
            val jsonFile = File(tempDir, "spots.json")
            if (!jsonFile.exists()) {
                throw FileNotFoundException("Pack does not contain spots.json")
            }

            val jsonText = jsonFile.readText()
            val spots = Json.decodeFromString<List<SpotDetails>>(jsonText)

            var importedCount = 0

            // Ensure destination directories exist
            val thumbnailsDestDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val imagesDestDir = File(context.filesDir, "images").apply { mkdirs() }

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

                        // Copy original image from temp if it's local/content URI
                        if (image.imagePath.isNotEmpty() &&
                            !image.imagePath.startsWith("android.resource://") &&
                            !image.imagePath.startsWith("http")
                        ) {
                            val filename = getImageFileName(context, image)
                            val tempImageFile = File(tempDir, "images/$filename")
                            if (tempImageFile.exists() && tempImageFile.isFile) {
                                val destImageFile =
                                    File(imagesDestDir, "img_${UUID.randomUUID()}.jpg")
                                try {
                                    tempImageFile.inputStream().use { input ->
                                        destImageFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    newImagePath = destImageFile.absolutePath
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        // Copy thumbnail from temp if it exists
                        if (image.thumbnailPath.isNotEmpty() &&
                            !image.thumbnailPath.startsWith("android.resource://") &&
                            !image.thumbnailPath.startsWith("http")
                        ) {
                            val filename = getThumbnailFileName(image)
                            val tempThumbFile = File(tempDir, "thumbnails/$filename")
                            if (tempThumbFile.exists() && tempThumbFile.isFile) {
                                val destThumbFile =
                                    File(thumbnailsDestDir, "thumb_${UUID.randomUUID()}.jpg")
                                try {
                                    tempThumbFile.inputStream().use { input ->
                                        destThumbFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    newThumbnailPath = destThumbFile.absolutePath
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                // If thumbnail is missing in ZIP but original image was successfully imported,
                                // regenerate the thumbnail locally!
                                if (newImagePath.isNotEmpty() && !newImagePath.startsWith("android.resource://")) {
                                    val localImageFile = File(newImagePath)
                                    if (localImageFile.exists()) {
                                        val generatedThumbPath = ImageOptimizer.createThumbnail(context, localImageFile)
                                        if (generatedThumbPath != null) {
                                            newThumbnailPath = generatedThumbPath
                                        }
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
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}