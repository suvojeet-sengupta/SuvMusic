plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.suvojeet.suvmusic.core.domain"
        compileSdk = 37
        minSdk = 26
        withHostTestBuilder { }
    }

    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:model"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val androidMain by getting {
            dependencies {
                // Media3 ExoPlayer — Phase 4.2 Android audio backend. The
                // shared MusicPlayer expect/actual delegates to ExoPlayer
                // here; the legacy rich :app player (spatial audio, audio
                // focus, MediaSession, BT autoplay) stays separate and is
                // not consumed through this surface. Common code only needs
                // the basic playback verbs.
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.common)
            }
        }
        val desktopMain by getting {
            dependencies {
                // VLCJ — Phase 4.1 Desktop audio backend. Uses LibVLC native
                // libraries that the user must have installed (typically via
                // `winget install VideoLAN.VLC` on Windows, or VLC.app on
                // macOS, or system package manager on Linux). LibVLC bundling
                // into the MSI is deferred — VLCJ's NativeDiscovery handles
                // typical install paths and we fall back to a logged warning
                // (no crash) when the lib isn't found.
                implementation(libs.vlcj)
            }
        }
    }
}
