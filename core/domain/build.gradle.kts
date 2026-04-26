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
        val androidMain by getting
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
