<div align="center">
  <img src="screenshots/suvmusic_banner.png" alt="SuvMusic Banner" width="100%" />

  <br />
  <br />

  <h1>SuvMusic</h1>
  <h3>High-Fidelity Music Streaming for Android</h3>

  <p align="center">
    <strong>SuvMusic</strong> is a premium, open-source music streaming application designed for high-resolution audio enthusiasts. Built with <strong>Jetpack Compose</strong> and a custom <strong>C++ Native Audio Engine</strong>, it provides an ad-free experience with advanced features bridging the gap between cloud streaming and professional local playback.
  </p>

  <!-- Status Badges -->
  <div align="center">
    <a href="https://github.com/suvojeet-sengupta/SuvMusic/releases/latest">
      <img src="https://img.shields.io/github/v/release/suvojeet-sengupta/SuvMusic?style=for-the-badge&color=FA2D48&label=DOWNLOAD%20APK&logo=android" alt="Download APK" />
    </a>
    <a href="https://t.me/TechToli">
      <img src="https://img.shields.io/badge/Join_Telegram-Community-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Join Telegram" />
    </a>
  </div>

  <br />

  <div align="center">
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" />
    <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=android&logoColor=white" alt="Compose" />
    <img src="https://img.shields.io/badge/Engine-Native_C++-00599C?style=flat-square&logo=c%2B%2B&logoColor=white" alt="C++" />
  </div>
</div>

---

## Core Features

### Audio Engineering & Performance
- **Native Audio Engine**: High-fidelity playback powered by a custom C++ JNI layer for low-latency digital signal processing.
- **WSOLA Time-Stretching**: High-quality pitch and speed adjustments without digital artifacts or distortion.
- **Parametric EQ & Spatial Audio**: 10-band ISO standard equalizer and real-time 3D sound positioning with ITD/ILD head shadowing models.
- **High-Resolution Streaming**: Stream Opus audio up to 256kbps with dual-source resolution for consistent quality.

### UI & User Experience
- **Material 3 Interface**: Dynamic theming engine that adjusts system-wide colors based on album artwork.
- **Dynamic Island Mini-Player**: Interactive, floating overlay for background playback control.
- **Gesture Controls**: Integrated video player gestures—Double tap to seek, Vertical swipes for volume/brightness, and Pinch-to-zoom for fill/fit modes.
- **Synchronized Lyrics**: Real-time, word-by-word lyrics integration from LRCLIB, Kugou, and SimpMusic providers.

### Advanced Functionality
- **Listen Together**: Real-time synchronized listening rooms with low-latency network protocols.
- **Music Haptics**: Beat-synchronized tactile feedback for immersive listening.
- **Spotify Migration**: Direct playlist importing from Spotify accounts.
- **Smart Asset Management**: Batch download capabilities with background service support and persistent caching.

---

## Showcase

<div align="center">
  <table>
    <tr>
      <td width="33%"><img src="screenshots/Screenshot_20260128-151017.png" width="100%" alt="Home Screen"></td>
      <td width="33%"><img src="screenshots/Screenshot_20260128-151100.png" width="100%" alt="Player Interface"></td>
      <td width="33%"><img src="screenshots/Screenshot_20260128-151110.png" width="100%" alt="Lyrics View"></td>
    </tr>
    <tr>
      <td><img src="screenshots/Screenshot_20260128-151115.png" width="100%" alt="Library"></td>
      <td><img src="screenshots/Screenshot_20260128-151120.png" width="100%" alt="Settings"></td>
      <td><img src="screenshots/Screenshot_20260128-151123.png" width="100%" alt="Search"></td>
    </tr>
  </table>
</div>

---

## Technical Specifications

SuvMusic is engineered using modern Android development standards.

- **Frontend**: 100% Jetpack Compose for a reactive and fluid user interface.
- **Architecture**: Clean Architecture with MVVM, Hilt Dependency Injection, Room Database, and Kotlin Coroutines.
- **Audio Core**: Custom C23 Native Engine via JNI for high-performance DSP (Limiter, Soft Clipping, EQ).
- **Networking**: Retrofit & OkHttp with custom extractors for high-fidelity stream resolution.

---

## Installation

1.  Navigate to the **[Releases](https://github.com/suvojeet-sengupta/SuvMusic/releases)** page.
2.  Download the latest stable APK file.
3.  Install the APK on an Android device (Android 8.0 or higher required).

---

## Acknowledgments

SuvMusic is an independent project featuring original UI/UX and a custom audio engine. The project utilizes specific core logic from the following open-source resources to maintain compatibility:

- **[Metrolist](https://github.com/MetrolistGroup/Metrolist)**: Core logic for the 'Listen Together' protocol.
- **[NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor)**: High-performance metadata extraction.
- **[SimpMusic](https://github.com/SimpMusic/SimpMusic)**: Lyrics provider implementation logic.

---

## Developer

**Suvojeet Sengupta**
Senior Android Engineer

[![GitHub](https://img.shields.io/badge/GitHub-@suvojeet--sengupta-181717?style=for-the-badge&logo=github)](https://github.com/suvojeet-sengupta)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-@suvojeet--sengupta-0077B5?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/suvojeet-sengupta/)
[![Buy Me A Coffee](https://img.shields.io/badge/Support-Buy_Me_A_Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/suvojeet_sengupta)

---

## License

Copyright © 2026 Suvojeet Sengupta.
This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

**Disclaimer**: This application is intended for educational and research purposes. It interacts with third-party services. Please respect intellectual property rights and applicable terms of service.
