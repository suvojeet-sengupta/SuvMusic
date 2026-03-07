package com.suvojeet.suvmusic.data.model

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
            date = "March 7, 2026",
            isLatest = true,
            changes = listOf(
                "Smart Recommendation Engine: Native scoring system with genre vectors and deep YouTube integration",
                "Infinite Play: New toggle in Queue screen to automatically fetch similar songs",
                "Made for You: Personalized mixes (Daily Mix, Genre Mix, Contextual Mixes)",
                "Closeable Recommendations: Dismiss personalized banners with adaptive 7-day cooldown",
                "Per-Song Action Menus: 3-dot menus for all queue items including Now Playing",
                "Advanced Listen Together: Redesigned with Material 3 Expressive and low-latency Protobuf transport",
                "User Profiles: Personalized settings with radiant UI and user profile integration",
                "Video Mode 2.0: Smooth transitions and stabilized layouts in portrait and landscape",
                "Home Screen Redesign: Professional UI with infinite scrolling and smoother animations",
                "Modern Queue UI: Absolute indexing, better density, and enhanced visual feedback",
                "Set as Ringtone: Fully restored with permission handling and integrated trimmer",
                "Significant Performance: JNI-based scoring, binary transport for sync, and UI optimizations"
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
