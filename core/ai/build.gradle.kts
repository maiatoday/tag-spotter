plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maiatoday.spotcache.core.ai"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:photo"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.google.generativeai)
    
    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    
    // Test Fixtures
    testFixturesImplementation(project(":core:model"))
}
