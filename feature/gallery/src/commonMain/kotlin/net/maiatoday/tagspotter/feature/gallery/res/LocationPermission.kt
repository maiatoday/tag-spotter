package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable

interface LocationPermissionState {
    val hasPermission: Boolean
    fun requestPermission()
}

@Composable
expect fun rememberLocationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): LocationPermissionState
