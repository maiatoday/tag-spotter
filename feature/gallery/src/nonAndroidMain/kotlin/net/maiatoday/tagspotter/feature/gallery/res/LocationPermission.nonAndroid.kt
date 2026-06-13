package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class NonAndroidLocationPermissionState : LocationPermissionState {
    override val hasPermission: Boolean
        get() = true

    override fun requestPermission() {
        // No-op
    }
}

@Composable
actual fun rememberLocationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): LocationPermissionState {
    return remember { NonAndroidLocationPermissionState() }
}
