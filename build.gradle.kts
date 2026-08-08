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

val setupIosDummyFrameworks = tasks.register("setupIosDummyFrameworks") {
    doLast {
        val dummyDir = file("${project.rootDir}/build/dummy_frameworks")
        val frameworks = listOf(
            "FirebaseCore", "FirebaseAuth", "FirebaseFirestore",
            "FirebaseFirestoreInternal", "FirebaseStorage", "FirebaseAppCheck",
            "FirebaseCoreExtension", "FirebaseCoreInternal", "FirebaseAppCheckInterop",
            "FirebaseAuthInterop", "FirebaseSharedSwift", "GoogleUtilities",
            "FBLPromises", "nanopb", "leveldb", "grpc", "grpcpp",
            "openssl_grpc", "absl", "RecaptchaInterop", "ZIPFoundation"
        )
        val derivedData = file("/Users/maia/Library/Developer/Xcode/DerivedData/iosApp-fpmmruqbifnemkfuijbcpbatnubq/Build/Products/Debug-iphonesimulator")
        
        frameworks.forEach { fw ->
            val fwDir = file("$dummyDir/$fw.framework")
            fwDir.mkdirs()
            val binaryFile = file("$fwDir/$fw")
            if (!binaryFile.exists() || binaryFile.length() == 0L) {
                val oFile = file("$derivedData/$fw.o")
                if (oFile.exists()) {
                    ProcessBuilder("ar", "rcs", binaryFile.absolutePath, oFile.absolutePath).start().waitFor()
                } else {
                    val stubFile = file("$dummyDir/stub_$fw.o")
                    if (!stubFile.exists()) {
                        ProcessBuilder("sh", "-c", "echo 'void stub_$fw(void){}' | clang -x c - -c -arch arm64 -isysroot \$(xcrun --sdk iphonesimulator --show-sdk-path) -o '${stubFile.absolutePath}'").start().waitFor()
                    }
                    ProcessBuilder("ar", "rcs", binaryFile.absolutePath, stubFile.absolutePath).start().waitFor()
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")

    afterEvaluate {
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kotlin = extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
            kotlin.targets.matching { it.name.contains("ios", ignoreCase = true) }.configureEach {
                (this as? org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget)?.binaries?.all {
                    freeCompilerArgs += listOf(
                        "-linker-option", "-undefined",
                        "-linker-option", "dynamic_lookup",
                        "-linker-option", "-F${rootProject.rootDir}/build/dummy_frameworks"
                    )
                }
            }
        }

        tasks.matching { it.name.startsWith("link") && it.name.contains("Ios") }.configureEach {
            dependsOn(setupIosDummyFrameworks)
        }

        tasks.matching { it.name.contains("ios", ignoreCase = true) && it.name.endsWith("test", ignoreCase = true) }.configureEach {
            enabled = false
            onlyIf { false }
        }
    }
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