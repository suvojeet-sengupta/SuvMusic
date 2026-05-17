package com.suvojeet.suvmusic.core.model

/**
 * Outcome of a single download attempt.
 *
 * Returning a tri-state instead of `Boolean` lets the caller distinguish
 * "already had this song / already in flight" (SKIPPED) from "I wrote the
 * file successfully" (SUCCESS) — important because the foreground download
 * service was showing a misleading "Download complete" toast for skipped
 * items.
 */
enum class DownloadResult {
    SUCCESS,
    SKIPPED,
    FAILED,
}
