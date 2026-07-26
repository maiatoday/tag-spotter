plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.feature.main"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        
        androidResources {
            enable = true
        }
    }
    
    jvm()
    
    listOf(
        iosSimulatorArm64(),
        iosArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedApp"
            isStatic = true
            export(project(":core:database"))
            export(project(":core:model"))
            export(project(":core:ai"))
        }
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain {
            dependencies {
                api(project(":core:model"))
                api(project(":core:database"))
                implementation(project(":core:settings"))
                implementation(project(":core:location"))
                implementation(project(":core:photo"))
                api(project(":core:ai"))
                implementation(project(":core:ui"))
                implementation(project(":core:sync"))
                implementation(project(":feature:gallery"))
                implementation(project(":feature:map"))
                implementation(project(":feature:detail"))
                implementation(project(":feature:settings"))
                
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                
                implementation(libs.androidx.lifecycle.runtime.compose)
                
                // Navigation 3 runtime and UI
                implementation(libs.androidx.navigation3.runtime)
                implementation(libs.androidx.navigation3.ui)
                implementation(libs.androidx.lifecycle.viewmodel.navigation3)
                
                // Koin
                implementation(libs.koin.core)
                implementation("io.insert-koin:koin-compose-viewmodel:4.2.1")

                // ImagePickerKMP
                implementation(libs.imagepickerkmp)
            }
        }
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        val nonAndroidMain = create("nonAndroidMain") {
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

        getByName("iosSimulatorArm64Main") {
            dependsOn(iosMain.get())
        }

        getByName("iosArm64Main") {
            dependsOn(iosMain.get())
        }
        
        wasmJsMain {
            dependsOn(nonAndroidMain)
        }
    }
}
