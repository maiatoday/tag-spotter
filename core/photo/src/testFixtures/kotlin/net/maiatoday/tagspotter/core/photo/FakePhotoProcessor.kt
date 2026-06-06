package net.maiatoday.tagspotter.core.photo

import android.graphics.Bitmap

class FakePhotoProcessor : PhotoProcessor {
    var saveToPublicResult: String? = "public_uri"
    var createThumbFileResult: String? = "thumb_path"
    var createThumbUriResult: String? = "thumb_path"
    var metadataResult: PhotoMetadata? = PhotoMetadata(12.34, 56.78, 123456789L)
    var deleteFileResult: Boolean = true
    var tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
    var decodeScaledBitmapResult: Bitmap? = null

    var deleteFileCalledWith: String? = null
    var saveImageCalledWith: String? = null
    var createThumbnailFromFileCalledWith: String? = null
    var createThumbnailFromUriCalledWith: String? = null
    var extractMetadataFromUriCalledWith: String? = null

    override suspend fun saveImageToPublicGallery(filePath: String): String? {
        saveImageCalledWith = filePath
        return saveToPublicResult
    }

    override suspend fun createThumbnailFromFile(filePath: String): String? {
        createThumbnailFromFileCalledWith = filePath
        return createThumbFileResult
    }

    override suspend fun createThumbnailFromUri(uriString: String): String? {
        createThumbnailFromUriCalledWith = uriString
        return createThumbUriResult
    }

    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? {
        extractMetadataFromUriCalledWith = uriString
        return metadataResult
    }

    override fun createTempCameraFile(): TempFileDetails {
        return tempCameraFileResult
    }

    override fun deleteFile(filePath: String): Boolean {
        deleteFileCalledWith = filePath
        return deleteFileResult
    }

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): Bitmap? {
        return decodeScaledBitmapResult
    }
}
