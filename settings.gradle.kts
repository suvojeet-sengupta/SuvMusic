pluginManagement {
    repositories {
        google()
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    }
}

rootProject.name = "SuvMusic"
include(":app")
include(":updater")
include(":media-source")
include(":scrobbler")
include(":lyric-simpmusic")
include(":lyric-lrclib")
include(":lyric-kugou")
include(":extractor")
include(":core:model")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:home")
include(":feature:player")
include(":feature:library")
include(":feature:search")
include(":feature:settings")
// KMP migration (Phase 0): parallel desktop/multiplatform module. Does not yet
// replace :app — Android APK still builds from :app unchanged.
include(":composeApp")