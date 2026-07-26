plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "net.maiatoday.tagspotter.core.database"
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
        commonMain {
            dependencies {
                implementation(project(":core:model"))
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
        
        val nonWebMain = create("nonWebMain") {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(project(":core:photo"))
                implementation(project(":core:location"))
                implementation("com.squareup.okio:okio:3.9.0")
                
                // Room
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
        
        androidMain {
            dependsOn(nonWebMain)
            dependencies {
                implementation(libs.androidx.core.ktx)
                // WorkManager (needed by PackManager / Workers)
                implementation(libs.androidx.work.runtime.ktx)
                // Koin
                implementation(libs.koin.android)
            }
        }
        
        jvmMain {
            dependsOn(nonWebMain)
        }
        
        iosMain {
            dependsOn(nonWebMain)
        }
        
        getByName("iosSimulatorArm64Main") {
            dependsOn(iosMain.get())
        }
        
        getByName("iosArm64Main") {
            dependsOn(iosMain.get())
        }
        wasmJsMain {
            dependencies {
                // No Room dependencies here
            }
        }
        
        val nonWebTest = create("nonWebTest") {
            dependsOn(getByName("commonTest"))
        }
        
        getByName("androidHostTest") {
            dependsOn(nonWebTest)
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
                implementation(libs.junit.jupiter.api)
                implementation("org.robolectric:robolectric:4.12.2")
            }
        }
        
        getByName("jvmTest") {
            dependsOn(nonWebTest)
        }
        
        val iosTest = create("iosTest") {
            dependsOn(nonWebTest)
        }
        
        getByName("iosSimulatorArm64Test") {
            dependsOn(iosTest)
        }
        
        getByName("iosArm64Test") {
            dependsOn(iosTest)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    // No KSP Room compiler for wasmJs
}
