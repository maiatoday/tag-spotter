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
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        
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
                // Pure Compose Desktop (No JavaFX or WebKit dependencies)
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
