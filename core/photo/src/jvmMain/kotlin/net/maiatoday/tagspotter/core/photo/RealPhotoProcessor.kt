package net.maiatoday.tagspotter.core.photo

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
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

    override fun fileExists(filePath: String): Boolean {
        if (filePath.isEmpty() || filePath.startsWith("http") || filePath.startsWith("content://")) return false
        return try {
            val cleanPath = cleanUriPath(filePath)
            FileSystem.SYSTEM.exists(cleanPath.toPath())
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = File(cleanUriPath(imagePath))
            if (!file.exists()) return@withContext null

            val sourceImage = ImageIO.read(file) ?: return@withContext null
            val origWidth = sourceImage.width
            val origHeight = sourceImage.height
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            val (targetWidth, targetHeight) = if (maxDimension > 0 && (origWidth > maxDimension || origHeight > maxDimension)) {
                val scale = maxDimension.toDouble() / maxOf(origWidth, origHeight).toDouble()
                (origWidth * scale).toInt().coerceAtLeast(1) to (origHeight * scale).toInt().coerceAtLeast(1)
            } else {
                origWidth to origHeight
            }

            val rgbImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
            val graphics = rgbImage.createGraphics()
            try {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, targetWidth, targetHeight)
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null)
            } finally {
                graphics.dispose()
            }

            val baos = ByteArrayOutputStream()
            val written = ImageIO.write(rgbImage, "jpg", baos)
            if (written) baos.toByteArray() else null
        } catch (e: Exception) {
            null
        }
    }

    internal fun cleanUriPath(uriString: String): String {
        if (uriString.startsWith("file:")) {
            val parsed = runCatching { File(URI(uriString)).path }.getOrNull()
            if (!parsed.isNullOrBlank()) return parsed
            return if (uriString.startsWith("file://")) {
                uriString.removePrefix("file://")
            } else {
                uriString.removePrefix("file:")
            }
        }
        return uriString
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
