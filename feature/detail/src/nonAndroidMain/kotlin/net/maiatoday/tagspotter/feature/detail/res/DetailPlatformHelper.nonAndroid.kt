package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDetailPlatformHelper(): DetailPlatformHelper {
    return remember {
        object : DetailPlatformHelper {
            override fun searchImageWithLens(imagePath: String) {
                println("Search image with lens: $imagePath")
            }

            @Composable
            override fun rememberSpeechRecognizerLauncher(onResult: (String) -> Unit): () -> Unit {
                return {
                    println("Speech recognition is not supported on this platform.")
                }
            }

            @Composable
            override fun rememberPhotoPickerLauncher(onPhotosPicked: (List<Pair<String, String>>) -> Unit): () -> Unit {
                return {
                    println("Photo picker is not supported on this platform.")
                }
            }

            override fun navigateToLocation(latitude: Double, longitude: Double) {
                println("Navigate to location: lat=$latitude, lng=$longitude")
            }

            override fun checkOriginalPhotoDeleted(imagePath: String, callback: (Boolean) -> Unit) {
                callback(false)
            }
        }
    }
}
