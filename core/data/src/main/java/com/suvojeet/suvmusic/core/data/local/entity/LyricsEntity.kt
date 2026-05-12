package com.suvojeet.suvmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent cache for fetched lyrics so word-timed Enhanced LRC ("LRC v2")
 * survives app restarts and avoids re-hitting providers on every replay.
 *
 * The primary key combines [songId] and [providerName] so multiple providers
 * can be cached side-by-side for the same song (e.g. SimpMusic enhanced +
 * LRCLIB plain). The repository writes the canonical Enhanced LRC string
 * (output of LyricsUtils.serialize) to [lrcContent].
 */
@Entity(tableName = "lyrics_cache", primaryKeys = ["songId", "providerName"])
data class LyricsEntity(
    val songId: String,
    val providerName: String,
    val lrcContent: String,
    val isSynced: Boolean,
    val sourceCredit: String?,
    val timestamp: Long = System.currentTimeMillis()
)
