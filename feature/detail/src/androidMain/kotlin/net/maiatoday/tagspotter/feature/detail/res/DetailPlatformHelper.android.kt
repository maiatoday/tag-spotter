package net.maiatoday.tagspotter.feature.detail.res

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.photo.ImageOptimizer
import net.maiatoday.tagspotter.feature.detail.R
import java.io.File
import java.util.Locale

@Composable
actual fun rememberDetailPlatformHelper(): DetailPlatformHelper {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        object : DetailPlatformHelper {
            override fun searchImageWithLens(imagePath: String) {
                if (imagePath.isNotEmpty()) {
                    try {
                        val file = File(imagePath)
                        if (file.exists()) {
                            val authority = "${context.packageName}.fileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.content_desc_search_lens)))
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_img_file_not_found), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.toast_failed_share_img, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_no_img_search), Toast.LENGTH_SHORT).show()
                }
            }

            @Composable
            override fun rememberSpeechRecognizerLauncher(onResult: (String) -> Unit): () -> Unit {
                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val data = result.data
                            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            val spokenText = results?.firstOrNull() ?: ""
                            if (spokenText.isNotEmpty()) {
                                onResult(spokenText)
                            }
                        }
                    }
                )
                return {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.recognizer_prompt))
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, context.getString(R.string.speech_recognition_unsupported), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            @Composable
            override fun rememberPhotoPickerLauncher(onPhotosPicked: (List<Pair<String, String>>) -> Unit): () -> Unit {
                val pickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris: List<Uri> ->
                    if (uris.isNotEmpty()) {
                        uris.forEach { uri ->
                            try {
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        scope.launch(Dispatchers.Default) {
                            val list = uris.mapIndexedNotNull { index, uri ->
                                val thumbnailPath = ImageOptimizer.createThumbnail(context, uri)
                                if (thumbnailPath != null) {
                                    uri.toString() to thumbnailPath
                                } else {
                                    null
                                }
                            }
                            onPhotosPicked(list)
                        }
                    }
                }
                return {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }

            override fun navigateToLocation(latitude: Double, longitude: Double) {
                val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            }

            override fun checkOriginalPhotoDeleted(imagePath: String, callback: (Boolean) -> Unit) {
                if (imagePath.startsWith("android.resource://") || imagePath.startsWith("http")) {
                    callback(false)
                } else if (imagePath.startsWith("content://")) {
                    val uri = Uri.parse(imagePath)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { }
                        callback(false)
                    } catch (_: Exception) {
                        callback(true)
                    }
                } else {
                    val file = File(imagePath)
                    callback(!file.exists())
                }
            }
        }
    }
}
