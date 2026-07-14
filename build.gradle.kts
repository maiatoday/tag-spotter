// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.android.kotlin.multiplatform.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.jetbrains.compose) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.kover)
}

allprojects {
    configurations.all {
        if (name.contains("wasmJs", ignoreCase = true)) {
            resolutionStrategy {
                dependencySubstitution {
                    substitute(module("io.coil-kt.coil3:coil-network-ktor2"))
                        .using(module("io.coil-kt.coil3:coil-network-ktor3:3.4.0"))
                }
                eachDependency {
                    if (requested.group == "io.ktor") {
                        useVersion("3.0.3")
                    }
                }
            }
        } else {
            resolutionStrategy {
                eachDependency {
                    if (requested.group == "io.ktor") {
                        useVersion("2.3.12")
                    }
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    kover(project(":androidApp"))
    kover(project(":core:model"))
    kover(project(":core:photo"))
    kover(project(":core:location"))
    kover(project(":core:database"))
    kover(project(":core:sync"))
    kover(project(":core:settings"))
    kover(project(":core:ai"))
    kover(project(":core:ui"))
    kover(project(":feature:gallery"))
    kover(project(":feature:map"))
    kover(project(":feature:detail"))
    kover(project(":feature:settings"))
    kover(project(":feature:main"))
    kover(project(":wear"))
    kover(project(":desktopApp"))
}