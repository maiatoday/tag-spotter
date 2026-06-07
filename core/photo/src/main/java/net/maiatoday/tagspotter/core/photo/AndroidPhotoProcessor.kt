package net.maiatoday.tagspotter.core.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val uri = uriString.toUri()
        return ImageOptimizer.createThumbnail(context, uri)
    }

    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? {
        val uri = uriString.toUri()
        val photoUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                android.provider.MediaStore.setRequireOriginal(uri)
            } catch (_: Exception) {
                uri
            }
        } else {
            uri
        }
        val meta = ExifLocationExtractor.getPhotoMetadata(context, photoUri) ?: return null
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
        if (filePath.isEmpty() ||
            filePath.startsWith("content://") ||
            filePath.startsWith("android.resource://") ||
            filePath.startsWith("http://") ||
            filePath.startsWith("https://")
        ) {
            return false
        }
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

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!imagePath.startsWith("content://") && !imagePath.startsWith("file://")) {
                val file = File(imagePath)
                if (!file.exists()) return@withContext null
                
                // Get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)
                
                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                if (srcWidth <= 0 || srcHeight <= 0) return@withContext null
                
                // Calculate sample size (power of 2)
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                BitmapFactory.decodeFile(imagePath, decodeOptions)
            } else {
                val uri = imagePath.toUri()
                // Get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
                
                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                if (srcWidth <= 0 || srcHeight <= 0) return@withContext null
                
                // Calculate sample size (power of 2)
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
