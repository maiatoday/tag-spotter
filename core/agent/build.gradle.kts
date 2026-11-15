plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.core.agent"
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
                implementation(project(":core:a2ui"))
                implementation(libs.google.adk.kotlin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.core)
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
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.ai)
                implementation(libs.google.adk.kotlin.firebase.android)
            }
        }
        jvmMain {
            dependencies {
                implementation("com.google.genai:google-genai:1.44.0")
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.google.adk.kotlin.processor)
    add("kspJvm", libs.google.adk.kotlin.processor)
}
