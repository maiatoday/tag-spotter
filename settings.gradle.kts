pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "TagSpotter"
include(":androidApp")
include(":core:model")
include(":core:photo")
include(":core:location")
include(":core:database")
include(":core:sync")
include(":core:settings")
include(":core:ai")
include(":core:ui")
include(":feature:gallery")
include(":feature:map")
include(":feature:detail")
include(":feature:settings")
include(":feature:main")
include(":wear")
include(":desktopApp")
include(":webApp")
