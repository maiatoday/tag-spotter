plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "net.maiatoday.tagspotter.core.database"
        compileSdk = 37
        minSdk = 29
        
        withHostTestBuilder { }
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
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain.get())
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain.get())
        }
        
        wasmJsMain {
            dependencies {
                // No Room dependencies here
            }
        }
        
        val nonWebTest by creating {
            dependsOn(commonTest.get())
        }
        
        val androidHostTest by getting {
            dependsOn(nonWebTest)
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
                implementation(libs.junit.jupiter.api)
                implementation("org.robolectric:robolectric:4.12.2")
            }
        }
        
        val jvmTest by getting {
            dependsOn(nonWebTest)
        }
        
        val iosTest by creating {
            dependsOn(nonWebTest)
        }
        
        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }
        
        val iosArm64Test by getting {
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
