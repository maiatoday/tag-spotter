package net.maiatoday.tagspotter.core.database

import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.BackupWrapper
import net.maiatoday.tagspotter.core.model.SpotDetails
import okio.FileSystem
import okio.Path.Companion.toPath

object MultiplatformPackExporter {

    fun getImageFileName(imagePath: String): String {
        return imagePath.substringAfterLast('/')
    }

    fun getThumbnailFileName(thumbnailPath: String): String {
        return thumbnailPath.substringAfterLast('/')
    }

    fun exportPack(
        spots: List<SpotDetails>,
        destZipFilePath: String,
        cacheDir: String,
        minRating: Int = 0
    ) {
        val tempDir = "$cacheDir/export_temp_${generateUuid()}".toPath()
        fileSystem.createDirectories(tempDir)

        try {
            // 1. Filter spots by minRating, preserving the main image
            val filteredSpots = spots.map { spotDetails ->
                val heroImage = spotDetails.images.firstOrNull { it.isMain } 
                    ?: spotDetails.images.maxByOrNull { it.timestamp }
                spotDetails.copy(images = spotDetails.images.filter { it.rating >= minRating || it == heroImage })
            }

            // 2. Write spots.json
            val jsonFile = tempDir / "spots.json"
            val backupWrapper = BackupWrapper(backupVersion = 2, spots = filteredSpots)
            val jsonString = Json.encodeToString(backupWrapper)
            fileSystem.write(jsonFile) {
                writeUtf8(jsonString)
            }

            // Create images and thumbnails directories in temp
            val tempImagesDir = tempDir / "images"
            val tempThumbnailsDir = tempDir / "thumbnails"
            fileSystem.createDirectories(tempImagesDir)
            fileSystem.createDirectories(tempThumbnailsDir)

            // 3. Copy files to temp directory
            filteredSpots.forEach { spotDetails ->
                spotDetails.images.forEach { image ->
                    // Copy thumbnail
                    if (image.thumbnailPath.isNotEmpty() && !image.thumbnailPath.startsWith("http")) {
                        val srcFile = image.thumbnailPath.toPath()
                        if (fileSystem.exists(srcFile)) {
                            val destFile = tempThumbnailsDir / getThumbnailFileName(image.thumbnailPath)
                            try {
                                fileSystem.write(destFile) {
                                    writeAll(fileSystem.source(srcFile))
                                }
                            } catch (e: Exception) {
                                println("Error exporting thumbnail: ${e.message}")
                            }
                        }
                    }

                    // Copy original image
                    if (image.imagePath.isNotEmpty() && !image.imagePath.startsWith("http")) {
                        val srcFile = image.imagePath.toPath()
                        if (fileSystem.exists(srcFile)) {
                            val destFile = tempImagesDir / getImageFileName(image.imagePath)
                            try {
                                fileSystem.write(destFile) {
                                    writeAll(fileSystem.source(srcFile))
                                }
                            } catch (e: Exception) {
                                println("Error exporting image: ${e.message}")
                            }
                        }
                    }
                }
            }

            // 4. Zip the temp directory to the destination ZIP file
            val destZipFile = destZipFilePath.toPath()
            // Ensure parent directory for destination exists
            destZipFile.parent?.let { fileSystem.createDirectories(it) }
            
            // Delete destination file if it already exists
            if (fileSystem.exists(destZipFile)) {
                fileSystem.delete(destZipFile)
            }

            zip(tempDir.toString(), destZipFilePath)

        } finally {
            // Clean up temporary files
            try {
                fileSystem.deleteRecursively(tempDir)
            } catch (e: Exception) {
                println("Error deleting export temp dir: ${e.message}")
            }
        }
    }
}
