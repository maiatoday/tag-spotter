package com.example.tagspotter.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ExifLocationExtractor {

    data class ExifLocation(
        val latitude: Double,
        val longitude: Double
    )

    fun getPhotoLocation(context: Context, imageUri: Uri): ExifLocation? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    ExifLocation(latLong[0], latLong[1])
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
