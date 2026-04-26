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
                implementation(project(":core:model"))
                // :core:domain provides the MusicPlayer expect class
                // (Phase 4) — backed by VLCJ on Desktop, by a stub on
                // Android until Phase 4.2 wires the existing Media3
                // player. Brings in kotlinx-coroutines transitively.
                implementation(project(":core:domain"))
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
                // NewPipe Extractor for YouTube search + stream URL
                // resolution. Pure JVM library — works on Desktop without
                // any Android-specific bits. Brings in jsoup + Mozilla
                // Rhino transitively (~5 MB extra in the MSI).
                implementation(libs.newpipe.extractor)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.suvojeet.suvmusic.composeapp.MainKt"

        // ProGuard disabled for the desktop release. Reasons:
        // 1. jsoup optionally references com.google.re2j (not on classpath)
        //    — ProGuard treats this as a fatal unresolved-reference error.
        // 2. VLCJ's JNA bindings, Mozilla Rhino's JS interop, and NewPipe
        //    Extractor all rely on runtime reflection that ProGuard would
        //    need extensive -keep rules to preserve. Wrong rule = silent
        //    runtime crash deep inside playback or extraction.
        // 3. The unminified MSI is ~80 MB vs ~50 MB minified — acceptable
        //    cost given the alternative is hours of ProGuard rule tuning
        //    for every transitive dep.
        // Re-enable in a future chunk if MSI size becomes a real problem;
        // would need a curated -dontwarn + -keep rules file.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

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
