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
        // androidMain + desktopMain land here once Phase 4.0 introduces
        // the first expect/actual platform code (MusicPlayer scaffold).
        // Empty for now — the KMP plugin auto-creates them and they
        // inherit commonMain dependencies.
        val androidMain by getting
        val desktopMain by getting
    }
}
