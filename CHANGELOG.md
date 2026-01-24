# Changelog

## [1.0.4] - 2026-01-25

### Added
- **Music Haptics**: Beat-synchronized vibration engine for an immersive listening experience.
- **Word-by-Word Lyrics**: Precise, time-synced lyrics with active word highlighting and provider switcher.
- **Floating Player**: Draggable system-wide overlay (formerly Dynamic Island) for global playback control.
- **Pitch & Speed Control**: Real-time DSP for playback speed (up to 5x) and pitch adjustment.
- **Shared Element Transitions**: Cinematic animations when navigating between Mini Player and full Player screen.
- **Batch Downloads**: Queue multiple albums/playlists for background processing with notification support.
- **Instant Downloads**: Save buffered tracks instantly to local storage.
- **YouTube Integration**: Explore and Community sections from YouTube Music directly on the Home screen.
- **Glance Widgets**: Modern homescreen widgets with real-time state updates.
- **Support Screen**: Integrated options for "Buy Me a Coffee", GitHub starring, and community links.

### Changed
- **UI Redesign**: Complete Material 3 overhaul for Library, Settings, and Storage screens.
- **Pure Black Mode**: True OLED black background for Home screen and Player in dark mode.
- **Kotlin 2.3**: Upgraded to latest Kotlin compiler and Gradle 8.14.
- **Hilt 2.58**: Updated DI framework for Kotlin 2.3 compatibility.
- **Media3 1.9.0**: Upgraded media session and playback engine.
- **Marquee Text**: Added scrolling effect for long titles in Mini Player and widgets.

### Fixed
- Resolved session management race conditions and improved async data handling.
- Fixed memory leaks in MusicPlayer and receiver leaks in services.
- Corrected download reliability and persistent "Downloaded" indicators.
- Improved volume normalization parameters to eliminate stuttering.
- Fixed critical crashes related to image loading and vector drawable rendering.

## [1.0.3] - Previous Release

### New Features

### Dual Music Source & High-Fidelity Audio
- **JioSaavn Integration**: Added full support for JioSaavn as a music source, enabling 320kbps MP3 streaming capabilities.
- **Source Selection**: Implemented a new "Default Music Source" selection during onboarding (Welcome Screen) and in Settings, allowing users to prioritize between YouTube Music and JioSaavn.
- **Dynamic Content**: Search tabs and Home content now dynamically adapt based on the selected primary music source (e.g., JioSaavn tabs appear first if selected).
- **Audio Quality Indicators**: Added explicit quality badges in the player (e.g., "320kbps") and dynamic labels in Settings to reflect the active source's bitrate capabilities.
- **Smart Sharing**: Enhanced the sharing functionality to generate native JioSaavn links when sharing songs sourced from JioSaavn.

### OTA & Updates
- **In-App Updates (OTA)**: Integrated support for Over-The-Air updates, featuring a new scrollable update dialog to view release notes comfortably within the app.

### Artist & Credits Experience
- **Artist Screen Redesign**: Completely overhauled the Artist Screen with an Apple Music-inspired UI, featuring high-resolution imagery and improved navigation.
- **Enhanced Song Credits**: Redesigned the "View Credits" sheet to display detailed artist information, including profile photos sourced from YouTube Music/JioSaavn.

### Ringtone Management
- **Set as Ringtone**: Added native support to set the currently playing song as the device ringtone directly from the player menu.
- **Permission Handling**: Implemented robust system permission handling for modifying system settings (ringtones).

## Enhancements

### User Interface & Experience
- **Landscape Support**: Added full landscape mode support for the Player Screen, ensuring a seamless experience on tablets and rotated devices.
- **Adaptive Layouts**: Implemented width-based adaptive layouts for floating windows to improve multitasking on various screen sizes.
- **Visual Polish**: Fixed full-screen blur backgrounds in the player and normalized high-resolution artwork display across Notifications and Home screens.
- **About Screen**: Updated with comprehensive developer information, social media links (GitHub, Instagram, Telegram), and clear feature highlights.
- **Welcome Experience**: Expanded the onboarding flow to highlight new features and guide users through source selection.

### Performance
- **Instant Playback**: Optimized buffering logic to achieve near-instant playback start times (approx. 0.5s).
- **Concurrent Operations**: Improved threading to allow seamless downloading of tracks while simultaneously streaming music without interruptions.

## Bug Fixes

- **Home Screen Stability**: Resolved critical issues causing the Home Screen to appear blank or flicker after navigation gestures (swiping).
- **Playlist Management**: Fixed bugs related to incorrect playlist thumbnails and author attribution display.
- **Compatibility**: Enabled Java desugaring to prevent crashes on older Android versions and added package visibility queries for Android 11+ compliance.
- **Download Status**: Fixed an issue where the "Downloaded" indicator would fail to appear immediately after an app restart.

## Technical Updates

- **Architecture**: Refactored `AppModule` and Repositories to support dependency injection for the dual-source architecture.
- **Caching**: Implemented intelligent caching for JioSaavn home content to improve offline resilience and load times.
