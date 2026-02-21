import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.suvojeet.suvmusic.lastfm"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProperties.load(it) }
        }

        val lastFmApiKey = System.getenv("LAST_FM_API_KEY") ?: localProperties.getProperty("LAST_FM_API_KEY") ?: ""
        val lastFmSecret = System.getenv("LAST_FM_SHARED_SECRET") ?: localProperties.getProperty("LAST_FM_SHARED_SECRET") ?: ""

        buildConfigField("String", "LAST_FM_API_KEY", "\"$lastFmApiKey\"")
        buildConfigField("String", "LAST_FM_SHARED_SECRET", "\"$lastFmSecret\"")
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
