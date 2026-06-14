plugins {
    kotlin("multiplatform")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":feature:main"))
                implementation(project(":core:database"))
                implementation(project(":core:settings"))
                implementation(project(":core:location"))
                implementation(project(":core:photo"))
                implementation(project(":core:ai"))
                implementation(project(":core:ui"))
                
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.desktop.currentOs)
                
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.androidx.lifecycle.viewmodel)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "net.maiatoday.tagspotter.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "TagSpotter"
            packageVersion = "1.0.0"
        }
    }
}
