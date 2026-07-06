package net.maiatoday.tagspotter.core.sync

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.maiatoday.tagspotter.core.settings.SecureStorage

class JvmAuthService(
    private val client: JvmFirebaseClient,
    private val secureStorage: SecureStorage
) : AuthService {

    private val _currentUserFlow = MutableStateFlow<FirebaseUserWrapper?>(null)
    override val currentUserFlow: Flow<FirebaseUserWrapper?> = _currentUserFlow.asStateFlow()

    override val isGoogleSignInSupported: Boolean = false

    init {
        val uid = secureStorage.getString("firebase_user_uid", "")
        val email = secureStorage.getString("firebase_user_email", "")
        val displayName = secureStorage.getString("firebase_user_display_name", "")
        val idToken = secureStorage.getString("firebase_id_token", "")
        if (uid.isNotEmpty() && idToken.isNotEmpty()) {
            _currentUserFlow.value = FirebaseUserWrapper(
                uid = uid,
                email = email.ifEmpty { null },
                displayName = displayName.ifEmpty { null }
            )
        }
    }

    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> = runCatching {
        throw UnsupportedOperationException("Google Sign-In is not supported on Desktop JVM REST fallback.")
    }

    private fun extractAndMapError(text: String, defaultMessage: String): Exception {
        return try {
            val json = client.jsonConfig.parseToJsonElement(text).jsonObject
            val errorObj = json["error"]?.jsonObject
            val rawMessage = errorObj?.get("message")?.jsonPrimitive?.content ?: ""
            val mapped = mapAuthError(rawMessage, defaultMessage)
            Exception(mapped)
        } catch (e: Exception) {
            Exception(defaultMessage)
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> = runCatching {
        val response = client.authClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${JvmFirebaseConfig.apiKey}") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            })
        }
        val text = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw extractAndMapError(text, "Sign in failed")
        }
        val json = client.jsonConfig.parseToJsonElement(text).jsonObject
        val idToken = json["idToken"]?.jsonPrimitive?.content ?: throw Exception("No ID token in response")
        val refreshToken = json["refreshToken"]?.jsonPrimitive?.content ?: throw Exception("No Refresh token in response")
        val uid = json["localId"]?.jsonPrimitive?.content ?: throw Exception("No localId in response")
        val resEmail = json["email"]?.jsonPrimitive?.content
        val displayName = json["displayName"]?.jsonPrimitive?.content

        secureStorage.putString("firebase_id_token", idToken)
        secureStorage.putString("firebase_refresh_token", refreshToken)
        secureStorage.putString("firebase_user_uid", uid)
        secureStorage.putString("firebase_user_email", resEmail ?: "")
        secureStorage.putString("firebase_user_display_name", displayName ?: "")

        _currentUserFlow.value = FirebaseUserWrapper(uid, resEmail, displayName)
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> = runCatching {
        val response = client.authClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${JvmFirebaseConfig.apiKey}") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            })
        }
        val text = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw extractAndMapError(text, "Sign up failed")
        }
        val json = client.jsonConfig.parseToJsonElement(text).jsonObject
        val idToken = json["idToken"]?.jsonPrimitive?.content ?: throw Exception("No ID token in response")
        val refreshToken = json["refreshToken"]?.jsonPrimitive?.content ?: throw Exception("No Refresh token in response")
        val uid = json["localId"]?.jsonPrimitive?.content ?: throw Exception("No localId in response")
        val resEmail = json["email"]?.jsonPrimitive?.content

        secureStorage.putString("firebase_id_token", idToken)
        secureStorage.putString("firebase_refresh_token", refreshToken)
        secureStorage.putString("firebase_user_uid", uid)
        secureStorage.putString("firebase_user_email", resEmail ?: "")
        secureStorage.putString("firebase_user_display_name", "")

        _currentUserFlow.value = FirebaseUserWrapper(uid, resEmail, null)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val response = client.authClient.post("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=${JvmFirebaseConfig.apiKey}") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("requestType", "PASSWORD_RESET")
                put("email", email)
            })
        }
        val text = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw extractAndMapError(text, "Password reset failed")
        }
    }

    override suspend fun signOut() {
        secureStorage.putString("firebase_id_token", "")
        secureStorage.putString("firebase_refresh_token", "")
        secureStorage.putString("firebase_user_uid", "")
        secureStorage.putString("firebase_user_email", "")
        secureStorage.putString("firebase_user_display_name", "")
        _currentUserFlow.value = null
    }
}
