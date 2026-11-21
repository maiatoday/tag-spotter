package net.maiatoday.tagspotter.feature.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maiatoday.tagspotter.R
import net.maiatoday.tagspotter.core.database.ImportPackWorker
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.SyncManager
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
  private var initialSpotId by mutableStateOf<Long?>(null)
  private var initialTab by mutableStateOf<Tab?>(null)
  private var initialTabRequestId by mutableStateOf(0L)
  private var isInitialTabHandled = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    handleImportIntent(intent)
    if (intent.hasExtra("EXTRA_SPOT_ID")) {
      initialSpotId = intent.getLongExtra("EXTRA_SPOT_ID", -1L).takeIf { it != -1L }
    }
    isInitialTabHandled = savedInstanceState?.getBoolean(KEY_INITIAL_TAB_HANDLED, false) ?: false
    if (!isInitialTabHandled && isTagAlongIntent(intent)) {
      initialTabRequestId += 1L
      initialTab = Tab.TagAlong
    }

    setContent {
      val settingsRepository: net.maiatoday.tagspotter.core.settings.SettingsRepository = koinInject()
      val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle()
      MyApplicationTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val currentTabRequestId = initialTabRequestId
          MainActivityContent(
            initialSpotId = initialSpotId,
            initialTab = initialTab,
            initialTabRequestId = currentTabRequestId,
            onNavigateToSpotHandled = { initialSpotId = null },
            onInitialTabHandled = {
              if (initialTabRequestId == currentTabRequestId) {
                initialTab = null
                isInitialTabHandled = true
              }
            }
          )
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(KEY_INITIAL_TAB_HANDLED, isInitialTabHandled)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleImportIntent(intent)
    if (intent.hasExtra("EXTRA_SPOT_ID")) {
      initialSpotId = intent.getLongExtra("EXTRA_SPOT_ID", -1L).takeIf { it != -1L }
    }
    if (isTagAlongIntent(intent)) {
      isInitialTabHandled = false
      initialTabRequestId += 1L
      initialTab = Tab.TagAlong
    }
  }

  private fun isTagAlongIntent(intent: Intent?): Boolean {
    return intent?.data?.toString() == "tagspotter://tagalong" ||
      intent?.action == "net.maiatoday.tagspotter.action.TAGALONG"
  }

  companion object {
    private const val KEY_INITIAL_TAB_HANDLED = "initial_tab_handled"
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

@Composable
fun MainActivityContent(
    initialSpotId: Long?,
    initialTab: Tab? = null,
    initialTabRequestId: Long = 0L,
    onNavigateToSpotHandled: () -> Unit,
    onInitialTabHandled: () -> Unit = {},
    viewModel: MainViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var tagAlongPhotoCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val pickTagAlongMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            tagAlongPhotoCallback?.invoke(uri.toString())
            tagAlongPhotoCallback = null
        }
    }
    val tagAlongMediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pickTagAlongMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    // Permission Launcher for GPS Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
        viewModel.updateLocationPermission(granted)
    }

    // Photo Gallery Picker Launcher (Native Visual Media Contract)
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handlePhotoPicked(uri.toString())
        }
    }

    // Permission Launcher for ACCESS_MEDIA_LOCATION (Android 10+)
    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val triggerFiles = { _: (String) -> Unit ->
        val permission = Manifest.permission.ACCESS_MEDIA_LOCATION
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            mediaLocationPermissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        viewModel.updateLocationPermission(granted)
    }

    val authService: AuthService = koinInject()
    val syncManager: SyncManager = koinInject()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authService, syncManager) {
        authService.currentUserFlow.collect { user ->
            if (user != null) {
                syncManager.startRealtimeSync(user.uid)
            } else {
                syncManager.stopRealtimeSync()
            }
        }
    }


    val googleSignInTrigger = {
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("261786695723-4tunva2l3vha7c7hmmr49ou39h3j3s26.apps.googleusercontent.com")
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                
                authService.signInWithGoogle(idToken).onSuccess {
                    authService.currentUserFlow.first()?.uid?.let { uid ->
                        syncManager.startRealtimeSync(uid)
                    }
                    Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Sync Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Cancelled or failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        Unit
    }

    MainNavigation(
        initialSpotId = initialSpotId,
        initialTab = initialTab,
        initialTabRequestId = initialTabRequestId,
        onNavigateToSpotHandled = onNavigateToSpotHandled,
        onInitialTabHandled = onInitialTabHandled,
        onTriggerFiles = triggerFiles,
        versionName = versionName,
        showToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
        onGoogleSignInClick = googleSignInTrigger,
        tagAlongContent = { onOpenSpotDetail, onViewOnMainMap ->
            TagAlongScreen(
                onOpenSpotDetail = onOpenSpotDetail,
                onViewOnMainMap = onViewOnMainMap,
                onPickPhoto = { onPicked ->
                    tagAlongPhotoCallback = onPicked
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val permission = Manifest.permission.ACCESS_MEDIA_LOCATION
                        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                        if (isGranted) {
                            pickTagAlongMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            tagAlongMediaLocationPermissionLauncher.launch(permission)
                        }
                    } else {
                        pickTagAlongMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            )
        }
    )
}
