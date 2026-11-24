package net.maiatoday.tagspotter.core.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
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
            val bytes = readBytesForMetadata(uriString) ?: return@withContext null
            extractMetadataFromBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readBytesForMetadata(uriString: String): ByteArray? {
        if (!uriString.startsWith("content://")) {
            val cleanPath = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString
            val file = File(cleanPath)
            if (file.exists() && file.isFile) {
                return file.readBytes()
            }
            if (!uriString.startsWith("file://")) {
                return null
            }
        }

        val uri = uriString.toUri()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.authority == MediaStore.AUTHORITY) {
            val originalBytes = try {
                val photoUri = MediaStore.setRequireOriginal(uri)
                context.contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
            if (originalBytes != null) return originalBytes
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    internal companion object {
        private fun isValidCoordinate(lat: Double?, lng: Double?): Boolean =
            lat != null && lng != null && !(lat == 0.0 && lng == 0.0)

        internal fun extractMetadataFromBytes(bytes: ByteArray): PhotoMetadata? {
            if (bytes.isEmpty()) return null
            val kimMeta = ExifMetadataParser.extractMetadata(bytes)
            val kimHasCoords = isValidCoordinate(kimMeta?.latitude, kimMeta?.longitude)
            val kimHasTimestamp = kimMeta?.timestamp != null

            if (kimHasCoords && kimHasTimestamp) {
                return kimMeta
            }

            val exifMeta = runCatching {
                val exif = ExifInterface(ByteArrayInputStream(bytes))
                val latLong = exif.latLong
                val lat = latLong?.getOrNull(0)
                val lng = latLong?.getOrNull(1)
                val validLat = if (isValidCoordinate(lat, lng)) lat else null
                val validLng = if (isValidCoordinate(lat, lng)) lng else null
                val timestamp = parseExifDate(
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                )
                PhotoMetadata(
                    latitude = validLat,
                    longitude = validLng,
                    timestamp = timestamp
                )
            }.getOrNull()

            val finalLat = if (kimHasCoords) kimMeta?.latitude else exifMeta?.latitude
            val finalLng = if (kimHasCoords) kimMeta?.longitude else exifMeta?.longitude
            val finalTimestamp = kimMeta?.timestamp ?: exifMeta?.timestamp

            return if (finalLat != null || finalLng != null || finalTimestamp != null) {
                PhotoMetadata(
                    latitude = finalLat,
                    longitude = finalLng,
                    timestamp = finalTimestamp
                )
            } else {
                null
            }
        }

        private fun parseExifDate(dateString: String?): Long? {
            if (dateString.isNullOrBlank()) return null
            return runCatching {
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(dateString)?.time
            }.getOrNull()
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

    override fun fileExists(filePath: String): Boolean {
        if (filePath.isEmpty() ||
            filePath.startsWith("content://") ||
            filePath.startsWith("android.resource://") ||
            filePath.startsWith("http://") ||
            filePath.startsWith("https://")
        ) {
            return false
        }
        val cleanPath = if (filePath.startsWith("file://")) filePath.removePrefix("file://") else filePath
        return try {
            val file = File(cleanPath)
            file.exists() && file.isFile
        } catch (e: Exception) {
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
