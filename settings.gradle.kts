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

rootProject.name = "AI-Growth-OS"
include(":app")
include(":core:database")
include(":core:network")
include(":core:ai-engine")
include(":core:design")
include(":feature:learning")
include(":feature:growth")
include(":feature:creator")
include(":feature:accounting")