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
rootProject.name = "TapSave"
include(":app")

// CI-only semantic UI fixture; never packaged in Tap Save.
include(":instagram-fixture")
// Explicit ADB diagnostic utility, separate package and never a shipping dependency.
include(":device-probe")
