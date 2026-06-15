package net.maiatoday.tagspotter.core.photo

data class PhotoMetadata(
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long?
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
    suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray?
    suspend fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean
}

expect fun resolveLocalPath(path: String): String

