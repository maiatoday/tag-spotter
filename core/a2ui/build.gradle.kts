plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.core.a2ui"
        compileSdk {
            version = release(libs.versions.androidCompileSdk.get().toInt()) {
                minorApiLevel = libs.versions.androidCompileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest { }
    }
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:ui"))
                api(libs.a2ui.get4k())
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.coil.compose)
                api(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.a2ui.model)
                implementation(libs.androidx.a2ui.engine)
                implementation(libs.androidx.a2ui.compose.runtime)
                implementation(libs.androidx.a2ui.compose.ui)
                implementation(libs.androidx.compose.material3.a2ui)
            }
        }
        jvmMain {
            dependencies {
            }
        }
    }
}
