package net.maiatoday.tagspotter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.util.UUID
import androidx.core.graphics.scale

object ImageOptimizer {

    fun createThumbnail(context: Context, sourceFile: File): String? {
        return try {
            compressAndScale(context, { sourceFile.inputStream() }, 480)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createThumbnail(context: Context, sourceUri: Uri): String? {
        return try {
            compressAndScale(context, { context.contentResolver.openInputStream(sourceUri) }, 480)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressAndScale(
        context: Context,
        inputStreamProvider: () -> InputStream?,
        maxDimension: Int
    ): String? {
        var inputStream = inputStreamProvider() ?: return null
        try {
            // 1. Get original dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            // 2. Calculate sample size (power of 2)
            var inSampleSize = 1
            val maxSrcDim = maxOf(srcWidth, srcHeight)
            while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 3. Decode sub-sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            inputStream = inputStreamProvider() ?: return null
            val subSampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return null
            inputStream.close()

            // 3b. Read EXIF orientation and rotate if needed
            val exifInterface = inputStreamProvider()?.use { stream ->
                ExifInterface(stream)
            }
            val orientation = exifInterface?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val rotatedBitmap = if (degrees != 0f) {
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(subSampledBitmap, 0, 0, subSampledBitmap.width, subSampledBitmap.height, matrix, true)
                if (rotated != subSampledBitmap) {
                    subSampledBitmap.recycle()
                }
                rotated
            } else {
                subSampledBitmap
            }

            // 4. Scale down precisely to fit maxDimension
            val currentWidth = rotatedBitmap.width
            val currentHeight = rotatedBitmap.height
            val scale = maxDimension.toFloat() / maxOf(currentWidth, currentHeight)
            val finalBitmap = if (scale < 1.0f) {
                val destWidth = (currentWidth * scale).toInt()
                val destHeight = (currentHeight * scale).toInt()
                val scaled = rotatedBitmap.scale(destWidth, destHeight)
                if (scaled != rotatedBitmap) {
                    rotatedBitmap.recycle()
                }
                scaled
            } else {
                rotatedBitmap
            }

            // 5. Save compressed thumbnail to private directory
            val thumbnailDir = File(context.filesDir, "thumbnails")
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
            }
            val destinationFile = File(thumbnailDir, "thumb_${UUID.randomUUID()}.jpg")
            destinationFile.outputStream().use { outputStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }
            finalBitmap.recycle()

            return destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }
    }
}
