plugins {
    kotlin("multiplatform")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

    wasmJs {
        // moduleName removed; webpack config defines output
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(project(":feature:main"))
                implementation(project(":core:database"))
                implementation(project(":core:sync"))
                implementation(project(":core:settings"))
                implementation(project(":core:location"))
                implementation(project(":core:photo"))
                implementation(project(":core:ai"))
                implementation(project(":core:ui"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

                implementation(libs.koin.core)
            }
        }
    }
}

tasks.register("wasmJsBrowserRun") {
    dependsOn("wasmJsBrowserDevelopmentRun")
}
