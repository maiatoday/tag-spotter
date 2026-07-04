package net.maiatoday.tagspotter.core.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import androidx.core.graphics.createBitmap

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

    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? = withContext(Dispatchers.IO) {
        try {
            val uri = uriString.toUri()
            val photoUri =
                try {
                    android.provider.MediaStore.setRequireOriginal(uri)
                } catch (_: Exception) {
                    uri
                }
            val bytes = context.contentResolver.openInputStream(photoUri)?.use { it.readBytes() } ?: return@withContext null
            ExifMetadataParser.extractMetadata(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = decodeScaledBitmapToAndroidBitmap(imagePath, maxDimension) ?: return@withContext null
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        bitmap.recycle()
        stream.toByteArray()
    }

    override suspend fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).writeBytes(bytes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun decodeScaledBitmapToAndroidBitmap(imagePath: String, maxDimension: Int): Bitmap? {
        try {
            if (imagePath.startsWith("content://") || imagePath.startsWith("file://") || imagePath.startsWith("android.resource://")) {
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
                if (srcWidth <= 0 || srcHeight <= 0) {
                    if (imagePath.startsWith("android.resource://")) {
                        val bitmap = decodeResourceToBitmap(context, uri, maxDimension)
                        if (bitmap != null) return bitmap
                    }
                    return null
                }
                
                // Calculate sample size (power of 2)
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                }
                if (bitmap == null && imagePath.startsWith("android.resource://")) {
                    val fallbackBitmap = decodeResourceToBitmap(context, uri, maxDimension)
                    if (fallbackBitmap != null) return fallbackBitmap
                }
                return bitmap
            } else {
                val file = File(imagePath)
                if (!file.exists()) return null
                
                // Get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)
                
                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                if (srcWidth <= 0 || srcHeight <= 0) return null
                
                // Calculate sample size (power of 2)
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                return BitmapFactory.decodeFile(imagePath, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun decodeResourceToBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        try {
            val packageName = uri.authority ?: context.packageName
            val pathSegments = uri.pathSegments
            val resId = when (pathSegments.size) {
                2 -> {
                    val type = pathSegments[0]
                    val name = pathSegments[1]
                    context.resources.getIdentifier(name, type, packageName)
                }
                1 -> {
                    pathSegments[0].toIntOrNull() ?: 0
                }
                else -> {
                    0
                }
            }
            if (resId != 0) {
                val drawable = ResourcesCompat.getDrawable(context.resources, resId, context.theme)
                if (drawable != null) {
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else maxDimension
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else maxDimension
                    
                    val maxDim = maxOf(width, height)
                    val scale = if (maxDim > maxDimension) maxDimension.toFloat() / maxDim else 1f
                    val finalWidth = (width * scale).toInt().coerceAtLeast(1)
                    val finalHeight = (height * scale).toInt().coerceAtLeast(1)
                    
                    val bitmap = createBitmap(finalWidth, finalHeight)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, finalWidth, finalHeight)
                    drawable.draw(canvas)
                    return bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
