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
        // commonMain stays free of Android and Compose deps. kotlinx-datetime
        // is the one allowed addition — it gives us `Clock.System.now()` so
        // data classes can pick up timestamps in default parameters without
        // reaching for the JVM-only `System.currentTimeMillis()`.
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
            }
        }

        // androidMain currently only holds the `Song.fromLocal(Uri, Uri)`
        // convenience factory, which still references android.net.Uri.
        // Everything else has migrated to commonMain.
        val androidMain by getting {
            // No deps for now.
        }
    }
}
