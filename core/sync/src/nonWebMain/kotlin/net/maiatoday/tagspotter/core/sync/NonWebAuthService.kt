package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class NonWebAuthService : AuthService {
    private val auth by lazy {
        try {
            Firebase.auth
        } catch (e: Exception) {
            println("Firebase Auth not available (expected on Desktop JVM): ${e.message}")
            null
        }
    }

    private val mockUserFlow = kotlinx.coroutines.flow.MutableStateFlow<FirebaseUserWrapper?>(null)

    override val currentUserFlow: Flow<FirebaseUserWrapper?> = flow {
        val actualAuth = auth
        if (actualAuth != null) {
            emitAll(actualAuth.authStateChanged.map { user ->
                user?.let {
                    FirebaseUserWrapper(
                        uid = it.uid,
                        email = it.email,
                        displayName = it.displayName
                    )
                }
            })
        } else {
            emitAll(mockUserFlow)
        }
    }

    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> = runCatching {
        val actualAuth = auth
        if (actualAuth != null) {
            if (idToken == null) throw IllegalArgumentException("ID token is required for non-web Google Sign-In")
            val credential = GoogleAuthProvider.credential(idToken, null)
            actualAuth.signInWithCredential(credential)
        } else {
            mockUserFlow.value = FirebaseUserWrapper("mock-google-uid", "desktop-user@google.mock", "Desktop Google User")
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> = runCatching {
        val actualAuth = auth
        if (actualAuth != null) {
            actualAuth.signInWithEmailAndPassword(email, password)
        } else {
            mockUserFlow.value = FirebaseUserWrapper("mock-email-uid", email, email.substringBefore("@"))
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> = runCatching {
        val actualAuth = auth
        if (actualAuth != null) {
            actualAuth.createUserWithEmailAndPassword(email, password)
        } else {
            mockUserFlow.value = FirebaseUserWrapper("mock-email-uid", email, email.substringBefore("@"))
        }
    }

    override suspend fun signOut() {
        val actualAuth = auth
        if (actualAuth != null) {
            actualAuth.signOut()
        } else {
            mockUserFlow.value = null
        }
    }
}
