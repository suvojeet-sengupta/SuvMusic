plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
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
        // chunk 2.3 once Song.localUri is refactored to String.
        val androidMain by getting {
            dependencies {
                implementation(platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.runtime)
            }
        }
    }
}

android {
    namespace = "com.suvojeet.suvmusic.core.model"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
