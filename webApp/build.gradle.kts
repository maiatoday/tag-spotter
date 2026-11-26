import java.util.Properties
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

    wasmJs {
        // moduleName removed; webpack config defines output
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(project(":feature:main"))
                implementation(project(":core:database"))
                implementation(project(":core:sync"))
                implementation(project(":core:settings"))
                implementation(project(":core:location"))
                implementation(project(":core:photo"))
                implementation(project(":core:ai"))
                implementation(project(":core:ui"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

                implementation(libs.koin.core)
            }
        }
    }
}

tasks.named<ProcessResources>("wasmJsProcessResources") {
    val localProps = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    val projectId = localProps.getProperty("firebase.project.id")
        ?: System.getenv("FIREBASE_PROJECT_ID")
        ?: "tagspotter-mock-id"
    val apiKey = localProps.getProperty("firebase.web.api.key")
        ?: localProps.getProperty("firebase.api.key")
        ?: System.getenv("FIREBASE_API_KEY")
        ?: "AIzaSy_MockWebApiKeyForCompilation"
    val authDomain = localProps.getProperty("firebase.web.auth.domain")
        ?: "$projectId.firebaseapp.com"
    val storageBucket = localProps.getProperty("firebase.storage.bucket")
        ?: System.getenv("FIREBASE_STORAGE_BUCKET")
        ?: "$projectId.firebasestorage.app"
    val messagingSenderId = localProps.getProperty("firebase.web.messaging.sender.id")
        ?: "123456789012"
    val appId = localProps.getProperty("firebase.web.app.id")
        ?: "1:123456789012:web:46b766a08df2171476926e"

    inputs.property("firebaseProjectId", projectId)
    inputs.property("firebaseApiKey", apiKey)
    inputs.property("firebaseAuthDomain", authDomain)
    inputs.property("firebaseStorageBucket", storageBucket)
    inputs.property("firebaseMessagingSenderId", messagingSenderId)
    inputs.property("firebaseAppId", appId)

    filesMatching("index.html") {
        filter { line ->
            line.replace("__FIREBASE_API_KEY__", apiKey)
                .replace("__FIREBASE_AUTH_DOMAIN__", authDomain)
                .replace("__FIREBASE_PROJECT_ID__", projectId)
                .replace("__FIREBASE_STORAGE_BUCKET__", storageBucket)
                .replace("__FIREBASE_MESSAGING_SENDER_ID__", messagingSenderId)
                .replace("__FIREBASE_APP_ID__", appId)
        }
    }
}

tasks.register("wasmJsBrowserRun") {
    dependsOn("wasmJsBrowserDevelopmentRun")
}
