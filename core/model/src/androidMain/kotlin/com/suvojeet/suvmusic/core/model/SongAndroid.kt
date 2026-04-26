package com.suvojeet.suvmusic.core.model

import android.net.Uri

/**
 * Android-only convenience factory: build a [Song] from local-storage Uri
 * values supplied by ContentResolver / MediaStore. Lives outside Song.kt
 * (which is now in commonMain) so the data class itself stays platform-neutral.
 *
 * Call sites use it as `Song.fromLocal(...)` — the extension on
 * [Song.Companion] is syntactically identical to a regular companion function.
 */
fun Song.Companion.fromLocal(
    id: Long,
    title: String,
    artist: String,
    album: String,
    duration: Long,
    albumArtUri: Uri?,
    contentUri: Uri,
    releaseDate: String? = null
): Song = Song(
    id = id.toString(),
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    thumbnailUrl = albumArtUri?.toString(),
    source = SongSource.LOCAL,
    localUri = contentUri.toString(),
    releaseDate = releaseDate
)
