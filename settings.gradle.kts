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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add flatDir if you are referencing AARs directly in libs (Needed for the SDK AAR)
        flatDir {
//            dirs("libs") // If your SDK is in the top-level libs
            dirs("app/libs") // If your SDK is in the app/libs
        }
    }
}

rootProject.name = "HawcxDemoApp"
include(":app")
