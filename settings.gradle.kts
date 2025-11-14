pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()


        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }


    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://artifactory.appodeal.com/appodeal-public/") }

        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
    }
}

rootProject.name = "LetsLink"
include(":app")
