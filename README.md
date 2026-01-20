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
  <img src="https://img.shields.io/badge/Kotlin-2.3-purple?style=flat-square" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT">
</p>

---

## About

SuvMusic is a music streaming app that combines YouTube Music with HQ audio sources into one seamless experience. Built entirely in Kotlin with Jetpack Compose, it focuses on clean design and fast performance.

The app started as a personal project when I wanted a single place to stream music without dealing with multiple apps or ads.

---

## Features

### Playback
- Stream from YouTube Music or HQ sources (up to 320kbps)
- Gapless playback with preloading
- Background playback with media controls
- Adjustable playback speed (0.5x - 2.0x)
- Sleep timer with custom duration support
- Queue management with drag-to-reorder

### Player
- Full-screen player with album art color extraction
- Apple Music-style playback controls with press animations
- Synced lyrics display
- Song credits (artists, producers, writers)
- Video mode toggle for YouTube content
- Output device selection (Bluetooth, wired, etc.)

### Library
- Downloads with quality selection (128k/256k/320k)
- Local file playback (MP3, WAV, M4A, FLAC)
- Playlist creation and management
- Recently played history
- Listening statistics

### UI/UX
- Material 3 design with dynamic theming
- Album art-based color extraction
- Custom seekbar styles (waveform, pill, classic)
- Artwork shape customization
- Light/Dark/System theme modes
- Dynamic Island-style floating player
- Smooth animations throughout

### Sharing
- Custom `suvmusic://play?id=` deep links
- Direct share to any app
- Open audio files from file managers

---

## Architecture

```
com.suvojeet.suvmusic/
├── MainActivity.kt              # Entry point, deep link handling
├── SuvMusicApplication.kt       # Hilt application class
├── data/
│   ├── model/                   # Data classes (Song, Playlist, PlayerState, etc.)
│   ├── repository/
│   │   ├── YouTubeRepository    # NewPipe extractor, stream URLs, search
│   │   ├── JioSaavnRepository   # HQ audio streaming, lyrics
│   │   ├── DownloadRepository   # Offline downloads, file management
│   │   ├── LocalAudioRepository # Device music scanning
│   │   ├── ListeningHistoryRepository
│   │   └── UpdateRepository     # App updates from GitHub releases
│   ├── local/                   # Room database, DAOs
│   └── SessionManager.kt        # DataStore preferences, auth state
├── player/
│   ├── MusicPlayer.kt           # MediaController wrapper, playback logic
│   └── SleepTimerManager.kt     # Timer functionality
├── service/
│   ├── MusicPlayerService.kt    # Media3 MediaSessionService
│   ├── DynamicIslandService.kt  # Floating overlay player
│   └── CoilBitmapLoader.kt      # Artwork loading for notifications
├── ui/
│   ├── screens/                 # 21 composable screens
│   ├── components/              # Reusable UI components
│   ├── viewmodel/               # PlayerViewModel
│   └── theme/                   # Material 3 theming
├── navigation/
│   ├── NavGraph.kt              # Navigation setup
│   └── Destinations.kt          # Route definitions
├── recommendation/
│   └── RecommendationEngine.kt  # Radio mode, similar songs
├── di/
│   └── AppModule.kt             # Hilt dependency injection
└── util/                        # Network monitor, helpers
```

---

## Key Components

### Repositories

| Repository | Purpose | Size |
|------------|---------|------|
| `YouTubeRepository` | Search, streaming, playlists, lyrics, comments | ~108KB |
| `JioSaavnRepository` | HQ audio, synced lyrics, Indian content | ~56KB |
| `DownloadRepository` | Progressive downloads, queue management | ~36KB |
| `SessionManager` | All user preferences, auth, playback state | ~35KB |
| `LocalAudioRepository` | Device music, metadata extraction | ~6KB |
| `ListeningHistoryRepository` | Play counts, stats | ~6KB |

### Screens

- **HomeScreen** - Personalized feed, quick picks, mixes
- **SearchScreen** - Multi-tab search (songs, albums, artists, playlists)
- **LibraryScreen** - Downloads, playlists, local files, liked songs
- **PlayerScreen** - Full playback UI with lyrics, queue, credits
- **SettingsScreen** - App configuration, appearance, playback settings
- **PlaylistScreen** - Playlist details with shuffle/play
- **ArtistScreen** - Artist page with albums, tracks
- **AlbumScreen** - Album track list

### UI Components

- `MiniPlayer` - Compact player for non-player screens
- `WaveformSeeker` - Custom seekbar with waveform visualization
- `SongCreditsSheet` - Apple Music-style credits display
- `PlaylistDialogs` - Create, edit, add to playlist flows
- `SleepTimerSheet` - Timer with presets and custom input
- `DominantColorExtractor` - Palette extraction from artwork

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.3 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Async | Coroutines, Flow |
| Media | Media3 (ExoPlayer) |
| Network | OkHttp, NewPipe Extractor |
| Images | Coil |
| Storage | DataStore, Room |
| Navigation | Compose Navigation |

---

## Building

Requirements:
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 36

```bash
git clone https://github.com/suvojeet-sengupta/SuvMusic.git
cd SuvMusic
./gradlew assembleDebug
```

APK will be in `app/build/outputs/apk/debug/`

---

## Installation

Download the latest APK from [Releases](https://github.com/suvojeet-sengupta/SuvMusic/releases).

Requirements:
- Android 8.0 (API 26) or higher
- ~50MB storage for the app
- Internet connection for streaming

---

## Deep Links

SuvMusic registers a custom URL scheme for sharing:

```
suvmusic://play?id=VIDEO_ID
```

Clicking this link on any device with SuvMusic installed will open the app and start playing that song immediately.

Shared messages include both the custom link and a fallback YouTube Music link for users without the app.

---

## Permissions

| Permission | Usage |
|------------|-------|
| `INTERNET` | Streaming, search, API calls |
| `READ_MEDIA_AUDIO` | Local music playback |
| `POST_NOTIFICATIONS` | Playback controls |
| `FOREGROUND_SERVICE` | Background playback |
| `SYSTEM_ALERT_WINDOW` | Dynamic Island overlay (optional) |

---

## Contributing

PRs welcome. For major changes, open an issue first.

---

## Disclaimer

This is a personal project for educational purposes. Not affiliated with Google, YouTube, or any streaming service. Use responsibly.

---

<p align="center">
  Built by <a href="https://github.com/suvojeet-sengupta">Suvojeet</a>
</p>
