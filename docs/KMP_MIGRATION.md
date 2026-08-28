# SuvMusic Kotlin Multiplatform Migration

This branch establishes the safe KMP path for SuvMusic without replacing the production Android application prematurely. The Android APK remains owned by `:app`; the shared UI and desktop executable are owned by `:composeApp`, with reusable platform-neutral models and services under `:core:*`.

## Migration contract

| Area | Current safe owner | Migration rule | Verification |
|---|---|---|---|
| Android APK | `:app` | Do not remove or rewrite the Android entry point until shared feature parity is proven | `./gradlew :app:assembleDebug` in GitHub Actions |
| Shared UI | `:composeApp:commonMain` | Keep composables stateless where possible and pass platform actions as callbacks | `:composeApp:compileKotlinDesktop` in GitHub Actions |
| Android shared host | `:composeApp:androidMain` | Use Android-only adapters only at the host boundary | Android workflow |
| Linux desktop host | `:composeApp:desktopMain` | Keep AWT/file/browser/VLCJ integration out of `commonMain` | Linux package workflow |
| Shared domain/model | `:core:model`, `:core:domain`, `:core:db` | Prefer common Kotlin APIs; isolate Media3, SQLDelight drivers, and VLCJ in platform source sets | Android + Linux workflows |
| Legacy Android services | `:core:data`, `:media-source`, `:extractor`, `:scrobbler`, lyrics modules, `:updater` | Migrate one boundary at a time; retain Android-compatible artifacts until consumers move | Android workflow |

## Desktop targets

The Compose Multiplatform desktop module now declares Debian and RPM outputs in addition to the existing Windows installers. The Linux executable uses VLCJ and therefore expects a system VLC/LibVLC installation at runtime. The package workflow validates artifact creation; runtime playback should be smoke-tested on a Fedora or Ubuntu desktop with VLC installed.

### Ubuntu/Debian runtime

Install VLC before launching the Debian package or the unpacked desktop distribution:

```bash
sudo apt update
sudo apt install vlc libvlc5 libvlccore9
```

### Fedora runtime

Install VLC from the enabled Fedora RPM Fusion repositories, then launch the RPM package or unpacked desktop distribution:

```bash
sudo dnf install vlc
```

The exact LibVLC package name can vary with the enabled Fedora release repositories. The application fails gracefully and displays a playback-engine warning when VLC cannot be discovered.

## CI-only verification

Local Gradle builds are intentionally not part of the migration procedure. GitHub Actions is the source of truth for verification because it provisions the Android SDK, NDK, CMake, Protobuf compiler, and Linux packaging tools consistently.

| Workflow | Purpose | Manual trigger |
|---|---|---|
| `.github/workflows/kmp-linux.yml` | Compile shared/desktop code and build `.deb` and `.rpm` artifacts | `gh workflow run kmp-linux.yml --ref feat/kmp-linux-desktop-migration` |
| `.github/workflows/android-kmp-verification.yml` | Assemble the existing Android debug APK after KMP changes | `gh workflow run android-kmp-verification.yml --ref feat/kmp-linux-desktop-migration` |

A successful migration change requires both workflows to complete successfully. The release workflow remains unchanged and is not used for feature-branch verification.

## Current migration ledger

| Shared capability | KMP status | Android compatibility | Linux desktop status |
|---|---|---|---|
| TTML parsing and BetterLyrics | `commonMain` client/parser with Android and desktop HTTP engines | Verified by Android APK CI | Compiled through Linux package CI |
| SimpMusic lyrics | `commonMain` client/models with Android DI adapter | Verified by Android APK CI | Compiled through Linux package CI |
| KuGou lyrics | `commonMain` client/models with Android DI adapter | Verified by Android APK CI | Compiled through Linux package CI |
| LRCLIB lyrics | `commonMain` client with Android DI adapter | Verified by Android APK CI | Compiled through Linux package CI |
| Library and recent listening history stores | Common SQLDelight facade consumed by desktop UI; timestamp/order/reactive-status parity fixed | Existing Room path remains protected | Linux desktop UI reads the per-user SQLDelight database |
| Update metadata/checking | Common update models/checker; Android installer remains host-specific | Existing Android installer/API compatibility retained | Common checker compiles; desktop installer UX remains |
| Last.fm/scrobbler protocol | Common client/models with KMP MD5 actuals | Credentials/config supplied by Android app host | JVM signing/client path compiles |
| YouTube extraction boundary | Shared `YouTubeSource` contract; Android and desktop NewPipe adapters remain host-specific | Existing Android search/stream services remain source of truth | Desktop NewPipe façade implements the contract |
| Downloads | Shared `DownloadManager` contract plus optional shared Home/Search actions and status UI | Existing MediaStore/download queue remains protected; shared host seam is additive | Linux downloader is wired into desktop Main, persists metadata atomically, and exposes progress/status; full Downloads screen parity remains |
| Settings and account session | Typed common settings/session contracts with Android and desktop adapters | Existing DataStore/encrypted session storage remains protected | Settings persist through JVM Preferences; opaque account session is process-local until Secret Service support |
| Local media scanning | Shared `LocalMediaSource` plus optional common root-management capability | Existing MediaStore queries remain protected | Music/Downloads roots feed shared Library UI; native folder picker, persisted roots, and rescan controls are wired |
| Recommendations | Shared personalized/up-next contract; Android engine adapter and Linux offline local adapter | Existing RecommendationEngine remains source of truth | Home UI displays offline local recommendations |
| Library sync scheduling | Shared `LibrarySyncScheduler`; Android WorkManager adapter and Linux scheduled-executor adapter | Existing `LikedSongsSyncWorker` remains protected | Desktop scheduler is host-wired to a periodic local scan with lifecycle shutdown; active only while the app runs |
| Persistence migration beyond these shared contracts, playback feature parity, notifications, installer UX, and final Android host wiring | Still incomplete | Protected by existing `:app` path | Not yet fully ported |

## Incremental end state

The current branch has completed the lyrics-provider KMP slices, shared protocol/data contracts, and database-backed desktop UI surfaces. The packaged Linux runtime now has a CI assertion that extracts the RPM payload and confirms `java.sql/java/sql/DriverManager.class` is present. The final architecture is reached through feature slices rather than a single risky rewrite.
 First, pure models, parsers, repositories, and UI state move to `commonMain`. Next, Android and desktop implementations are supplied for media playback, persistence, networking, file selection, notifications, downloads, and account integrations. Finally, `:app` is reduced to a thin Android host and the desktop host is kept thin as well. No legacy Android module is deleted until all consumers have moved and both platform workflows pass.

> A KMP port is complete only when the same feature contract is exercised from both Android and Linux hosts; copying source files into `commonMain` without platform implementations is not considered complete.
