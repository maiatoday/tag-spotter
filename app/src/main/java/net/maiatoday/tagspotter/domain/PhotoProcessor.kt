package net.maiatoday.tagspotter.domain

import android.graphics.Bitmap

data class PhotoMetadata(
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long?
)

data class ProcessedPhoto(
    val publicUriString: String,
    val thumbnailPath: String,
    val latitude: Double,
    val longitude: Double,
    val isFallback: Boolean,
    val captureTime: Long?
)

data class TempFileDetails(
    val uriString: String,
    val fileAbsolutePath: String
)

interface PhotoProcessor {
    suspend fun saveImageToPublicGallery(filePath: String): String?
    suspend fun createThumbnailFromFile(filePath: String): String?
    suspend fun createThumbnailFromUri(uriString: String): String?
    suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata?
    fun createTempCameraFile(): TempFileDetails
    fun deleteFile(filePath: String): Boolean
    suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): Bitmap?
}
