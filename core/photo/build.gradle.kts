plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "net.maiatoday.spotcache.core.photo"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    
    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android) // because it uses androidContext()
    
    // Test Fixtures
    testFixturesImplementation(project(":core:model"))
}
