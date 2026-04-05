# Changelog

## [2.2.1.0] - 2026-04-05

### Added
- **Real-time Download Progress**: Experience a more native feel with live percentage and MB tracking during in-app updates.
- **Local Audio Duration Filter**: New setting to filter out short audio clips from your local library for a cleaner browsing experience.
- **Advanced Lyrics Customization**: Added blur intensity control and appearance settings for a personalized lyrics view.
- **Sticky Lyrics Header**: Redesigned lyrics screen with a sticky top layout for persistent access to playback controls.

### Improved
- **Premium Updater UI**: Completely redesigned the System Update screen using expressive gradients, Squircle shapes, and Material 3 Expressive components.
- **Performance Overhaul**: Reduced redundant UI recompositions by ~70% and offloaded LibraryRepository operations to background threads for instantaneous feedback.
- **UI Modernization**: Replaced legacy Toast notifications with modern, theme-aware Snackbars across 17+ core files.
- **Lyrics Animations**: Implemented Apple Music-style smooth animations for lyrics transitions.

### Fixed
- **Playlist Stability**: Resolved infinite loading loops in PlaylistScreen and improved playlist ownership detection.
- **Media Notifications**: Fixed notification thumbnails not showing due to corrupted URL resolution in `getHighResThumbnail`.
- **System Theme Support**: Switched to MaterialComponents base theme to resolve Snackbar inflation crashes.
- **Multimedia Reliability**: Optimized YouTube stream resolution recovery and fixed artwork fallbacks in the queue.

## [2.1.4.0] - 2026-03-29 (Hotfix)

### Fixed
- **Shuffle Integrity**: Resolved a critical issue where shuffle mode would cause rapid song skipping or "Source Errors."
- **Audio Output Switching**: Fixed playback failing to resume or becoming silent when switching between Bluetooth and speakers.
- **UI Transitions**: Eliminated artwork style flicker when expanding the Mini Player.

### Added
- **Visual Error Feedback**: Introduced a modern, expressive error overlay on the Player screen with "Copy Error" and "Retry" actions for easier troubleshooting.

### Improved
- **Intelligent Recovery**: Implemented exponential backoff for network retries and automatic search fallback for expired YouTube streams (403/410 errors).
- **Battery & Efficiency**: Optimized battery life with Dynamic Audio Offload and reduced memory usage for Android Auto thumbnails.
- **Safe Volume**: Added "Safe Volume Ducking" to prevent sudden loud volume when connecting Bluetooth devices.
- **Backup Efficiency**: Optimized cloud backup rules to exclude large cache files, ensuring faster and more reliable device transfers.

## [2.1.3.0] - 2026-03-25

### Added
- **Comprehensive Backup & Restore**: Securely back up your entire SuvMusic experience, including library cache and encrypted YouTube settings, using the new high-performance `.suv` format.
- **Lyrics Screen Overhaul**: Experience a completely redesigned, more immersive lyrics screen built with modern Material 3 Expressive components.
- **Home Screen Personalization**: Take control of your home feed with new layout customization options and a refined 4-row Quick Picks layout.
- **Enhanced Playlist Management**: Expanded playlist support with native `.suv` and standard `.m3u` export options, plus a new 'Queue to Playlist' action.
- **Visual Refinements**: Introduced the M3E wavy seekbar as the new default with smoother, more expressive animations and enabled rotating vinyl artwork by default.
- **Customization & Links**: New blur customization for iOS-style navigation bars and added official SuvMusic website and Privacy Policy links for quick access.

### Changed
- **Audio Stability**: Optimized audio output detection and fixed 'Auto-resume after calls' to ensure seamless playback transitions.
- **Under-the-Hood Fixes**: Resolved various UI state restoration issues, improved dynamic theme transitions, and filtered virtual devices for better audio switching.

## [2.1.2.0] - 2026-03-21

### Added
- **Modernized Credits**: Completely redesigned the Credits screen with expressive styling, TopAppBar scroll behavior, and unified developer profile visuals matching the About screen.
- **Universal Playlist Import**: Added direct native support for importing YouTube Music sources and universal support for `.m3u` playlist files.
- **MediaButtonReceiver**: Integrated support for better media hardware key control handling and peripheral stability.
- **Playlist Renaming**: Added the ability to rename local playlists directly from the app.

