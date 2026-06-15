package net.maiatoday.tagspotter.feature.main

import androidx.compose.runtime.Composable

actual val isCameraSupported: Boolean = false

@Composable
actual fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    return {}
}
