package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import net.maiatoday.tagspotter.core.photo.PhotoProcessor

object PackManager {

    fun getImageFileName(imagePath: String): String {
        return imagePath.substringAfterLast('/')
    }

    fun getThumbnailFileName(thumbnailPath: String): String {
        return thumbnailPath.substringAfterLast('/')
    }

    fun exportPack(spots: List<SpotDetails>, outputStream: OutputStream, minRating: Int = 0) {
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
                    // Write thumbnail if it exists locally
                    if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                        val thumbFile = File(image.thumbnailPath)
                        if (thumbFile.exists() && thumbFile.isFile) {
                            try {
                                zos.putNextEntry(ZipEntry("thumbnails/${getThumbnailFileName(image.thumbnailPath)}"))
                                thumbFile.inputStream().use { input ->
                                    input.copyTo(zos)
                                }
                                zos.closeEntry()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // Write original image if it exists locally
                    if (image.imagePath.isNotEmpty() && !image.imagePath.startsWith("http")) {
                        val file = File(image.imagePath)
                        if (file.exists() && file.isFile) {
                            try {
                                zos.putNextEntry(ZipEntry("images/${getImageFileName(image.imagePath)}"))
                                file.inputStream().use { input ->
                                    input.copyTo(zos)
                                }
                                zos.closeEntry()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun importPack(
        repository: SpotRepository,
        photoProcessor: PhotoProcessor,
        packFilePath: String,
        filesDir: String,
        cacheDir: String,
        currentPhotographerName: String
    ): Int {
        return MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFilePath,
            filesDir = filesDir,
            cacheDir = cacheDir,
            currentPhotographerName = currentPhotographerName,
            createThumbnail = { path ->
                photoProcessor.createThumbnailFromFile(path)
            }
        )
    }
}
