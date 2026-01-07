# SuvMusic

**A Premium, Native Android Client for YouTube Music**

SuvMusic is a modern, feature-rich music player built with Jetpack Compose that brings the YouTube Music experience to Android with a premium user interface and enhanced functionality. It offers a seamless blend of online streaming and offline playback capabilities, wrapped in a beautiful Material 3 design.

![Platform](https://img.shields.io/badge/Platform-Android_8.0+-00C853?style=flat-square)
![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9-7F52FF?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## Key Features

### 🎵 Comprehensive Music Experience
*   **Native YouTube Music Integration**: Access personalized home sections, charts, and recommendations directly from YouTube Music.
*   **Smart Search**: Powerful search capability for songs, albums, artists, and playlists.
*   **Unified Library**: Seamlessly manage your YouTube liked songs along with local downloads.

### 🎧 Advanced Playback
*   **High-Fidelity Audio**: Support for high-quality audio streaming and downloading.
*   **Gapless Playback**: uninterrupted listening experience for albums and mixes.
*   **Background Playback**: Full support for background audio with media notification controls.
*   **Synchronized Lyrics**: Live, time-synchronized lyrics display inspired by modern premium players.

### ⚡ Performance & Offline
*   **Instant Launch**: Smart caching system ensures the app opens immediately with content, even without an internet connection.
*   **Offline Mode**: Download tracks with customizable quality settings for offline listening.
*   **Intelligent Caching**: Automatically optimizes data usage and load times.

### 🎨 Modern User Interface
*   **Material 3 Design**: Fully adaptive UI with dynamic coloring based on album artwork.
*   **Theme Support**: Complete support for system Dark and Light themes.
*   **Fluid Animations**: Smooth transitions and interactive elements throughout the application.
*   **Apple Music-Inspired Recents**: A dedicated history view to track your listening journey.

---

## Technical Architecture

SuvMusic is built using modern Android development practices and libraries, ensuring robustness, scalability, and performance.

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Architecture Pattern**: MVVM (Model-View-ViewModel) with Clean Architecture principles
*   **Dependency Injection**: Hilt
*   **Asynchronous Programming**: Kotlin Coroutines & Flows
*   **Media Engine**: Android Media3 (ExoPlayer)
*   **Network**: Retrofit / OkHttp
*   **Data Extraction**: NewPipe Extractor (for YouTube internal API interaction)
*   **Local Storage**: DataStore Preferences (for session and settings), File System (for caching)
*   **Image Loading**: Coil

---

## Installation

1.  Download the latest APK from the [Releases](https://github.com/suvojeet-sengupta/SuvMusic/releases) section.
2.  Install the APK on your Android device (Android 8.0 Oreo or higher required).
3.  Launch the app and sign in with your Google account for personalized recommendations, or use it anonymously.

---

## Disclaimer

This project is an open-source educational initiative. It uses the NewPipe Extractor to interface with YouTube services. It is not affiliated with, endorsed by, or associated with Google LLC or YouTube. All content is copyright to their respective owners.

---

**Developed by Suvojeet Sengupta**
