plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    kotlin("multiplatform")
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "net.maiatoday.tagspotter.core.photo"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
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
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(libs.ashampoo.kim)
            implementation(libs.koin.core)
            implementation("com.squareup.okio:okio:3.9.0")
            implementation(libs.kotlinx.coroutines.core)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
            implementation(libs.androidx.exifinterface)
        }
        
        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        
        wasmJsMain {
            dependsOn(nonAndroidMain)
            dependencies {
                implementation(npm("pako", "2.1.0"))
            }
        }
    }
}


tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    failOnNoDiscoveredTests = false
}
