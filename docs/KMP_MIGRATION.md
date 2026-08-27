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

Install VLC from the enabled RPM Fusion repositories, then launch the RPM or AppImage:

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

## Incremental end state

The final architecture is reached through feature slices rather than a single risky rewrite. First, pure models, parsers, repositories, and UI state move to `commonMain`. Next, Android and desktop implementations are supplied for media playback, persistence, networking, file selection, notifications, downloads, and account integrations. Finally, `:app` is reduced to a thin Android host and the desktop host is kept thin as well. No legacy Android module is deleted until all consumers have moved and both platform workflows pass.

> A KMP port is complete only when the same feature contract is exercised from both Android and Linux hosts; copying source files into `commonMain` without platform implementations is not considered complete.
