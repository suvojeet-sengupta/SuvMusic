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
            version = "1.3.1.0",
            date = "February 26, 2026",
            isLatest = true,
            changes = listOf(
                "Added visually striking What's New screen",
                "Improved Updater UI with gradient backgrounds and animations",
                "Set default mini player style to Floating Pill",
                "Enhanced Navigation Bar with 85% default transparency",
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
