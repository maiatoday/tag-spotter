plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    kotlin("multiplatform")
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.core.photo"
        compileSdk = 37
        minSdk = 29
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
        
        jvmMain {
            dependsOn(commonMain.get())
        }
        
        val iosMain by creating {
            dependsOn(commonMain.get())
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
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
