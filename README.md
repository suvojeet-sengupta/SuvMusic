# SuvMusic

<p align="center">
  <img src="app/src/main/res/drawable/logo.webp" alt="SuvMusic" width="120" height="120" style="border-radius: 24px;">
</p>

<p align="center">
  <b>A clean, fast music player for Android</b><br>
  <sub>YouTube Music + HQ Audio streaming. No ads. No nonsense.</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0+-green?style=flat-square" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-1.9-purple?style=flat-square" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT">
</p>

---

## What is this?

Built this because I wanted a single app that handles YouTube Music and high-quality audio streaming. SuvMusic does both.

**Features:**
- Stream from YouTube Music or HQ sources (320kbps)
- Synced lyrics
- Download for offline
- Local file playback
- Dynamic Island-style mini player
- Share via `suvmusic://` deep links
- Material 3 UI with album colors
- Sleep timer, playback speed, queue

## Screenshots

*coming soon*

## Tech Stack

- Kotlin + Jetpack Compose
- MVVM with Hilt DI
- Media3 (ExoPlayer)
- Coil for images
- Coroutines/Flow
- NewPipe extractor

## Installation

Grab the APK from [Releases](https://github.com/suvojeet-sengupta/SuvMusic/releases). Needs Android 8.0+.

Or build:
```bash
git clone https://github.com/suvojeet-sengupta/SuvMusic.git
cd SuvMusic
./gradlew assembleDebug
```

## Deep Links

Share songs with custom URLs:
```
suvmusic://play?id=VIDEO_ID
```

## Note

Personal project, not affiliated with Google. Use responsibly.

---

<p align="center">
  Built by <a href="https://github.com/suvojeet-sengupta">Suvojeet</a>
</p>
