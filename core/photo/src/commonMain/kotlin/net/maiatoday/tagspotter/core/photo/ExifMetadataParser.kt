package net.maiatoday.tagspotter.core.photo

import com.ashampoo.kim.Kim
import com.ashampoo.kim.common.PhotoMetadataConverter

object ExifMetadataParser {
    fun extractMetadata(bytes: ByteArray): PhotoMetadata? {
        return try {
            val imageMetadata = Kim.readMetadata(bytes) ?: return null
            val photoMetadata = PhotoMetadataConverter.convertToPhotoMetadata(imageMetadata)
            PhotoMetadata(
                latitude = photoMetadata.gpsCoordinates?.latitude,
                longitude = photoMetadata.gpsCoordinates?.longitude,
                timestamp = photoMetadata.takenDate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
