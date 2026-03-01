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
            version = "1.3.1.2",
            date = "March 1, 2026",
            isLatest = true,
            changes = listOf(
                "Increased default navigation bar opacity to 90% for improved visual aesthetics",
                "Fixed ACRA 5.13 notification configuration to ensure reliable crash reporting"
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
