plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = "net.maiatoday.tagspotter.core.settings"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        
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
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlinx.coroutines.core)
                
                // Koin
                implementation(libs.koin.core)
            }
        }
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.multiplatform.settings.test)
            }
        }
        
        val nonWebMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.datastore.preferences)
            }
        }
        
        androidMain {
            dependsOn(nonWebMain)
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.security.crypto)
                
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
        

        
        val nonWebTest by creating {
            dependsOn(commonTest.get())
        }
        
        val androidHostTest by getting {
            dependsOn(nonWebTest)
            dependencies {
                implementation(libs.androidx.test.core)
            }
        }
        
        val jvmTest by getting {
            dependsOn(nonWebTest)
        }
        
        iosTest {
            dependsOn(nonWebTest)
        }
    }
}
