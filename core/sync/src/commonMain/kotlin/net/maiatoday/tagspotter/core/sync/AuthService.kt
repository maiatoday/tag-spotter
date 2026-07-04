package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.Flow

data class FirebaseUserWrapper(
    val uid: String,
    val email: String?,
    val displayName: String?
)

interface AuthService {
    val currentUserFlow: Flow<FirebaseUserWrapper?>
    suspend fun signInWithGoogle(idToken: String? = null): Result<Unit>
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit>
    suspend fun signOut()
}
