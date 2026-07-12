@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "net.maiatoday.tagspotter.core.ai"
        compileSdk = 37
        minSdk = 29
        withHostTest { }
    }
    
    jvm()
    
    iosArm64()
    iosSimulatorArm64()
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:photo"))
            implementation(libs.kotlinx.serialization.json)
            
            // Ktor Client for API requests
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.vertexai)
            implementation(libs.firebase.appcheck)
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        
        iosMain {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.3")
            }
        }
        
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:3.0.3")
            }
        }

        

        val androidHostTest by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }
    }
}
