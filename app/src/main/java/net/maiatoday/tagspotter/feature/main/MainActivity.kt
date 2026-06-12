package net.maiatoday.tagspotter.feature.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import net.maiatoday.tagspotter.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import net.maiatoday.tagspotter.core.database.ImportPackWorker
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
  private var initialSpotId by mutableStateOf<Long?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    handleImportIntent(intent)
    if (intent.hasExtra("EXTRA_SPOT_ID")) {
      initialSpotId = intent.getLongExtra("EXTRA_SPOT_ID", -1L).takeIf { it != -1L }
    }

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            initialSpotId = initialSpotId,
            onNavigateToSpotHandled = { initialSpotId = null }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleImportIntent(intent)
    if (intent.hasExtra("EXTRA_SPOT_ID")) {
      initialSpotId = intent.getLongExtra("EXTRA_SPOT_ID", -1L).takeIf { it != -1L }
    }
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
    val isFileScheme = uri.scheme == "file"
    if (isFileScheme && fileName != null && 
        !fileName.endsWith(".ts_pack", ignoreCase = true) && 
        !fileName.endsWith(".zip", ignoreCase = true)) {
      Toast.makeText(this, getString(R.string.toast_invalid_pack), Toast.LENGTH_LONG).show()
      return
    }

    Toast.makeText(this, getString(R.string.toast_starting_import), Toast.LENGTH_SHORT).show()

    lifecycleScope.launch(Dispatchers.IO) {
      try {
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
          // Copy input stream to a temp file in cacheDir to preserve access in background
          val tempFile = File(cacheDir, "import_pending_${UUID.randomUUID()}.ts_pack")
          tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
          }

          val importWorkRequest = OneTimeWorkRequestBuilder<ImportPackWorker>()
            .setInputData(
              workDataOf(
                ImportPackWorker.KEY_TEMP_FILE_PATH to tempFile.absolutePath
              )
            )
            .build()
          WorkManager.getInstance(applicationContext).enqueue(importWorkRequest)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          Toast.makeText(
            this@MainActivity,
            getString(R.string.toast_failed_import_start, e.localizedMessage ?: ""),
            Toast.LENGTH_LONG
          ).show()
        }
      }
    }
  }
}