### Changed
- **Expressive UI Overhaul**: Redesigned Playlist and Import screens with Material 3 Expressive UI, including enhanced headers and sort menus.
- **UI Consistency**: Expanded the use of the 28.dp Squircle shape across all remaining components for complete brand uniformity.
- **TopAppBar Behavior**: Enabled dynamic scroll behavior for TopAppBars on all major screens, including the Credits screen.
- **Fab Menu Polish**: Renamed 'Import from Spotify' to 'Import Playlist' in the FAB menu to reflect universal import support.

### Fixed
- **Database Stability**: Incremented database version to 9 to resolve critical Room migration and data integrity crashes.
- **Playlist Management**: Resolved an issue where imported playlists would occasionally be duplicated or uneditable.
- **Player Layout**: Stabilized player layout during loading by using fixed-height seekbar containers to prevent UI jank.
- **Library Sorting**: Fixed an issue where the 'Date Added' sorting was not correctly applied in some library views.
- **Radio Logic**: Corrected an edge-case bug where the Radio queue logic would fail to trigger under specific conditions.
- **Auto-Skip Stability**: Eliminated auto-skip chains caused by parse errors during stream resolution.
- **Resolution Performance**: Optimized playback latency and fixed resolution race conditions during rapid track skipping.

## [2.1.1.0] - 2026-03-17

### Added
- **Instant Playback**: Reduced playback startup latency to 500ms for near-instant response.
- **Wavy Seekbar**: Introduced the Material 3 Expressive wavy seekbar style for a more dynamic playback experience.
- **Bouncy Animations**: YouTube-style scale and bounce animations for Like and Dislike buttons.
- **Adaptive UI**: Redesigned Credits and Song Info screens that adapt their background color to the current track's artwork.
- **Offline Enhancements**: Added folder browsing support and integrated local albums/artists into the search experience.
- **Embedded Lyrics**: Support for extracting and displaying lyrics directly from local audio files.
- **Track Sorting**: New sorting options (A-Z, Artist, Date) and mass reordering for playlists.
- **Playback Options**: Added 'Auto-resume after calls' setting and improved audio focus handling.

### Changed
- **Modern Infrastructure**: Fully migrated to Navigation 2.9.0 (Type-safe routes) and Coil 3 for improved performance and reliability.
- **Intelligent UI**: The 'Add to Queue' option is now context-aware and hidden for the currently playing song.
- **Marquee Effects**: Added smooth marquee scrolling for long song and artist titles in the player.
- **Optimized Caching**: Implemented a 24-hour auto-clear policy for temporary playlist data to optimize storage.
- **Performance Optimization**: Offloaded playback state saving to background threads and optimized service lookups.
- **Resource Management**: Optimized position updates to reduce main-thread CPU usage during playback.

### Fixed
- **Skip Stability**: Resolved orphaned resolution coroutines and auto-skip chains to ensure reliable track skipping.
- **Double Resolution**: Fixed race conditions between service and client when resolving placeholder URIs.
- **Audio Polish**: Resolved volume micro-toggle clicks and improved TTS ducking/restoration logic.
- **Android Auto Overhaul**: Comprehensive fixes for car-screen controls, skip buttons, auto-advance, and menu loading.
- **Sleep Timer Fix**: Resolved a critical issue where 'End of Song' timer wouldn't trigger when tracks were preloaded.
- **Stream Stability**: Improved fallback and recovery logic for high-latency or unstable network connections.
- **UI Refinements**: Fixed numerous small bugs in selection modes, loading indicators, and layout overflows.

### Performance
- **SDK 36**: Updated target SDK to 36 and optimized JNI-based recommendation scoring.

## [2.1.0.0] - 2026-03-14

### Added
- **Material 3 Expressive Redesign**: Massive UI overhaul across the entire app with unified expressive components.
- **Expressive Shapes**: Switched to modern `Squircle` shapes (28.dp) for all artwork, thumbnails, and action buttons.
- **Redesigned Core Screens**: Completely refreshed Settings, Album, Playlist, Artist, and Search screens for a more premium look and feel.
- **Enhanced Updater**: New M3E-styled Updater screen and dialog with custom pulse loading indicators.
- **Listen Together 3.0**: Redesigned sync screen with better visual feedback and expressive controls.

### Changed
- **Unified Icons**: Standardized setting items with icons enclosed in expressive squircle boxes.
- **Improved Hierarchy**: Standardized section titles and spacing across all configuration interfaces.

