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
rootProject.name = "JAGallery"
include(":app")
include(":core-data")
include(":core-ai")
include(":core-security")
include(":core-sync")
include(":core-ui")
include(":feature-album")
include(":feature-camera")
include(":feature-details")
include(":feature-duplicates")
include(":feature-editor")
include(":feature-search")
include(":feature-settings")
include(":feature-timeline")
include(":feature-vault")
include(":feature-viewer")
include(":feature-ai")
