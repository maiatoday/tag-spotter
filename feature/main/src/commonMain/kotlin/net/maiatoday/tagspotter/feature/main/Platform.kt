package net.maiatoday.tagspotter.feature.main

import androidx.compose.runtime.Composable

expect val isCameraSupported: Boolean

@Composable
expect fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit
): () -> Unit

