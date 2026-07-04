package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NonWebAuthService : AuthService {
    private val auth = Firebase.auth

    override val currentUserFlow: Flow<FirebaseUserWrapper?> = auth.authStateChanged.map { user ->
        user?.let {
            FirebaseUserWrapper(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName
            )
        }
    }

    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> = runCatching {
        if (idToken == null) throw IllegalArgumentException("ID token is required for non-web Google Sign-In")
        val credential = GoogleAuthProvider.credential(idToken, null)
        auth.signInWithCredential(credential)
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password)
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email, password)
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
