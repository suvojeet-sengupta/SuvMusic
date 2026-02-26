import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.suvojeet.suvmusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.suvojeet.suvmusic"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "1.3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load Last.fm keys from local.properties or environment
        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProperties.load(it) }
        }

        val lastFmApiKey = System.getenv("LAST_FM_API_KEY") 
            ?: localProperties.getProperty("LAST_FM_API_KEY") 
            ?: ""
        val lastFmSecret = System.getenv("LAST_FM_SHARED_SECRET") 
            ?: localProperties.getProperty("LAST_FM_SHARED_SECRET") 
            ?: ""

        buildConfigField("String", "LAST_FM_API_KEY", "\"$lastFmApiKey\"")
        buildConfigField("String", "LAST_FM_SHARED_SECRET", "\"$lastFmSecret\"")
    }

    signingConfigs {
        create("release") {
            // Read from environment variables (GitHub Actions secrets)
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("KEY_ALIAS")
            val keyPasswordValue = System.getenv("KEY_PASSWORD")
            
            if (keystorePath != null && keystorePassword != null && keyAliasValue != null && keyPasswordValue != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Use release signing config if available, otherwise use debug
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
    }
    
    compileOptions {
        // Enable desugaring for Java 8+ APIs on older Android versions
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "27.0.12077973" // Stable NDK 27
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    // Core Library Desugaring for Java 8+ APIs on older Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")
    
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    
    // Glance (Widgets)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Media3 (ExoPlayer)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui.compose)
    
    // Media Router
    implementation(libs.androidx.mediarouter)
    
    // NewPipe Extractor
    implementation(libs.newpipe.extractor)
    
    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    
    // Security
    implementation(libs.androidx.security.crypto)
    
    // Gson
    implementation(libs.gson)

    // Jsoup (HTML Parser)
    implementation(libs.jsoup)
    
    // Ktor (HTTP Client for lyrics providers)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.websockets)
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Tagging
    implementation(libs.jaudiotagger)
    
    // Protobuf for message serialization (lite version for Android)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(project(":providers"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":lastfm"))
    implementation(project(":simpmusic"))
    implementation(project(":lrclib"))
    implementation(project(":kugou"))
    implementation(project(":newpipe"))
    implementation(project(":core:model"))
}