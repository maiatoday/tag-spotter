package net.maiatoday.tagspotter.core.sync

import android.content.Context
import androidx.core.net.toUri
import org.koin.core.context.GlobalContext
import java.io.File

actual fun readBytesFromFile(filePath: String): ByteArray? {
    return try {
        if (filePath.startsWith("content://") || filePath.startsWith("android.resource://")) {
            val context: Context? = try { GlobalContext.get().get() } catch (_: Exception) { null }
            if (context != null) {
                context.contentResolver.openInputStream(filePath.toUri())?.use { it.readBytes() }
            } else {
                null
            }
        } else {
            val cleanPath = if (filePath.startsWith("file://")) {
                filePath.removePrefix("file://")
            } else if (filePath.startsWith("file:")) {
                filePath.removePrefix("file:")
            } else {
                filePath
            }
            val file = File(cleanPath)
            if (file.exists() && file.isFile) {
                file.readBytes()
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
