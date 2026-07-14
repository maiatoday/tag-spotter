package net.maiatoday.tagspotter.core.photo



class FakePhotoProcessor : PhotoProcessor {
    var saveToPublicResult: String? = "public_uri"
    var createThumbFileResult: String? = "thumb_path"
    var createThumbUriResult: String? = "thumb_path"
    var metadataResult: PhotoMetadata? = PhotoMetadata(12.34, 56.78, 123456789L)
    var deleteFileResult: Boolean = true
    var tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
    var decodeScaledBitmapResult: ByteArray? = null

    var deleteFileCalledWith: String? = null
    var saveImageCalledWith: String? = null
    var createThumbnailFromFileCalledWith: String? = null
    var createThumbnailFromUriCalledWith: String? = null
    var extractMetadataFromUriCalledWith: String? = null

    var tempCameraFileException: Exception? = null
    var saveImageException: Exception? = null
    var extractMetadataException: Exception? = null
    var writeBytesToFileResult: Boolean = true
    var writeBytesCalled: Boolean = false
    var writeBytesPath: String? = null

    override suspend fun saveImageToPublicGallery(filePath: String): String? {
        saveImageCalledWith = filePath
        saveImageException?.let { throw it }
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
        extractMetadataException?.let { throw it }
        return metadataResult
    }

    override fun createTempCameraFile(): TempFileDetails {
        tempCameraFileException?.let { throw it }
        return tempCameraFileResult
    }

    override fun deleteFile(filePath: String): Boolean {
        deleteFileCalledWith = filePath
        return deleteFileResult
    }

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray? {
        return decodeScaledBitmapResult
    }

    override suspend fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean {
        writeBytesCalled = true
        writeBytesPath = filePath
        return writeBytesToFileResult
    }
}
