package com.example.tagspotter.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object MediaStorageHelper {

    /**
     * Saves a captured image file to the public DCIM/Camera directory using MediaStore.
     * Returns the Uri of the saved public image.
     */
    fun saveImageToPublicGallery(context: Context, sourceFile: File): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            val filename = "spot_${System.currentTimeMillis()}.jpg"
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        try {
            sourceFile.inputStream().use { inputStream ->
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
            return imageUri
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            resolver.delete(imageUri, null, null)
            return null
        }
    }
}
