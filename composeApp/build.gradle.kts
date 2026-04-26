import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    // AGP 9+ KMP-aware Android library plugin (replaces com.android.library +
    // androidTarget(); see :core:model/build.gradle.kts for context).
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.suvojeet.suvmusic.composeapp"
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
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                // First KMP module dependency — proves the wiring end-to-end.
                // Only the commonMain leaf model classes (5 files) are usable
                // here; Song/Album/Artist/Playlist live in :core:model's
                // androidMain until chunk 2.3.
                implementation(project(":core:model"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.suvojeet.suvmusic.composeapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "SuvMusic"
            packageVersion = "1.0.0"
            description = "SuvMusic Desktop"
            vendor = "Suvojeet Sengupta"

            windows {
                menu = true
                shortcut = true
                // Stable UUID — keep this constant across releases so Windows treats
                // new MSIs as upgrades, not parallel installs. Generated once.
                upgradeUuid = "9F8D2C1A-4B7E-4F3D-9A6C-3E2B1A8D5C7F"
            }
        }
    }
}
