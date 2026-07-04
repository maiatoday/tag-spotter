@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "net.maiatoday.tagspotter.core.sync"
        compileSdk = 37
        minSdk = 29
        
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
                implementation(project(":core:database"))
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
        
        val nonWebMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.firebase.auth)
                implementation(libs.firebase.firestore)
                implementation(libs.firebase.storage)
                implementation("com.squareup.okio:okio:3.9.0")
            }
        }
        
        androidMain {
            dependsOn(nonWebMain)
            dependencies {
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.androidx.core.ktx)
                implementation(libs.koin.android)
            }
        }
        
        jvmMain {
            dependsOn(nonWebMain)
        }
        
        iosMain {
            dependsOn(nonWebMain)
        }
        
        
        wasmJsMain {
            dependencies {
                // We use standard JS Firebase via index.html interop for Web/Wasm target
            }
        }
    }
}
