plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "net.maiatoday.tagspotter.feature.map"
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
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:database"))
                implementation(project(":core:settings"))
                implementation(project(":core:ui"))
                implementation(project(":core:photo"))
                implementation(project(":feature:gallery"))
                
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                implementation(libs.androidx.lifecycle.runtime.compose)
                
                // Coil (Image Loading)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)
                
                // Koin
                implementation(libs.koin.core)
                implementation("io.insert-koin:koin-compose-viewmodel:4.2.1")
            }
        }
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }
        
        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        
        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.koin.android)
            }
        }
        
        jvmMain {
            dependsOn(nonAndroidMain)
        }
        
        iosMain {
            dependsOn(nonAndroidMain)
        }
        

        
        wasmJsMain {
            dependsOn(nonAndroidMain)
        }
        
        val nonAndroidTest by creating {
            dependsOn(commonTest.get())
        }
        
        val androidHostTest by getting {
            dependsOn(nonAndroidTest)
        }
        
        val jvmTest by getting {
            dependsOn(nonAndroidTest)
        }
        
        iosTest {
            dependsOn(nonAndroidTest)
        }
    }
}
