package com.example.tagspotter.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageOptimizer {

    fun optimizeAndSaveImage(context: Context, sourceFile: File): String? {
        return try {
            val destinationFile = File(context.filesDir, "spot_${UUID.randomUUID()}.jpg")
            sourceFile.inputStream().use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun optimizeAndSaveImage(context: Context, sourceUri: Uri): String? {
        return try {
            val destinationFile = File(context.filesDir, "spot_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
