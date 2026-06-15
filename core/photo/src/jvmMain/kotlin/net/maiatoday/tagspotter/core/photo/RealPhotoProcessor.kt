package net.maiatoday.tagspotter.core.photo

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class RealPhotoProcessor : PhotoProcessor {
    override suspend fun saveImageToPublicGallery(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val userHome = System.getProperty("user.home")
            val picturesDir = "$userHome/Pictures/TagSpotter"
            val cleanPath = cleanUriPath(filePath)
            val sourcePath = cleanPath.toPath()
            if (FileSystem.SYSTEM.exists(sourcePath)) {
                val destDir = picturesDir.toPath()
                if (!FileSystem.SYSTEM.exists(destDir)) {
                    FileSystem.SYSTEM.createDirectories(destDir)
                }
                val fileName = sourcePath.name
                val destPath = destDir / fileName
                FileSystem.SYSTEM.copy(sourcePath, destPath)
                destPath.toString()
            } else {
                filePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            filePath
        }
    }

    override suspend fun createThumbnailFromFile(filePath: String): String? {
        // Return original path to let Coil load it directly (Coil handles scaling/caching)
        return filePath
    }

    override suspend fun createThumbnailFromUri(uriString: String): String? {
        return uriString
    }

    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? = withContext(Dispatchers.IO) {
        try {
            val cleanPath = cleanUriPath(uriString)
            val path = cleanPath.toPath()
            if (FileSystem.SYSTEM.exists(path)) {
                val bytes = FileSystem.SYSTEM.read(path) {
                    readByteArray()
                }
                ExifMetadataParser.extractMetadata(bytes)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun createTempCameraFile(): TempFileDetails {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
        val fileName = "cam_${Random.nextInt(10000000)}.jpg"
        val path = tempDir / fileName
        return TempFileDetails(
            uriString = "file://$path",
            fileAbsolutePath = path.toString()
        )
    }

    override fun deleteFile(filePath: String): Boolean {
        return try {
            val cleanPath = cleanUriPath(filePath)
            val path = cleanPath.toPath()
            if (FileSystem.SYSTEM.exists(path)) {
                FileSystem.SYSTEM.delete(path)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cleanPath = cleanUriPath(imagePath)
            val path = cleanPath.toPath()
            if (FileSystem.SYSTEM.exists(path)) {
                FileSystem.SYSTEM.read(path) {
                    readByteArray()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanUriPath(uriString: String): String {
        return if (uriString.startsWith("file://")) {
            uriString.removePrefix("file://")
        } else if (uriString.startsWith("file:")) {
            uriString.removePrefix("file:")
        } else {
            uriString
        }
    }

    override suspend fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = cleanUriPath(filePath).toPath()
            FileSystem.SYSTEM.write(path) {
                write(bytes)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
