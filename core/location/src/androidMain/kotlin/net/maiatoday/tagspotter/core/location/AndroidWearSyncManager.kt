package net.maiatoday.tagspotter.core.location

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails
import java.io.ByteArrayOutputStream
import java.io.File


class AndroidWearSyncManager(private val context: Context) : WearSyncManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun shareSpotToWatch(spotDetails: SpotDetails) {
        scope.launch {
            try {
                Log.d("WearSyncManager", "Sharing spot details to watch: ${spotDetails.spot.description}")
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    Log.d("WearSyncManager", "No connected nodes to share spot details.")
                    return@launch
                }

                val json = Json.encodeToString(spotDetails)
                val payload = json.toByteArray(Charsets.UTF_8)

                for (node in nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/show_spot", payload)
                    Log.d("WearSyncManager", "Sent /show_spot to node: ${node.id}")
                }

                // If the spot has a main image, send the photo too!
                val mainImage = spotDetails.images.firstOrNull { it.isMain } ?: spotDetails.images.firstOrNull()
                if (mainImage != null) {
                    val pathToSend = mainImage.thumbnailPath.ifEmpty { mainImage.imagePath }
                    sendSpotPhoto(spotDetails.spot.id, pathToSend)
                }
            } catch (e: Exception) {
                Log.e("WearSyncManager", "Error sharing spot to watch", e)
            }
        }
    }

    override fun sendSpotPhoto(spotId: Long, imagePath: String) {
        scope.launch {
            try {
                Log.d("WearSyncManager", "Preparing photo for spot: $spotId from path: $imagePath")
                val uri = if (imagePath.startsWith("content://") || 
                             imagePath.startsWith("android.resource://") || 
                             imagePath.startsWith("file://")) {
                    imagePath.toUri()
                } else {
                    Uri.fromFile(File(imagePath))
                }

                // Verify the URI is readable
                val testStream = try {
                    context.contentResolver.openInputStream(uri)
                } catch (_: Exception) {
                    null
                }
                if (testStream == null) {
                    Log.w("WearSyncManager", "Image file/URI does not exist or cannot be opened: $imagePath")
                    return@launch
                }
                testStream.close()

                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    Log.d("WearSyncManager", "No connected nodes to send photo.")
                    return@launch
                }

                // 1. Decode sub-sampled bitmap to avoid OutOfMemory or huge files
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                
                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                if (srcWidth <= 0 || srcHeight <= 0) {
                    Log.w("WearSyncManager", "Invalid dimensions: w=$srcWidth, h=$srcHeight for path: $imagePath")
                    return@launch
                }

                val targetDim = 300
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= targetDim) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                val subSampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@launch

                // 2. Rotate image based on Exif data if needed
                val exifInterface = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream)
                    }
                } catch (e: Exception) {
                    null
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

                val rotated = if (degrees != 0f) {
                    val matrix = Matrix().apply { postRotate(degrees) }
                    val bmp = Bitmap.createBitmap(subSampled, 0, 0, subSampled.width, subSampled.height, matrix, true)
                    if (bmp != subSampled) {
                        subSampled.recycle()
                    }
                    bmp
                } else {
                    subSampled
                }

                // 3. Precise scale down to 300px max dimension
                val currentWidth = rotated.width
                val currentHeight = rotated.height
                val scale = targetDim.toFloat() / maxOf(currentWidth, currentHeight)
                val finalBitmap = if (scale < 1.0f) {
                    val destW = (currentWidth * scale).toInt()
                    val destH = (currentHeight * scale).toInt()
                    val scaled = rotated.scale(destW, destH)
                    if (scaled != rotated) {
                        rotated.recycle()
                    }
                    scaled
                } else {
                    rotated
                }

                // 4. Compress to JPEG
                val bos = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos)
                val bytes = bos.toByteArray()
                finalBitmap.recycle()

                Log.d("WearSyncManager", "Compressed photo size: ${bytes.size} bytes")

                // 5. Send to all nodes
                for (node in nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/spot_photo", bytes)
                    Log.d("WearSyncManager", "Sent /spot_photo to node: ${node.id}")
                }
            } catch (e: Exception) {
                Log.e("WearSyncManager", "Error sending spot photo to watch", e)
            }
        }
    }
}
