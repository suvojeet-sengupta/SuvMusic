package com.suvojeet.suvmusic.core.model

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>,
    val isLatest: Boolean = false
)

object ChangelogData {
    val entries = listOf(
        ChangelogEntry(
            version = "2.0.0.0",
            date = "March 8, 2026",
            isLatest = true,
            changes = listOf(
                "Spotify Pro Import: Enhanced Spotify integration supporting albums, artists, and individual tracks with real-time fetching progress and mobile share link support.",
                "YouTube Playlist Pagination: Resolved continuation token issues and pagination limits for large playlists, ensuring all songs are loaded correctly.",
                "F-Droid Readiness: Added Fastlane metadata and anti-feature disclosures for F-Droid submission.",
                "Next-Gen Personalization: High-performance Recommendation Engine with JNI-based native scoring, genre affinity vectors, and deep YouTube Music integration.",
                "Persistent Logging & Diagnostics: Integrated a robust file-based logging system that captures startup events and provides a 'Share App Logs' feature for easier troubleshooting.",
                "Performance Optimization: Implemented explicit keys in all major LazyColumn and LazyGrid lists, significantly reducing UI re-composition and ensuring buttery-smooth scrolling.",
                "App Health & Crash Reporting: Fully optimized ACRA integration for Android 16, capturing more detailed system context (RAM, Display, Build ID) in every bug report.",
                "Cinematic Player Transitions: Completely refactored Video Mode using AnimatedContent for seamless cross-fading between artwork and video without UI layout shifts.",
                "Infinite Play (Radio Mode): New toggle in the Queue screen that automatically extends your session with similar songs when the queue nears the end.",
                "Interactive Queue: Added full context (3-dot) menus to every item in the queue, including 'Now Playing', allowing for deep song management without leaving the list.",
                "Adaptive Recommendations: 'Made for You' banners are now closeable with a 7-day persistence logic, automatically switching to a 'Daily Mix' style when dismissed.",
                "Infinite Home Feed: Optimized auto-loading logic that proactively fetches diverse recommendation strategies (Artist Deep-dives, Nostalgia, Blended Genres) as you scroll.",
                "Listen Together 2.0: Massive redesign with Material 3 Expressive UI and ultra-low latency Protobuf-based binary transport for perfect real-time synchronization.",
                "MiniPlayer UI: Added dotted progress indicator for a more refined and modern aesthetic.",
                "Ringtone Engine: Fully restored 'Set as Ringtone' feature with integrated audio trimmer, progress tracking, and robust system permission handling.",
                "Under-the-hood Stability: Resolved JVM signature clashes in logging utilities, fixed critical coroutine import errors, and improved audio decoder resilience on newer Android versions."
            )
        ),
        ChangelogEntry(
            version = "1.3.1.2",
            date = "March 1, 2026",
            isLatest = false,
            changes = listOf(
                "Integrated ACRA crash reporting with Telegram and download log sharing",
                "Added iOS-style liquid glass bottom navigation (toggleable in Settings)",
                "Support for setting custom download locations in Settings",
                "Significant startup performance: fixed 3-second hang in MainActivity",
                "Enhanced splash screen transitions and fixed background morphing on Xiaomi",
                "Increased default navigation bar opacity to 90% for improved visual aesthetics",
                "Dynamic TopBar behavior: hides on scroll in Album/Playlist screens",
                "Fixed full-screen playback on Android 12 by using actual view height",
                "Initial Android TV feature support declarations in AndroidManifest.xml",
                "Restored classic app logo and related branding",
                "Fixed ACRA 5.13 notification configuration to ensure reliable crash reporting",
                "Reduced APK footprint and installation lag via resource optimization"
            )
        ),
        ChangelogEntry(
            version = "1.3.1.1",
            date = "February 27, 2026",
            isLatest = false,
            changes = listOf(
                "Implemented official Android Splash Screen API for smoother startup",
                "Added Adaptive Icon support (Circle.png) with 20% inset for all shapes",
                "Optimized app startup by lazy-loading encrypted session data",
                "Hardware acceleration for Mesh Gradient Background (lower CPU/GPU usage)",
                "Significant UI smoothness improvements in Search and Player Queue",
                "Fixed redundant dependency initialization in MainActivity",
                "Optimized theme-switching performance to reduce UI lag",
                "Fixed coroutine compilation errors in entrance animations",
                "Updated Splash Screen logo to match new branding",
                "Added branding credit to 𝕵𝖊𝖊𝖛𝖊𝖘𝖍 (@JazzeeBlaze) in Credits screen"
            )
        ),
        ChangelogEntry(
            version = "1.3.1.0",
            date = "February 26, 2026",
            isLatest = false,
            changes = listOf(
                "Added visually striking What's New screen",
                "Improved Updater UI with gradient backgrounds and animations",
                "Set default mini player style to Floating Pill",
                "Enhanced Navigation Bar with 15% default transparency",
                "Allowed UI content to flow behind Navigation Bar (Glass Effect)",
                "Added 'Pay via UPI' in Support screen (suvojitsengupta21-3@okicici)",
                "Improved Support screen layout with better gradients",
                "Fixed 'Resources\$NotFoundException' crash on launch",
                "Removed QR code scanning and generation to optimize app size",
                "Added transparency customization (0-85%) for mini player and nav bar",
                "Removed blank backgrounds from mini player for all styles",
                "Embedded metadata and album art in downloaded songs",
                "Resolved various compiler warnings and deprecated API usages",
                "Optimized overall system stability and performance"
            )
        )
    )
}
