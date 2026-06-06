plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "net.maiatoday.tagspotter.core.testing"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:database"))
    api(project(":core:settings"))
    
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.core.ktx)
}
