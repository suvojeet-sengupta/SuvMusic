# Changelog

## New Features

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
