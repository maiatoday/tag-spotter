package net.maiatoday.tagspotter.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

object ExifLocationExtractor {

    data class PhotoMetadata(
        val latitude: Double?,
        val longitude: Double?,
        val timestamp: Long?
    )

    fun getPhotoMetadata(context: Context, imageUri: Uri): PhotoMetadata? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                val lat = latLong?.getOrNull(0)
                val lng = latLong?.getOrNull(1)

                val datetimeString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                val timestamp = datetimeString?.let {
                    try {
                        val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                        sdf.parse(it)?.time
                    } catch (e: Exception) {
                        null
                    }
                }

                if (lat != null || lng != null || timestamp != null) {
                    PhotoMetadata(lat, lng, timestamp)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Keep backwards compatibility for any existing code calling getPhotoLocation
    data class ExifLocation(
        val latitude: Double,
        val longitude: Double
    )

    fun getPhotoLocation(context: Context, imageUri: Uri): ExifLocation? {
        val meta = getPhotoMetadata(context, imageUri)
        if (meta?.latitude != null && meta.longitude != null) {
            return ExifLocation(meta.latitude, meta.longitude)
        }
        return null
    }
}
