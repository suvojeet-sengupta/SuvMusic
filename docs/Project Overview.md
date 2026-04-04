# Project Overview

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [MainActivity.kt](file://app/src/main/java/com/suvojeet/suvmusic/MainActivity.kt)
- [SuvMusicApplication.kt](file://app/src/main/java/com/suvojeet/suvmusic/SuvMusicApplication.kt)
- [app/build.gradle.kts](file://app/build.gradle.kts)
- [libs.versions.toml](file://gradle/libs.versions.toml)
- [CMakeLists.txt](file://app/src/main/cpp/CMakeLists.txt)
- [biquad.h](file://app/src/main/cpp/biquad.h)
- [limiter.h](file://app/src/main/cpp/limiter.h)
- [spatial_audio.cpp](file://app/src/main/cpp/spatial_audio.cpp)
- [pitch_shifter.h](file://app/src/main/cpp/pitch_shifter.h)
- [recommendation_scorer.cpp](file://app/src/main/cpp/recommendation_scorer.cpp)
- [MusicPlayer.kt](file://app/src/main/java/com/suvojeet/suvmusic/player/MusicPlayer.kt)
- [NativeSpatialAudio.kt](file://app/src/main/java/com/suvojeet/suvmusic/player/NativeSpatialAudio.kt)
- [SpatialAudioProcessor.kt](file://app/src/main/java/com/suvojeet/suvmusic/player/SpatialAudioProcessor.kt)
- [NavGraph.kt](file://app/src/main/java/com/suvojeet/suvmusic/navigation/NavGraph.kt)
- [Theme.kt](file://app/src/main/java/com/suvojeet/suvmusic/ui/theme/Theme.kt)
- [SessionManager.kt](file://app/src/main/java/com/suvojeet/suvmusic/data/SessionManager.kt)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
SuvMusic is a high-fidelity music streaming application for Android designed for high-resolution audio enthusiasts. It combines a modern, reactive UI built with Jetpack Compose and a custom native C++ audio engine to deliver professional-grade audio processing, bridging the gap between cloud streaming and local playback. The project emphasizes advanced audio engineering, immersive user experiences, and robust architecture patterns.

Key differentiators:
- Native audio engine with parametric EQ, spatial audio, limiter, and pitch-shifting
- Material 3 dynamic theming with album-art-based color adaptation
- Listen Together real-time synchronized playback
- Comprehensive lyrics integration and music haptics
- Persistent logging and crash reporting for diagnostics

**Section sources**
- [README.md:1-143](file://README.md#L1-L143)

## Project Structure
The project follows a modular Android architecture with clear separation between UI, data, domain, and native audio layers. The application is structured as a multi-module Gradle project with the main app module containing UI, repositories, services, and native C++ components.

```mermaid
graph TB
subgraph "App Module"
UI[Jetpack Compose UI]
Player[Music Player Layer]
Repositories[Data Repositories]
Services[Background Services]
Native[Native C++ Audio Engine]
end
subgraph "Core Modules"
CoreData[Core Data Layer]
CoreDomain[Core Domain Layer]
CoreModel[Core Model Layer]
end
subgraph "External Libraries"
Media3[Media3 ExoPlayer]
Hilt[Hilt DI]
Coil[Coil Image Loading]
Room[Room Database]
end
UI --> Player
Player --> Repositories
Player --> Services
Player --> Native
Repositories --> CoreData
CoreData --> CoreDomain
CoreDomain --> CoreModel
UI --> Hilt
Player --> Media3
UI --> Coil
Repositories --> Room
```

**Diagram sources**
- [app/build.gradle.kts:140-265](file://app/build.gradle.kts#L140-L265)
- [libs.versions.toml:39-162](file://gradle/libs.versions.toml#L39-L162)

**Section sources**
- [app/build.gradle.kts:14-110](file://app/build.gradle.kts#L14-L110)
- [libs.versions.toml:1-162](file://gradle/libs.versions.toml#L1-L162)

## Core Components
SuvMusic's core functionality revolves around three pillars: the native audio engine, the reactive UI layer, and the data management system.

### Native Audio Engine
The native audio engine provides professional-grade audio processing through carefully crafted C++ components:
- **Spatial Audio Processing**: Real-time 3D sound positioning with ITD/ILD head shadowing models
- **Parametric EQ**: 10-band ISO standard equalizer with adjustable bands
- **Limiter**: Hard limiter with lookahead processing for peak protection
- **Pitch Shifter**: High-quality pitch adjustment using dual delay-line technique
- **Crossfeed**: Headphone crossfeed simulation for speaker-like audio
- **Recommendation Scoring**: SIMD-accelerated recommendation engine using NEON/SSE

### UI Architecture
The UI layer implements a modern MVVM pattern with Jetpack Compose:
- **Material 3 Design System**: Dynamic theming with album-art-based color schemes
- **Responsive Layouts**: Adaptive designs for phones, tablets, and TV devices
- **State Management**: Reactive state flows with Hilt dependency injection
- **Navigation**: Type-safe navigation with destination-based routing

### Data Management
The data layer provides robust persistence and synchronization:
- **Room Database**: Local music library and user preferences storage
- **DataStore**: Encrypted preferences for sensitive user data
- **Repository Pattern**: Clean separation between local and remote data sources
- **Background Workers**: Efficient caching and synchronization tasks

**Section sources**
- [spatial_audio.cpp:16-475](file://app/src/main/cpp/spatial_audio.cpp#L16-L475)
- [biquad.h:17-125](file://app/src/main/cpp/biquad.h#L17-L125)
- [limiter.h:10-51](file://app/src/main/cpp/limiter.h#L10-L51)
- [pitch_shifter.h:14-109](file://app/src/main/cpp/pitch_shifter.h#L14-L109)
- [recommendation_scorer.cpp:1-503](file://app/src/main/cpp/recommendation_scorer.cpp#L1-L503)

## Architecture Overview
SuvMusic employs a Clean Architecture with MVVM pattern, implementing separation of concerns across multiple layers while maintaining high performance and user experience.

```mermaid
graph TB
subgraph "Presentation Layer"
UI[Jetpack Compose UI]
ViewModels[ViewModels]
Navigation[Navigation Graph]
end
subgraph "Domain Layer"
UseCases[Use Cases]
Repositories[Repository Interfaces]
end
subgraph "Data Layer"
LocalRepo[Local Repository]
RemoteRepo[Remote Repository]
Database[Room Database]
DataStore[Encrypted Preferences]
end
subgraph "Native Layer"
AudioEngine[Native Audio Engine]
DSP[Digital Signal Processing]
JNI[JNI Bridge]
end
UI --> ViewModels
ViewModels --> UseCases
UseCases --> Repositories
Repositories --> LocalRepo
Repositories --> RemoteRepo
LocalRepo --> Database
LocalRepo --> DataStore
RemoteRepo --> AudioEngine
AudioEngine --> DSP
DSP --> JNI
JNI --> AudioEngine
```

**Diagram sources**
- [MainActivity.kt:96-340](file://app/src/main/java/com/suvojeet/suvmusic/MainActivity.kt#L96-L340)
- [NavGraph.kt:54-692](file://app/src/main/java/com/suvojeet/suvmusic/navigation/NavGraph.kt#L54-L692)
- [MusicPlayer.kt:58-198](file://app/src/main/java/com/suvojeet/suvmusic/player/MusicPlayer.kt#L58-L198)

The architecture ensures:
- **Separation of Concerns**: Clear boundaries between UI, business logic, and data layers
- **Testability**: Dependency injection enables easy mocking and testing
- **Maintainability**: Modular structure supports team collaboration and feature development
- **Performance**: Native audio processing minimizes CPU overhead and latency

**Section sources**
- [MainActivity.kt:96-340](file://app/src/main/java/com/suvojeet/suvmusic/MainActivity.kt#L96-L340)
- [NavGraph.kt:54-692](file://app/src/main/java/com/suvojeet/suvmusic/navigation/NavGraph.kt#L54-L692)
- [Theme.kt:209-306](file://app/src/main/java/com/suvojeet/suvmusic/ui/theme/Theme.kt#L209-L306)

## Detailed Component Analysis

### Native Audio Engine Architecture
The native audio engine forms the technical backbone of SuvMusic's high-fidelity audio capabilities.

```mermaid
classDiagram
class NativeSpatialAudio {
-boolean isLibraryLoaded
+processPcm16(buffer, frames, channels, rate, azimuth, elevation)
+setSpatializerEnabled(enabled)
+setLimiterParams(threshold, ratio, attack, release, makeup)
+setEqBand(band, gain)
+setPlaybackParams(pitch)
+extractWaveform(path, points) FloatArray?
}
class SpatialAudioProcessor {
-ByteBuffer directNativeBuffer
-float azimuth
-float elevation
-boolean isSpatialEnabled
-boolean isLimiterEnabled
+setSpatialEnabled(enabled)
+setEqEnabled(enabled)
+setLimiterConfig(boost, amount, norm)
+setPlaybackParams(pitch)
+queueInput(buffer)
}
class Spatializer {
-vector<float> leftDelayBuffer
-vector<float> rightDelayBuffer
-int writeIndex
-float headRadius
-float speedOfSound
+process(buffer, frames, channels, azimuth, elevation, rate)
+setEnabled(enabled)
}
class ParametricEQ {
-vector<Biquad> filters
-atomic<bool> enabled
-atomic<float> preampGain
+setBandGain(band, gain)
+setPreamp(gain)
+setEnabled(enabled)
+process(buffer, frames, channels, rate)
}
class Limiter {
-atomic<bool> enabled
-float threshold
-float ratio
-float attackCoeff
-float releaseCoeff
-vector<float> delayBuffer
+process(buffer, frames, channels, rate)
+setParams(threshold, ratio, attack, release, makeup)
}
NativeSpatialAudio --> SpatialAudioProcessor : "controls"
SpatialAudioProcessor --> Spatializer : "uses"
SpatialAudioProcessor --> ParametricEQ : "uses"
SpatialAudioProcessor --> Limiter : "uses"
SpatialAudioProcessor --> PitchShifter : "uses"
```

**Diagram sources**
- [NativeSpatialAudio.kt:9-158](file://app/src/main/java/com/suvojeet/suvmusic/player/NativeSpatialAudio.kt#L9-L158)
- [SpatialAudioProcessor.kt:13-243](file://app/src/main/java/com/suvojeet/suvmusic/player/SpatialAudioProcessor.kt#L13-L243)
- [spatial_audio.cpp:16-104](file://app/src/main/cpp/spatial_audio.cpp#L16-L104)
- [biquad.h:17-125](file://app/src/main/cpp/biquad.h#L17-L125)
- [limiter.h:10-51](file://app/src/main/cpp/limiter.h#L10-L51)

### Audio Processing Pipeline
The audio processing pipeline demonstrates the sophisticated signal chain used for high-fidelity playback.

```mermaid
sequenceDiagram
participant App as "Android App"
participant Player as "MusicPlayer"
participant Processor as "SpatialAudioProcessor"
participant Native as "NativeSpatialAudio"
participant DSP as "DSP Engine"
participant Device as "Audio Device"
App->>Player : Play Request
Player->>Processor : Configure AudioFormat
Processor->>Processor : Set Effects State
Player->>Processor : queueInput(buffer)
Processor->>Processor : Convert PCM Format
Processor->>Native : processPcm16(buffer, params)
Native->>DSP : Apply Crossfeed
DSP->>DSP : Apply EQ Processing
DSP->>DSP : Apply Bass Boost
DSP->>DSP : Apply Virtualizer
DSP->>DSP : Apply Pitch Shifting
DSP->>DSP : Apply Spatial Positioning
DSP->>DSP : Apply Limiter
DSP-->>Native : Processed Buffer
Native-->>Processor : Processed Buffer
Processor-->>Player : Output Buffer
Player-->>Device : Render Audio
```

**Diagram sources**
- [MusicPlayer.kt:76-198](file://app/src/main/java/com/suvojeet/suvmusic/player/MusicPlayer.kt#L76-L198)
- [SpatialAudioProcessor.kt:113-242](file://app/src/main/java/com/suvojeet/suvmusic/player/SpatialAudioProcessor.kt#L113-L242)
- [spatial_audio.cpp:347-475](file://app/src/main/cpp/spatial_audio.cpp#L347-L475)

### Recommendation Engine
The native recommendation engine leverages SIMD acceleration for efficient music discovery.

```mermaid
flowchart TD
Start([Recommendation Request]) --> LoadFeatures["Load User Features"]
LoadFeatures --> ValidateInput["Validate Input Data"]
ValidateInput --> CheckSIMD{"SIMD Available?"}
CheckSIMD --> |Yes| Vectorized["NEON/SSE Vectorized Processing"]
CheckSIMD --> |No| Scalar["Scalar Processing"]
Vectorized --> ScoreCandidates["Score Candidate Songs"]
Scalar --> ScoreCandidates
ScoreCandidates --> CalculateWeights["Apply Weighted Scoring"]
CalculateWeights --> Similarity["Compute Genre Similarity"]
Similarity --> TopK["Select Top-K Candidates"]
TopK --> ReturnResults["Return Ranked Results"]
ReturnResults --> End([Processing Complete])
```

**Diagram sources**
- [recommendation_scorer.cpp:166-322](file://app/src/main/cpp/recommendation_scorer.cpp#L166-L322)
- [recommendation_scorer.cpp:328-344](file://app/src/main/cpp/recommendation_scorer.cpp#L328-L344)

**Section sources**
- [NativeSpatialAudio.kt:9-158](file://app/src/main/java/com/suvojeet/suvmusic/player/NativeSpatialAudio.kt#L9-L158)
- [SpatialAudioProcessor.kt:13-243](file://app/src/main/java/com/suvojeet/suvmusic/player/SpatialAudioProcessor.kt#L13-L243)
- [spatial_audio.cpp:16-475](file://app/src/main/cpp/spatial_audio.cpp#L16-L475)
- [recommendation_scorer.cpp:1-503](file://app/src/main/cpp/recommendation_scorer.cpp#L1-L503)

### UI and Theming System
SuvMusic implements a sophisticated theming system that adapts to user preferences and album artwork.

```mermaid
classDiagram
class SuvMusicTheme {
+boolean darkTheme
+boolean dynamicColor
+boolean pureBlack
+AppTheme appTheme
+DominantColors albumArtColors
+MaterialExpressiveTheme()
}
class ThemeManager {
+createColorSchemeFromDominantColors(colors, dark)
+getDynamicColorScheme(context, dark)
+applyPureBlackTheme(colorScheme)
}
class DominantColors {
+Color primary
+Color secondary
+Color accent
+Color onBackground
}
SuvMusicTheme --> ThemeManager : "uses"
ThemeManager --> DominantColors : "creates"
SuvMusicTheme --> DominantColors : "applies"
```

**Diagram sources**
- [Theme.kt:209-306](file://app/src/main/java/com/suvojeet/suvmusic/ui/theme/Theme.kt#L209-L306)
- [Theme.kt:155-205](file://app/src/main/java/com/suvojeet/suvmusic/ui/theme/Theme.kt#L155-L205)

**Section sources**
- [Theme.kt:209-306](file://app/src/main/java/com/suvojeet/suvmusic/ui/theme/Theme.kt#L209-L306)
- [SessionManager.kt:343-354](file://app/src/main/java/com/suvojeet/suvmusic/data/SessionManager.kt#L343-L354)

## Dependency Analysis
The project maintains clean dependency relationships through dependency injection and modular architecture.

```mermaid
graph TB
subgraph "Application Dependencies"
Hilt[Dagger Hilt]
Media3[Media3 ExoPlayer]
Coil[Coil Image Loader]
Room[Room Database]
DataStore[DataStore Preferences]
end
subgraph "Audio Dependencies"
OpenSLES[OpenSL ES]
NDK[Android NDK]
SIMD[NEON/SSE]
end
subgraph "External Services"
YouTube[YouTube API]
LastFM[Last.fm API]
SponsorBlock[SponsorBlock API]
end
Hilt --> Media3
Hilt --> Coil
Hilt --> Room
Hilt --> DataStore
Media3 --> OpenSLES
Native --> NDK
NDK --> SIMD
Repositories --> YouTube
Repositories --> LastFM
Repositories --> SponsorBlock
```

**Diagram sources**
- [app/build.gradle.kts:140-265](file://app/build.gradle.kts#L140-L265)
- [libs.versions.toml:39-162](file://gradle/libs.versions.toml#L39-L162)

**Section sources**
- [app/build.gradle.kts:140-265](file://app/build.gradle.kts#L140-L265)
- [libs.versions.toml:39-162](file://gradle/libs.versions.toml#L39-L162)

## Performance Considerations
SuvMusic implements several performance optimizations to ensure smooth operation across diverse Android devices:

### Native Performance Optimizations
- **SIMD Acceleration**: NEON and SSE instructions for vectorized processing
- **Memory Management**: Carefully managed buffers to minimize allocations
- **Efficient Data Structures**: Optimized for audio processing throughput
- **Multi-threading**: Background processing for heavy computations

### UI Performance
- **Jetpack Compose Optimization**: Efficient recomposition and state management
- **Lazy Loading**: Images and lists use lazy evaluation
- **Adaptive Rendering**: Dynamic refresh rates and GPU optimization
- **Memory Efficiency**: Proper lifecycle management and resource cleanup

### Audio Processing Efficiency
- **Lookahead Processing**: Minimizes latency in audio effects
- **Buffer Management**: Optimized buffer sizes for different scenarios
- **Selective Processing**: Effects applied only when active
- **Hardware Acceleration**: Leverages device-specific optimizations

## Troubleshooting Guide
Common issues and their solutions:

### Audio Quality Issues
- **Static or Distorted Audio**: Check limiter settings and ensure proper gain staging
- **Latency Problems**: Verify lookahead buffer settings and device compatibility
- **EQ Not Responding**: Confirm equalizer is enabled and bands are properly configured

### Performance Issues
- **High CPU Usage**: Reduce effect complexity or disable unused features
- **Battery Drain**: Disable background processing when not needed
- **Memory Leaks**: Ensure proper cleanup of native resources

### Integration Issues
- **YouTube Authentication**: Verify API credentials and network connectivity
- **Lyrics Providers**: Check provider availability and network access
- **Background Services**: Ensure proper permissions for background operation

**Section sources**
- [SuvMusicApplication.kt:40-82](file://app/src/main/java/com/suvojeet/suvmusic/SuvMusicApplication.kt#L40-L82)
- [MusicPlayer.kt:480-500](file://app/src/main/java/com/suvojeet/suvmusic/player/MusicPlayer.kt#L480-L500)

## Conclusion
SuvMusic represents a sophisticated approach to Android music streaming, combining cutting-edge audio engineering with modern mobile development practices. The project successfully bridges the gap between cloud streaming and professional local playback through its custom native audio engine, while maintaining excellent user experience through thoughtful UI design and robust architecture.

The modular design, extensive use of modern Android technologies, and commitment to high-fidelity audio make SuvMusic an exemplary case study in building professional-grade mobile applications. The codebase demonstrates best practices in performance optimization, dependency management, and user experience design that serve as valuable references for Android developers working on similar projects.