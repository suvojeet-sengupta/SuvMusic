plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9+ KMP-aware Android library plugin. Replaces the
    // `com.android.library` + `androidTarget()` combo which is deprecated and
    // broken in AGP 9.1 / Kotlin 2.3 (gives "Missing androidTarget()" hard
    // fail). https://kotl.in/gradle/agp-new-kmp
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.suvojeet.suvmusic.core.model"
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
        // commonMain stays pure Kotlin — no Android, no Compose annotations.
        // Currently holds Song-independent leaf model classes.
        val commonMain by getting {
            // No deps yet; pure Kotlin data classes only.
        }

        // androidMain holds model classes that still reference android.net.Uri
        // (Song + its consumers Album/Artist/Playlist). Migrated to common in
        // chunk 2.3 once Song.localUri is refactored to String. The @Immutable
        // Compose annotations were dropped in this same chunk — they were a
        // recomposition-skipping hint, not a correctness requirement, and
        // reintroducing them requires Compose Multiplatform's runtime artifact
        // which we'll add in Phase 5 when UI moves to commonMain.
        val androidMain by getting {
            // No deps for now.
        }
    }
}
