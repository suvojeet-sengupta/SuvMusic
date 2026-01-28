# SuvMusic v1.0.5 Release

![SuvMusic v1.0.5 Banner](release_banner_v1.0.5.png)

## Release Summary

SuvMusic v1.0.5 is a massive feature-packed update that redefines the user experience. This release brings **Spotify Playlist Import**, a completely **Redesigned Home Screen**, **Offline Caching**, and a new **Floating Pill Player**. We've also added **SponsorBlock**, **Waveform Visualizations**, **Voice Search**, and fully overhauled **Last.fm** and **Lyrics** integrations.

## Detailed Technical Changes

### 🚀 Major Features

#### Spotify Import & Migration
- **Background Import**: Implemented a robust background service for importing Spotify playlists with notification progress and cancellation support (Commits: `debc138`, `d469dea`).
- **Redesigned UI**: Created a new, Material 3 inspired import screen with real-time progress updates (Commit: `45a02b7`).
- **Smart Matching**: Improved logic to use actual playlist names and better matching algorithms (Commit: `25f9ccc`).

#### Home Screen 2.0
- **Visual Overhaul**: Complete redesign with a gradient background, quick access grid, and polished card components (Commit: `2f3213a`).
- **Smart Content**: Added "Mood Chips" for accurate content fetching and smart refresh logic (Commits: `6b6337f`, `8c74e35`).
- **Search Category Chips**: Added home-screen style category chips to the search interface for faster filtering (Commit: `cbee30d`).
- **Developer Spotlight**: Added a premium footer highlighting the developer and contributors (Commit: `85db7be`).

#### Visual & Metadata Polish
- **Multiple Artists**: Now correctly displays *all* artists for a track, not just the first one (Commit: `f63597d`).
- **Premium Loading**: Replaced generic spinners with premium blurred backgrounds for loading states (Commit: `71061c3`).


#### Offline Mode & Caching
- **Offline Infrastructure**: Implemented offline caching for playlist songs and liked music (Commit: `c78c1a0`).
- **Download Grouping**: Added support for downloading albums/playlists into folders and grouped them in the downloads screen (Commit: `70cf1ab`).
- **Library Visibility**: Ensured playlists and downloaded content are fully accessible and visible when offline (Commit: `36cf633`).

#### Floating Pill Player
- **New Design**: Implemented a sleek "Floating Pill" mini-player style (Commit: `6985650`).
- **Functionality**: Added like/dislike buttons, close functionality, and resolved transparency/gap issues (Commits: `a5f5b41`, `bc80974`).

#### Video Mode Experience
- **Seamless Switching**: improved logic for switching between Audio and Video modes without state resets (Commits: `10aaffc`, `33f23e6`).
- **Visual Immersion**: Added an **Ambient Glow Effect** for a more immersive viewing experience (Commit: `90a76b9`).
- **Smart Matching**: Enhanced algorithms to find official music videos more accurately (Commit: `9ebc4e6`).
- **Performance**: Optimized video preloading and background bandwidth usage for smoother playback (Commit: `ef1d31c`).

### 🎵 Enhancements & Integrations

#### SponsorBlock & Visuals
- **SponsorBlock**: Added granular category control, waveform visualization, and synced skipping across Audio/Video modes (Commits: `a45da62`, `7945194`, `bba0f59`).
- **Waveform**: added beautiful waveform visualizations to the player (Commit: `7945194`).

#### Last.fm & Lyrics
- **Last.fm Overhaul**: Ported logic to a new module, added in-app WebView auth, and created dedicated settings (Commits: `37806ba`, `a43f9c4`, `1fb8d4f`).
- **Dislike Sync**: Disliking a song now properly syncs the functionality with YouTube Music (Commit: `cd2591a`).
- **KuGou Lyrics**: Integrated KuGou provider for expanded lyrics coverage (Commit: `386b70d`).
- **Provider Choice**: Added option to select your preferred lyrics provider (Commit: `1acc408`).
- **Lyrics PDF Export**: Upgraded the lyrics export feature with a beautiful brand-aware design and PDF support (Commits: `567b372`, `c785972`).
- **Voice Search**: Added voice search capability for easier navigation (Commit: `a634af2`).

#### "Create a Mix"
- **Feature**: Implemented a "Create a Mix" feature with an artist picker and home screen card (Commit: `98f150e`, `6869e32`).

### 💎 Hidden Gems & Polish

#### Audio & Performance
- **Volume Boost**: Added a VLC-style volume boost feature in settings (Commit: `df993d3`).
- **Instant Playback**: Tuned buffer settings for near-instant 250ms playback start (Commit: `0346979`).

#### Visuals & UX
- **Pure Black Mode**: Redesigned appearance settings to include a new "Pure Black" OLED-friendly mode (Commit: `214231a`).
- **Credits Screen**: Added specialized credits screen to honor contributors and open-source libraries (Commit: `d1c9d23`).


