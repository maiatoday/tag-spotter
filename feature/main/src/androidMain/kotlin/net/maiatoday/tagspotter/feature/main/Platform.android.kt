package net.maiatoday.tagspotter.feature.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current
    val picker = rememberImagePickerKMP()
    val scope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        picker.launchCamera()
    }

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

    return {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            picker.launchCamera()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