### Fixed
- **Robust Backup Restore**: Resolved critical Swift Backup restoration crashes by robustly handling encrypted preference corruption after OS re-installs.
- **Login Optimization**: Automatically enables 'Sync with YouTube History' for logged-in users to ensure a seamless experience from the first run.

### Performance
- **TV & D-pad Optimization**: Integrated `dpadFocusable` across all major UI elements for buttery-smooth remote navigation.

## [2.0.2.0] - 2026-03-10

### Added
- **High-Quality Thumbnails**: Enforced 544px high-resolution artwork across all UI components for a more premium visual experience.
- **YouTube Music Style Mini Player**: Refined the YT Music mini player with a top-aligned progress bar and classic control layout.

### Changed
- **Memory Optimization**: Optimized ImageLoader configuration with 30% heap usage and hardware acceleration to resolve decoding jank.
- **Resource Management**: Replaced all manual ImageLoader instances with singleton context.imageLoader to prevent potential memory leaks.
- **Mini Player Refinement**: Removed artist name click action to prevent accidental navigation while controlling playback.
- **Recommendations**: Significantly improved the recommendation engine for better track discovery.
- **Enhanced Media Controls**: Improved notification sync for shuffle and repeat states with better visual feedback.

### Fixed
- **Stability**: Fixed mini player swipe-down dismissal logic and optimized transitions.

## [2.0.1.0] - 2026-03-10

### Added
- **Modernized Update Mechanism**: Migrated to a standalone `:updater` module with structured JSON changelog support and a dedicated rich-UI Updater screen.

### Changed
- **Stability Fixes**: Minor UI refinements and code optimization.

## [2.0.0.0] - 2026-03-08

### Added
- **Spotify Pro Import**: Enhanced Spotify integration supporting albums, artists, and individual tracks with real-time fetching progress.
- **Next-Gen Personalization**: High-performance Recommendation Engine with JNI-based native scoring and deep YouTube Music integration.
- **Persistent Logging & Diagnostics**: Integrated a robust file-based logging system with 'Share App Logs' feature.
- **Cinematic Player Transitions**: Completely refactored Video Mode using AnimatedContent for seamless transitions.
- **Infinite Play (Radio Mode)**: New toggle in the Queue screen that automatically extends sessions with similar songs.
- **Listen Together 2.0**: Redesigned with Material 3 Expressive UI and ultra-low latency Protobuf-based transport.
- **Ringtone Engine**: Fully restored 'Set as Ringtone' feature with integrated audio trimmer.

### Changed
- **Performance Optimization**: Implemented explicit keys in all major lists to reduce UI re-composition.
- **App Health**: Optimized ACRA integration for Android 15/16 with more detailed system context.
- **Interactive Queue**: Added full context menus to every item in the queue for better management.
- **Adaptive Recommendations**: 'Made for You' banners now have a 7-day persistence logic.

## [1.3.1.2] - 2026-03-01

### Added
- **Crash Reporting**: Integrated ACRA for robust crash reporting and log sharing via Telegram or direct download.
- **Custom Download Location**: Users can now specify a custom storage path for downloads in Settings.
- **Liquid Glass Navigation**: New iOS-inspired liquid glass bottom navigation bar (toggleable in Settings).
- **Android TV Support**: Added initial feature declarations for better compatibility with Android TV devices.

### Changed
- **UI Refinements**: Set default navigation bar opacity to 90% for a sleeker look.
- **Dynamic TopBar**: The TopBar now hides on scroll in Album and Playlist screens to prevent status bar overlap.
- **Branding**: Restored the classic app logo and related branding elements.

### Fixed
- **Splash Screen**: Resolved splash background morphing on Xiaomi devices and corrected adaptive icon rendering.
- **Full-Screen Playback**: Fixed view height calculations to ensure a true full-screen player experience on Android 12+.
- **Stability**: Fixed conflicting declarations in SessionManager and resolved various compilation errors in services.
- **ACRA Configuration**: Corrected notification DSL property names for ACRA 5.13 compatibility.

### Performance
- **Startup Optimization**: Eliminated a 3-second startup hang in MainActivity and optimized splash screen transitions.
- **APK Size**: Reduced APK footprint and installation time using `abiFilters` and `resourceConfigs`.
- **Resource Efficiency**: Optimized app logo size to eliminate lag during installation.

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
