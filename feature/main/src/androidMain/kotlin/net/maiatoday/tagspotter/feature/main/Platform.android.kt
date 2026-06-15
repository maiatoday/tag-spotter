package net.maiatoday.tagspotter.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import kotlinx.coroutines.launch

actual val isCameraSupported: Boolean = true

@Composable
actual fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val picker = rememberImagePickerKMP()
    val scope = rememberCoroutineScope()

    LaunchedEffect(picker.result) {
        val result = picker.result
        if (result is ImagePickerResult.Success) {
            val photo = result.photos.firstOrNull()
            if (photo != null) {
                scope.launch {
                    try {
                        val bytes = photo.loadBytes()
                        onPhotoCaptured(bytes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onError("Failed to load captured image bytes: ${e.message}")
                    }
                }
            }
        } else if (result is ImagePickerResult.Error) {
            onError("Camera error: ${result.exception.message}")
        }
    }

    return { picker.launchCamera() }
}
