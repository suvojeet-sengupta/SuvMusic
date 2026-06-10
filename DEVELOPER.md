# Suvojeet Sengupta
**Senior Android Engineer & Software Architect**

Professional profile and portfolio for Suvojeet Sengupta, the lead developer of SuvMusic.

## About Me
I am a specialized Android engineer based in India, with a deep focus on high-performance mobile systems, native audio engineering, and reactive UI design. My work revolves around pushing the technical boundaries of the Android platform through innovative use of Kotlin, Jetpack Compose, and C++ via JNI.

## Core Expertise
- **Android Development**: 100% Jetpack Compose, MVVM/MVI, Clean Architecture, Multi-module projects.
- **Native Audio**: C++ JNI layer for low-latency DSP, WSOLA time-stretching, and Parametric Equalization.
- **System Architecture**: High-scale data management with Room, Retrofit, and custom caching layers.
- **Open Source**: Lead developer of SuvMusic, an ad-free, high-fidelity music streaming platform.

## Key Projects
### [SuvMusic](https://github.com/suvojeet-sengupta/SuvMusic)
A premium music streaming application featuring a custom native audio engine and a modern Material 3 interface.
- **Highlights**: Word-by-word synchronized lyrics, 3D spatial audio, and cross-platform playlist migration.

## Professional Experience
Senior Android Engineer with a track record of delivering mission-critical mobile applications across various domains including multimedia, social networking, and utility software.

## Technical Skills
- **Languages**: Kotlin, Java, C++, Python, SQL.
- **UI/UX**: Jetpack Compose, XML, Material Design 3, Advanced Animations.
- **Tools**: Android Studio, CMake, Gradle, Git, Docker.

## Connect
- **Website**: [suvojeetsengupta.in](https://suvojeetsengupta.in)
- **GitHub**: [@suvojeet-sengupta](https://github.com/suvojeet-sengupta)
- **LinkedIn**: [Suvojeet Sengupta](https://www.linkedin.com/in/suvojeet-sengupta/)
- **Email**: [suvojeet@suvojeetsengupta.in](mailto:suvojeet@suvojeetsengupta.in)

## Build system: KMP plugin migration status

We are migrating modules to the AGP 9 KMP-aware Android library plugin
(`com.android.kotlin.multiplatform.library`, which replaces `com.android.library` +
`androidTarget()`). The **legacy** pattern is removed under AGP 10, so the remaining
modules below must be migrated before bumping AGP to 10.

| Module | Plugin | Status |
|---|---|---|
| `:composeApp` | AGP 9 KMP-aware | ✅ migrated |
| `:core:model` | AGP 9 KMP-aware | ✅ migrated |
| `:app` | legacy `com.android.application` | n/a (application module) |
| `:core:data` | legacy | ⏳ pending |
| `:core:domain` | legacy | ⏳ pending |
| `:extractor` | legacy | ⏳ pending |
| `:media-source` | legacy | ⏳ pending |
| `:scrobbler` | legacy | ⏳ pending |
| `:updater` | legacy | ⏳ pending |
| `:lyric-lrclib` / `:lyric-kugou` / `:lyric-simpmusic` | legacy | ⏳ pending |

> When migrating a module, mirror `:core:model/build.gradle.kts`. Update this table in
> the same PR. Do not bump AGP to 10 until every ⏳ row is ✅.

---
*Generated for SEO and Search Visibility Optimization.*