### 🛠️ Stability & Refactoring

- **Smart Updates**: Update checker now detects newer builds even if the version name is the same (Commit: `3486750`).
- **Privacy Fix**: Fixed an issue where viewed playlists were automatically saved to the library (Commit: `ef96f80`).
- **Audio Offload**: Added audio offload support for improved power efficiency on supported devices (Commit: `b7ce9c0`).
- **Video Mode**: Fixed black screens, state resets, and matching logic for official clips (Commits: `10aaffc`, `9ebc4e6`).
- **ANR Fixes**: Resolved critical ANRs in settings and race conditions in downloads (Commit: `860d02c`).
- **Module Migration**: Moved providers to a separate `:providers` module (Commit: `37806ba`).

## Full Changelog

- `bba0f59` - feat: SponsorBlock sync issues when switching between Audio and Video modes
- `42994ef` - style: redesign SponsorBlock settings to match app aesthetic
- `7945194` - feat: add granular category control and waveform visualization
- `37806ba` - Refactor: Migrate lyrics and Last.fm providers to :providers module
- `1fb8d4f` - feat: improve Last.fm integration with dedicated settings screen and better visibility
- `86a31d6` - feat: implement Last.fm integration with Mobile Login and configurable scrobbling settings
- `cd120f4` - ported lastfm logic from metrolist and switched to ktor
- `4cb361d` - add better logging and error handling for lastfm scrobbling
- `e2e6420` - fixed lastfm scrobbling not working properly
- `4d94183` - chore: cleanup debug logs and interceptors after successful Last.fm login debug
- `02ae1f2` - fix: handle nullable session and API errors in LastFmResponse
- `063ce21` - fix: revert format in signature and improve error logging
- `4f2794a` - fix: include format param in Last.fm signature generation
- `a43f9c4` - feat: implement in-app Last.fm authentication using WebView and fix connection issues
- `0d686cb` - fix: Resolve syntax errors in SettingsScreen
- `2ba79d7` - fix: Restore player settings in MusicPlayerService
- `a0e5125` - fix: Resolve build issues in CI and MusicPlayerService syntax
- `802b8b2` - fix: Resolve build issues in CI (Gradle imports and Hilt module syntax)
- `bccd71a` - feat: Integrate Last.fm scrobbling support
- `cdd07e2` - Fix: Ensure full synced song list is displayed after refresh
- `99ab16c` - Docs: Update README with new banner and organized screenshot gallery
- `25f9ccc` - UI: Remove dislike button from song actions, Spotify Import: Use actual playlist name
- `debc138` - feat: implement background Spotify playlist import with notifications and cancellation support
- `45a02b7` - design: redesign Spotify Import screen to Material 3
- `d1c9d23` - Add Credits screen featuring Suvojeet Sengupta, NewPipe Extractor, SponsorBlock, and other libraries
- `85db7be` - Redesign HomeScreen footer with premium UI and developer spotlight for Suvojeet Sengupta
- `a45da62` - feat: Add sponsorblock
- `d7ec6ce` - improved mood chips loading, now they actually show related songs and playlists
- `0a9cc60` - fixed newpipe extractor maven coordinates
- `b87d54b` - enhanced radio mode with extractor fallback for related songs
- `dc95505` - switched to official newpipe extractor 0.25.1
- `90a76b9` - added ambient glow effect for video mode
- `3393f45` - added lyrics text position and animation style settings in appearance
- `d3f675e` - added keep screen on feature in misc settings for expanded player
- `3486750 - update logic now checks for newer builds even if version name is the same
- `567b372` - upgraded lyrics pdf export with beautiful brand-aware design
- `f63597d` - fixed artist names to show everyone not just the first one
- `d8d8c20` - synced app version with build.gradle in about screen
- `cbee30d` - added category chips like home screen for search filtering
- `df993d3` - added volume boost in settings like vlc player
- `45488cf` - fix: Update NewPipeExtractor to fix YouTube playback (commit 17d6e5c)
- `f05d448` - Update NewPipe Extractor to v0.25.0 and clean up temp files
- `6b6337f` - feat: add mood chips to home screen with accurate content fetching
- `93467e8` - Revert "feat: implement like toggle logic and add toast notifications in PlayerScreen"
- `b5125ca` - Revert "fix: prevent like status reversion by implementing a temporary blacklist for unliked songs"
- `ee157d8` - Removed settings and notification icons from HomeScreen header
- `7a3b823` - Removed gradient background from HomeScreen
- `2f3213a` - Refactored HomeScreen with Spotify-style UI: added gradient background, quick access grid, and polished card components
- `a7d3e6b` - fix: prevent like status reversion by implementing a temporary blacklist for unliked songs
- `019496f` - feat: implement like toggle logic and add toast notifications in PlayerScreen
- `ef96f80` - fix: prevent automatic saving of viewed playlists
- `b7ce9c0` - feat: implement audio offload with info dialog and fix media3 build errors
- `a02967e` - feat: add audio offload setting for improved power efficiency
- `b824b09` - refactor: restrict miniplayer dragging and improve swipe gestures
- `d469dea` - Redesign Spotify Import with real-time progress and M3 visuals
- `ef1d31c` - fixed video preloading and added background bandwidth optimization
- `282c25c - added video error dialog with dominant color themes and fixed some transition glitches
- `10aaffc` - fixed video mode state reset on song change and added video stream support for new tracks
- `9ebc4e6` - fixed video mode black screen and made video matching smarter to find official clips
- `31cb6a8` - moved mix section to bottom and linked playlist save button to yt music
- `9df0433` - fix(ui): resolve build errors and improve video mode layout in PlayerScreen
- `dce508a` - feat: redesign Playlist screen and implement library sync with bookmarking
- `7b7ddf6` - fixed miniplayer icon flash and optimized transitions
- `33f23e6` - fixed video mode keeps resetting when toggled
- `a9857e5` - ensure unliking a song updates the local liked music cache
- `a5f5b41` - added like button functionality to the floating pill player
- `281de53` - fixed pill player gap to match nav bar
- `bc80974 - added close button to pill player and fixed transparency
- `0c3002d` - fix spotify import stuck issue and redesign import screen
- `6985650` - Implement Floating Pill Mini Player style and remove Navigation Bar Blur option
- `0b66534` - fix library playlist song count 0 issue
- `f83d064` - remove vertical volume gesture from player screen
- `71061c3` - upgrade loading UI with premium blurred backgrounds
- `75fd762` - replaced star with like icon and removed the dislike button
- `611d289` - fix(ui,player): reset mini player on home and improve notification image fallbacks
- `866aa14` - added notification artwork fallback
- `a324add - fixed audio output switching and added miniplayer dismissal without stopping music
- `28013b5` - music keeps playing even after closing the mini player
- `cd2591a` - feat: implement dislike and sync un-favourite with YouTube Music
- `36cf633` - fix: ensure playlists show in library screen when offline
- `c78c1a0` - feat: implement offline caching for playlist songs and liked music
- `a2c9086` - fix: restore android auto controls and simplify media session connection
- `ff8b07e` - add login requirement for mix creation and show playlist thumbnail in success screen
- `a8236c2` - update pick music UI and add playlist sharing
- `6869e32` - mix creation updated with 100 songs and yt sync
- `52f615a` - feat: Add close button to MiniPlayer on non-Home screens
- `98f150e` - feat: Implement 'Create a Mix' feature with artist picker and home screen card
- `64ec9c8` - chore: remove WelcomeScreen and related navigation logic
- `d89c3a1` - feat: replace welcome screen with dialog
- `0346979` - perf: tune buffer settings for instant playback (250ms start)
- `8c74e35` - feat: smart homescreen refresh logic (30min interval)
- `6d150af` - fix: improve playback reliability with retry logic and auto-recovery
- `92dc55b` - fix inconsistent lyrics search in BetterLyrics
- `c785972` - update lyrics pdf export to save in Documents/SuvMusic
- `768b17c` - added lyrics share as text or pdf
- `dfd3cc9` - fix output device switching not working
- `c224a7c` - added skip login option in welcome screen
- `492aa40` - Fix build errors: missing imports and DI parameter
- `d93920e` - UI: remove skip and guest buttons from welcome screen
- `1acc408` - added preferred lyrics provider selection
- `214231a` - redesigned appearance settings and added pure black mode
- `13cb4af` - added misc settings and lyrics provider screen
- `19e0e03` - fix: resolve SearchScreen build errors
- `e3b014b` - feat: improve Player interactivity, fix Search menu, and redesign Library tabs
- `478bdd8` - fixed android auto not showing up in launcher
- `1fa91e9` - fix: add android auto support metadata
- `5f5cf8e` - docs: update README and CHANGELOG for v1.0.4
- `4047610` - updated support screen with buy me coffee and github link
- `7514071` - removed the spinner entirely, only showing vibes and jokes now
- `5dec494` - fixed that silly syntax error and added some fun messages while lyrics are loading
- `2710e39` - added playback controls to lyrics screen and fixed that annoying auto-scroll while browsing
- `a634af2` - added voice search feature
- `5d25fac` - added full android auto support with youtube streaming impl
- `f1ab3d5` - added android auto support and browsing for downloads/local music
- `e1201b9` - fix: reset batch download progress when starting a new collection
- `70cf1ab` - added support for downloading albums/playlists into folders and grouped them in downloads screen
- `d001871` - added kugou toggle in settings
- `386b70d` - added kugou lyrics provider
- `860d02c` - Fix nasty ANR in settings and race condition in downloads
- `0927765` - Bump version to 1.0.5
