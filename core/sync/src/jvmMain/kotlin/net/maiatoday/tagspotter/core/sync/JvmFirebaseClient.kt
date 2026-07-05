package net.maiatoday.tagspotter.core.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.maiatoday.tagspotter.core.settings.SecureStorage
import java.io.File
import java.util.Properties

object JvmFirebaseConfig {
    var apiKey: String = ""
    var projectId: String = ""
    var storageBucket: String = ""

    private fun findLocalProperties(): File? {
        val f1 = File("local.properties")
        if (f1.exists()) return f1
        val f2 = File("../local.properties")
        if (f2.exists()) return f2
        return null
    }

    init {
        apiKey = System.getenv("FIREBASE_API_KEY") ?: ""
        projectId = System.getenv("FIREBASE_PROJECT_ID") ?: ""
        storageBucket = System.getenv("FIREBASE_STORAGE_BUCKET") ?: ""

        val file = findLocalProperties()
        if (file != null && file.exists()) {
            val props = Properties()
            try {
                file.inputStream().use { props.load(it) }
                if (apiKey.isEmpty()) apiKey = props.getProperty("firebase.api.key") ?: ""
                if (projectId.isEmpty()) projectId = props.getProperty("firebase.project.id") ?: ""
                if (storageBucket.isEmpty()) storageBucket = props.getProperty("firebase.storage.bucket") ?: ""
            } catch (e: Exception) {
                println("Failed to read local.properties: ${e.message}")
            }
        }

        // Default fallbacks from google-services.json
        if (apiKey.isEmpty()) apiKey = "AIzaSyAfwmc5S8Nq-HiFWcg5GGHQgQT5b3KCKpI"
        if (projectId.isEmpty()) projectId = "tagspotter-d58b1"
        if (storageBucket.isEmpty()) storageBucket = "tagspotter-d58b1.firebasestorage.app"
    }

    fun hasCredentials(): Boolean {
        val file = findLocalProperties()
        var hasLocal = false
        if (file != null && file.exists()) {
            val props = Properties()
            try {
                file.inputStream().use { props.load(it) }
                hasLocal = props.containsKey("firebase.api.key") || props.containsKey("firebase.project.id")
            } catch (e: Exception) {}
        }
        val hasEnv = System.getenv("FIREBASE_API_KEY") != null || System.getenv("FIREBASE_PROJECT_ID") != null
        return hasLocal || hasEnv
    }
}

class JvmFirebaseClient(private val secureStorage: SecureStorage) {

    val jsonConfig = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    val authClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    val authenticatedClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val idToken = secureStorage.getString("firebase_id_token", "")
                    val refreshToken = secureStorage.getString("firebase_refresh_token", "")
                    if (idToken.isNotEmpty() && refreshToken.isNotEmpty()) {
                        BearerTokens(idToken, refreshToken)
                    } else {
                        null
                    }
                }
                refreshTokens {
                    val refreshToken = secureStorage.getString("firebase_refresh_token", "")
                    if (refreshToken.isNotEmpty()) {
                        refreshIdToken(refreshToken)
                    } else {
                        null
                    }
                }
            }
        }
    }

    private suspend fun refreshIdToken(refreshToken: String): BearerTokens? {
        return try {
            val response = authClient.post("https://securetoken.googleapis.com/v1/token?key=${JvmFirebaseConfig.apiKey}") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("grant_type=refresh_token&refresh_token=$refreshToken")
            }
            val responseText = response.bodyAsText()
            val json = Json.parseToJsonElement(responseText).jsonObject
            val newIdToken = json["id_token"]?.jsonPrimitive?.content
            val newRefreshToken = json["refresh_token"]?.jsonPrimitive?.content
            val userId = json["user_id"]?.jsonPrimitive?.content

            if (newIdToken != null && newRefreshToken != null) {
                secureStorage.putString("firebase_id_token", newIdToken)
                secureStorage.putString("firebase_refresh_token", newRefreshToken)
                if (userId != null) {
                    secureStorage.putString("firebase_user_uid", userId)
                }
                BearerTokens(newIdToken, newRefreshToken)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Silent token refresh failed: ${e.message}")
            null
        }
    }
}
