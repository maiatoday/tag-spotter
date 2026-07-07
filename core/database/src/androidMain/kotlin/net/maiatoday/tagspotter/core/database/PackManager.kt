package net.maiatoday.tagspotter.core.database

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.photo.AndroidPhotoProcessor
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
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
            spotDetails.copy(images = spotDetails.images.filter { it.rating >= minRating.toLong() || it == heroImage })
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
        val tempFile = File(context.cacheDir, "import_temp_legacy_${generateUuid()}.ts_pack")
        try {
            tempFile.outputStream().use { fos ->
                inputStream.copyTo(fos)
            }
            val photoProcessor = AndroidPhotoProcessor(context)
            return MultiplatformPackImporter.importPack(
                repository = repository,
                packFilePath = tempFile.absolutePath,
                filesDir = context.filesDir.absolutePath,
                cacheDir = context.cacheDir.absolutePath,
                currentPhotographerName = currentPhotographerName,
                createThumbnail = { path ->
                    photoProcessor.createThumbnailFromFile(path)
                }
            )
        } finally {
            try {
                tempFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}