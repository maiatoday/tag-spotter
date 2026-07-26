import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
}

android {
    namespace = "net.maiatoday.tagspotter"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        applicationId = "net.maiatoday.tagspotter"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 10
        versionName = "0.0.10"
        testInstrumentationRunner = "net.maiatoday.tagspotter.TagSpotterTestRunner"

        // Load local.properties for developer-level Gemini API Key
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localProperties.load(stream)
            }
        }
        val apiKey = localProperties.getProperty("gemini.api.key") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
    testOptions {
        unitTests {
            all {
                it.useJUnitPlatform()
            }
        }
        suites {
            create("journeysTest") {
                assets {
                }
                targets {
                    create("default") {
                    }
                }
                useJunitEngine {
                    inputs += listOf(com.android.build.api.dsl.AgpTestSuiteInputParameters.TESTED_APKS)
                    includeEngines += listOf("journeys-test-engine")
                    enginesDependencies(libs.junit.platform.launcher)
                    enginesDependencies(libs.junit.platform.engine)
                    enginesDependencies(libs.journeys.junit.engine)
                }
                targetVariants += listOf("debug")
            }
        }
    }
}


dependencies {
  // Modules
  implementation(project(":core:model"))
  implementation(project(":core:photo"))
  implementation(project(":core:location"))
  implementation(project(":core:database"))
  implementation(project(":core:sync"))
  implementation(project(":core:settings"))
  implementation(project(":core:ai"))
  implementation(project(":core:ui"))
  implementation(project(":feature:gallery"))
  implementation(project(":feature:map"))
  implementation(project(":feature:detail"))
  implementation(project(":feature:settings"))
  implementation(project(":feature:main"))

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.vertexai)
  implementation(libs.firebase.appcheck)
  implementation(libs.firebase.appcheck.debug)

  // Koin DI
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.core)
  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)
  implementation(libs.koin.compose.viewmodel)

  // osm map tiles user agent requirements config helper (osmdroid is used in application init)
  implementation(libs.osmdroid.android)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.play.services.wearable)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  implementation(libs.sqlite.bundled)

  // Google Credential Manager for Native Google Sign-In
  implementation("androidx.credentials:credentials:1.5.0-rc01")
  implementation("androidx.credentials:credentials-play-services-auth:1.5.0-rc01")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}


