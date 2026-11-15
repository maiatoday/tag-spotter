plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.feature.tagalong"
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
                implementation(project(":core:database"))
                implementation(project(":core:sync"))
                implementation(project(":core:location"))
                implementation(project(":core:photo"))
                implementation(project(":core:ui"))
                implementation(project(":core:a2ui"))
                implementation(project(":core:agent"))
                implementation(libs.google.adk.kotlin.core)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)

                implementation(libs.koin.core)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
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
            }
        }
        jvmMain {
            dependencies {
            }
        }
    }
}
