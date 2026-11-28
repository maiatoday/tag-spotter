@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package net.maiatoday.tagspotter.core.photo

import kotlinx.cinterop.useContents
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

class RealPhotoProcessor : PhotoProcessor {
    override suspend fun saveImageToPublicGallery(filePath: String): String? = withContext(Dispatchers.Default) {
        try {
            val cleanPath = resolveRebasedPath(filePath)
            val sourcePath = cleanPath.toPath()
            if (FileSystem.SYSTEM.exists(sourcePath)) {
                // 1. Save to iOS Camera Roll (native gallery)
                val image = UIImage.imageWithContentsOfFile(cleanPath)
                if (image != null) {
                    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
                }

                // 2. Save to app's persistent Documents directory so it can be loaded by Coil/app
                val docPath = documentsDirectoryPath()
                if (docPath != null) {
                    val destDir = "$docPath/images".toPath()
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
            } else {
                filePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            filePath
        }
    }

    override suspend fun createThumbnailFromFile(filePath: String): String? = withContext(Dispatchers.Default) {
        createJpegThumbnail(filePath, maxDimension = 480) ?: filePath
    }

    override suspend fun createThumbnailFromUri(uriString: String): String? = withContext(Dispatchers.Default) {
        createJpegThumbnail(uriString, maxDimension = 480) ?: uriString
    }

    private fun createJpegThumbnail(sourcePathOrUri: String, maxDimension: Int): String? {
        if (sourcePathOrUri.isBlank() || sourcePathOrUri.startsWith("http")) return null
        return try {
            val cleanPath = resolveRebasedPath(sourcePathOrUri)
            val sourceImage = UIImage.imageWithContentsOfFile(cleanPath) ?: return null
            val (origWidth, origHeight) = sourceImage.size.useContents { width to height }
            if (origWidth <= 0.0 || origHeight <= 0.0) return null

            val maxOrig = maxOf(origWidth, origHeight)
            val scale = if (maxOrig > maxDimension.toDouble()) maxDimension.toDouble() / maxOrig else 1.0
            val targetWidth = (origWidth * scale).coerceAtLeast(1.0)
            val targetHeight = (origHeight * scale).coerceAtLeast(1.0)

            val scaledImage = if (scale < 1.0) {
                UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), true, 1.0)
                sourceImage.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
                val drawn = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                drawn ?: sourceImage
            } else {
                sourceImage
            }

            val jpegData: NSData = UIImageJPEGRepresentation(scaledImage, 0.8) ?: return null
            val docPath = documentsDirectoryPath() ?: return null
            val thumbDir = "$docPath/thumbnails".toPath()
            if (!FileSystem.SYSTEM.exists(thumbDir)) {
                FileSystem.SYSTEM.createDirectories(thumbDir)
            }
            val destPath = "$docPath/thumbnails/thumb_${NSUUID().UUIDString}.jpg"
            if (jpegData.writeToFile(destPath, atomically = true)) destPath else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? = withContext(Dispatchers.Default) {
        try {
            val cleanPath = resolveRebasedPath(uriString)
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
        if (filePath.isBlank() || filePath.startsWith("http")) return false
        return try {
            val cleanPath = resolveRebasedPath(filePath)
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
            val cleanPath = resolveRebasedPath(filePath)
            FileSystem.SYSTEM.exists(cleanPath.toPath())
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val cleanPath = resolveRebasedPath(imagePath)
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

    private fun documentsDirectoryPath(): String? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path

    private fun resolveRebasedPath(uriString: String): String =
        cleanUriPath(resolveLocalPath(uriString))

    private fun cleanUriPath(uriString: String): String {
        return if (uriString.startsWith("file://")) {
            uriString.removePrefix("file://")
        } else if (uriString.startsWith("file:")) {
            uriString.removePrefix("file:")
        } else {
            uriString
        }
    }

    override suspend fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean = withContext(Dispatchers.Default) {
        try {
            val path = resolveRebasedPath(filePath).toPath()
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
