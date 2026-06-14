@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.core.ai"
        compileSdk = 37
        minSdk = 29
    }
    
    jvm()
    
    iosArm64()
    iosSimulatorArm64()
    
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
        
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.3")
            }
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:3.0.3")
            }
        }

        

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.okhttp)
        }
    }
}
