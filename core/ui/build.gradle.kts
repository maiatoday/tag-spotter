plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "net.maiatoday.tagspotter.core.ui"
        compileSdk = 37
        minSdk = 29
        
        androidResources {
            enable = true
        }
        withHostTest { }
    }
    jvm()
    iosSimulatorArm64()
    iosArm64()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core:model"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                // Coil
                implementation(libs.coil.compose)
                
                // Lifecycle
                implementation(libs.androidx.lifecycle.runtime.compose)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        
        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.compose.ui.text.google.fonts)
                
                // OpenStreetMap
                implementation(libs.osmdroid.android)
            }
        }
        
        jvmMain {
            dependsOn(nonAndroidMain)
            dependencies {
                val osName = System.getProperty("os.name")
                val osArch = System.getProperty("os.arch")
                val javaFxPlatform = when {
                    osName.contains("Windows", ignoreCase = true) -> "win"
                    osName.contains("Mac", ignoreCase = true) -> if (osArch == "aarch64") "mac-aarch64" else "mac"
                    osName.contains("Linux", ignoreCase = true) -> if (osArch == "aarch64") "linux-aarch64" else "linux"
                    else -> "mac-aarch64"
                }
                
                implementation("org.openjfx:javafx-base:21.0.1:$javaFxPlatform")
                implementation("org.openjfx:javafx-graphics:21.0.1:$javaFxPlatform")
                implementation("org.openjfx:javafx-controls:21.0.1:$javaFxPlatform")
                implementation("org.openjfx:javafx-swing:21.0.1:$javaFxPlatform")
                implementation("org.openjfx:javafx-web:21.0.1:$javaFxPlatform")
                implementation("org.openjfx:javafx-media:21.0.1:$javaFxPlatform")
            }
        }
        
        iosMain {
            dependsOn(nonAndroidMain)
        }
        

        
        wasmJsMain {
            dependsOn(nonAndroidMain)
        }
    }
}
