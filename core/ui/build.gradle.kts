plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "net.maiatoday.tagspotter.core.ui"
        compileSdk = 37
        minSdk = 29
        
        androidResources {
            enable = true
        }
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
        }
        
        iosMain {
            dependsOn(nonAndroidMain)
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain.get())
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain.get())
        }
        
        wasmJsMain {
            dependsOn(nonAndroidMain)
        }
    }
}
