package net.maiatoday.tagspotter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import net.maiatoday.tagspotter.theme.MyApplicationTheme

import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.data.SpotDetails

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    handleImportIntent(intent)
    setContent {
      MyApplicationTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleImportIntent(intent)
  }

  private fun getFileName(uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
      val cursor = contentResolver.query(uri, null, null, null, null)
      cursor?.use {
        if (it.moveToFirst()) {
          val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (index != -1) {
            result = it.getString(index)
          }
        }
      }
    }
    if (result == null) {
      result = uri.path
      val cut = result?.lastIndexOf('/') ?: -1
      if (cut != -1) {
        result = result?.substring(cut + 1)
      }
    }
    return result
  }

  private fun handleImportIntent(intent: Intent?) {
    if (intent == null || intent.action != Intent.ACTION_VIEW) return
    val uri = intent.data ?: return

    val fileName = getFileName(uri)
    if (fileName != null && !fileName.endsWith(".ts_pack", ignoreCase = true)) {
      Toast.makeText(this, "Not a valid Tag Spotter Pack (.ts_pack)", Toast.LENGTH_LONG).show()
      return
    }

    val app = applicationContext as TagSpotterApplication

    lifecycleScope.launch(Dispatchers.IO) {
      try {
        val inputStream = contentResolver.openInputStream(uri)
        val jsonText = inputStream?.use { stream ->
          stream.bufferedReader().readText()
        }
        if (jsonText != null) {
          val spots = Json.decodeFromString<List<SpotDetails>>(jsonText)
          val importedCount = app.repository.importSpots(spots)
          withContext(Dispatchers.Main) {
            Toast.makeText(
              this@MainActivity,
              "Imported $importedCount spots!",
              Toast.LENGTH_LONG
            ).show()
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          Toast.makeText(
            this@MainActivity,
            "Failed to import: ${e.localizedMessage}",
            Toast.LENGTH_LONG
          ).show()
        }
      }
    }
  }
}
