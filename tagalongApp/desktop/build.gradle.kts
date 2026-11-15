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
                implementation(project(":feature:tagalong"))
                implementation(project(":core:a2ui"))
                implementation(project(":core:agent"))
                implementation(project(":core:database"))
                implementation(project(":core:location"))
                implementation(project(":core:photo"))
                implementation(project(":core:settings"))
                implementation(project(":core:sync"))
                implementation(project(":core:ui"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

                implementation(libs.koin.core)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.google.adk.kotlin.core)
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
        mainClass = "net.maiatoday.tagalong.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "TagAlong"
            packageVersion = "1.0.0"
        }
    }
}
