package net.maiatoday.tagspotter.data.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import net.maiatoday.tagspotter.domain.PhotoMetadata
import net.maiatoday.tagspotter.domain.PhotoProcessor
import net.maiatoday.tagspotter.domain.TempFileDetails
import net.maiatoday.tagspotter.utils.ExifLocationExtractor
import net.maiatoday.tagspotter.utils.ImageOptimizer
import net.maiatoday.tagspotter.utils.MediaStorageHelper
import java.io.File
import java.util.UUID

class AndroidPhotoProcessor(private val context: Context) : PhotoProcessor {

    override suspend fun saveImageToPublicGallery(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null
        val publicUri = MediaStorageHelper.saveImageToPublicGallery(context, file)
        return publicUri?.toString()
    }

    override suspend fun createThumbnailFromFile(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null
        return ImageOptimizer.createThumbnail(context, file)
    }

    override suspend fun createThumbnailFromUri(uriString: String): String? {
        val uri = Uri.parse(uriString)
        return ImageOptimizer.createThumbnail(context, uri)
    }

    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? {
        val uri = Uri.parse(uriString)
        val meta = ExifLocationExtractor.getPhotoMetadata(context, uri) ?: return null
        return PhotoMetadata(
            latitude = meta.latitude,
            longitude = meta.longitude,
            timestamp = meta.timestamp
        )
    }

    override fun createTempCameraFile(): TempFileDetails {
        val file = File(context.cacheDir, "cam_${UUID.randomUUID()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return TempFileDetails(
            uriString = uri.toString(),
            fileAbsolutePath = file.absolutePath
        )
    }

    override fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
