pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Tag Spotter"
include(":app")
include(":core:model")
include(":core:photo")
include(":core:location")
include(":core:database")
include(":core:settings")
include(":core:ai")
include(":core:ui")
include(":feature:gallery")
include(":feature:map")
include(":feature:detail")
include(":feature:settings")
