# SuvMusic

<div align="center">
  <img src="app/src/main/res/drawable/logo.webp" alt="SuvMusic Logo" width="140" height="140" style="border-radius: 20px;">
  <br>
  <h1>SuvMusic</h1>
  <p><b>A Premium, Native Music Client for Android</b></p>
  <p>
    <i>Seamlessly blending YouTube Music & HQ Audio in a stunning Material 3 interface.</i>
  </p>
</div>

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android_8.0+-00C853?style=flat-square)
![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9-7F52FF?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

</div>

---

## Overview

**SuvMusic** redefines the music streaming experience on Android. Built with modern **Jetpack Compose**, it aggregates content from **YouTube Music** and **HQ Audio** into a single, cohesive, and premium application. Whether you want the vast library of YouTube or the 320kbps high-fidelity audio of HQ Audio, SuvMusic delivers it with style.

## ✨ Key Features

### 🎵 Dual Music Sources
*   **YouTube Music Integration**: Full access to charts, mixes, and personalized recommendations.
*   **HQ Audio Support**: Stream songs in **320kbps MP3** quality directly from HQ Audio.
*   **Seamless Switching**: Set your primary source in Settings. Search and Home content dynamically adapt to your preference.

### 🎧 Premium Playback Experience
*   **High-Fidelity Audio**: Crystal clear audio engine with gapless playback.
*   **Smart Quality Badges**: Know exactly what you're listening to (e.g., "HQ Audio • 320kbps" vs "YouTube Music").
*   **Synchronized Lyrics**: Beautiful, time-synced lyrics with blurred background artistry.
*   **Song Credits**: Detailed credits sheet inspired by Apple Music (Performing Artists, Production, Source).

### 🚀 Smart & Dynamic UI
*   **Adaptive Search**: Search tabs automatically reorder based on your preferred music source.
*   **Instant Caching**: Home sections for both YouTube and HQ Audio are cached for instant offline loading.
*   **Material 3 Design**: A modern, fluid interface that extracts colors from album artwork.
*   **Apple Music-Style Recents**: A visual history of your listening journey.

### ⚡ Offline & Library
*   **Unified Library**: Manage downloaded tracks and liked songs in one place.
*   **Offline Mode**: Download music for data-free listening.
*   **Local Playback**: Full support for playing local audio files.

---

## 🛠️ Technical Architecture

SuvMusic is engineered with robust, modern Android practices:

*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM + Clean Architecture
*   **Dependency Injection**: Hilt
*   **Async Processing**: Coroutines & Flows
*   **Caching**: DataStore & File-based Caching (Custom implementation for Home/Search)
*   **Network**: Retrofit, OkHttp, NewPipe Extractor
*   **Media**: Android Media3 (ExoPlayer)
*   **Image Loading**: Coil (with high-res thumbnail extraction)

---

## 📥 Installation

1.  Download the latest APK from the [Releases](https://github.com/suvojeet-sengupta/SuvMusic/releases) section.
2.  Install on your Android device (Android 8.0+ required).
3.  Enjoy premium music streaming!

---

## ⚠️ Disclaimer

This project is an open-source educational initiative. It interfaces with public APIs of YouTube Music and HQ Audio. It is not affiliated with, endorsed by, or associated with Google LLC, YouTube, or HQ Audio. All content is copyright to their respective owners.

---

<div align="center">
  <b>Developed with ❤️ by Suvojeet Sengupta</b>
</div>
