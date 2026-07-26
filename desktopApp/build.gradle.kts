plugins {
    kotlin("multiplatform")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    
    sourceSets {
        val commonMain = getByName("commonMain") {
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
                
                implementation(libs.koin.core)
                implementation(libs.androidx.lifecycle.viewmodel)
            }
        }
        val jvmMain = getByName("jvmMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
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
