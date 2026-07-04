package net.maiatoday.tagspotter.feature.main

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.SyncManager

object IosAuthHelper : KoinComponent {
    val authService: AuthService by inject()
    val syncManager: SyncManager by inject()
}

fun iosSignInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
    MainScope().launch {
        IosAuthHelper.authService.signInWithGoogle(idToken).onSuccess {
            IosAuthHelper.authService.currentUserFlow.first()?.uid?.let { uid ->
                IosAuthHelper.syncManager.startRealtimeSync(uid)
            }
            onResult(true, null)
        }.onFailure {
            onResult(false, it.message ?: "Unknown error")
        }
    }
}

fun createMainViewController(
    onTriggerFiles: (onPhotoPicked: (String) -> Unit) -> Unit,
    onGoogleSignInClick: (() -> Unit)? = null
): UIViewController {
    println("iOS createMainViewController: onGoogleSignInClick is ${if (onGoogleSignInClick != null) "NOT null" else "null (default fallback)"}")
    return ComposeUIViewController {
        MyApplicationTheme {
            MainNavigation(
                initialSpotId = null,
                onNavigateToSpotHandled = {},
                onTriggerFiles = onTriggerFiles,
                versionName = "1.0.0-ios",
                showToast = { println("Toast: $it") },
                onGoogleSignInClick = onGoogleSignInClick
            )
        }
    }
}

