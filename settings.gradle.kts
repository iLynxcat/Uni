rootProject.name = "uni"

include("plugins:UniCore")

include("libs:utils")
include("unisrc-api", "unisrc-server")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "placeholderapi"
        }
        maven("https://jitpack.io") {
            name = "jitpack"
        }
    }
}
