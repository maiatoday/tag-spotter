package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable

interface DetailPlatformHelper {
    fun searchImageWithLens(imagePath: String)
    @Composable
    fun rememberSpeechRecognizerLauncher(onResult: (String) -> Unit): () -> Unit
    @Composable
    fun rememberPhotoPickerLauncher(onPhotosPicked: (List<Pair<String, String>>) -> Unit): () -> Unit
    fun navigateToLocation(latitude: Double, longitude: Double)
    fun checkOriginalPhotoDeleted(imagePath: String, callback: (Boolean) -> Unit)
}

@Composable
expect fun rememberDetailPlatformHelper(): DetailPlatformHelper
