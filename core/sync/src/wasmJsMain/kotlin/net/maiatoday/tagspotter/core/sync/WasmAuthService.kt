package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// External JS helper declarations
external fun webAuthListenState(
    onUserChanged: (uid: String, email: String, displayName: String) -> Unit,
    onNoUser: () -> Unit
)

external fun webAuthSignInWithGoogle(
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

external fun webAuthSignInWithEmail(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

external fun webAuthSignUpWithEmail(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

external fun webAuthSignOut(
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)

class WasmAuthService : AuthService {
    private val _currentUserFlow = MutableStateFlow<FirebaseUserWrapper?>(null)
    override val currentUserFlow: Flow<FirebaseUserWrapper?> = _currentUserFlow
    override val isGoogleSignInSupported: Boolean = true

    init {
        try {
            webAuthListenState(
                onUserChanged = { uid, email, displayName ->
                    _currentUserFlow.value = FirebaseUserWrapper(
                        uid = uid,
                        email = if (email.isEmpty()) null else email,
                        displayName = if (displayName.isEmpty()) null else displayName
                    )
                },
                onNoUser = {
                    _currentUserFlow.value = null
                }
            )
        } catch (e: Exception) {
            println("Error registering web auth state listener: ${e.message}")
        }
    }

    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            webAuthSignInWithGoogle(
                onSuccess = { if (continuation.isActive) continuation.resume(Result.success(Unit)) },
                onFailure = { err -> if (continuation.isActive) continuation.resume(Result.failure(Exception(err))) }
            )
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            webAuthSignInWithEmail(
                email,
                password,
                onSuccess = { if (continuation.isActive) continuation.resume(Result.success(Unit)) },
                onFailure = { err -> if (continuation.isActive) continuation.resume(Result.failure(Exception(err))) }
            )
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            webAuthSignUpWithEmail(
                email,
                password,
                onSuccess = { if (continuation.isActive) continuation.resume(Result.success(Unit)) },
                onFailure = { err -> if (continuation.isActive) continuation.resume(Result.failure(Exception(err))) }
            )
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        println("WASM: sendPasswordResetEmail not implemented for $email")
    }

    override suspend fun signOut() {
        suspendCancellableCoroutine<Unit> { continuation ->
            webAuthSignOut(
                onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                onFailure = { if (continuation.isActive) continuation.resume(Unit) }
            )
        }
    }
}
