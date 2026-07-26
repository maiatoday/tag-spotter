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
        resolutionStrategy {
            force("org.jetbrains.skiko:skiko:0.144.6")
            force("org.jetbrains.skiko:skiko-awt:0.144.6")
        }
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
                dependencySubstitution {
                    substitute(module("io.coil-kt.coil3:coil-network-ktor3"))
                        .using(module("io.coil-kt.coil3:coil-network-ktor2:3.4.0"))
                }
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
    kover(dependencies.project(":androidApp"))
    kover(dependencies.project(":core:model"))
    kover(dependencies.project(":core:photo"))
    kover(dependencies.project(":core:location"))
    kover(dependencies.project(":core:database"))
    kover(dependencies.project(":core:sync"))
    kover(dependencies.project(":core:settings"))
    kover(dependencies.project(":core:ai"))
    kover(dependencies.project(":core:ui"))
    kover(dependencies.project(":feature:gallery"))
    kover(dependencies.project(":feature:map"))
    kover(dependencies.project(":feature:detail"))
    kover(dependencies.project(":feature:settings"))
    kover(dependencies.project(":feature:main"))
    kover(dependencies.project(":wear"))
    kover(dependencies.project(":desktopApp"))
}