plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.core.model"
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
            implementation(libs.kotlinx.serialization.json)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
