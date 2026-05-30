package com.suvojeet.suvmusic.data.repository.youtube.streaming.potoken

/**
 * Result of a successful Web-client PoToken generation.
 *
 * - [playerRequestPoToken] is bound to the videoId and goes into the
 *   `/player` request body at `serviceIntegrityDimensions.poToken`.
 * - [streamingDataPoToken] is bound to the session id (visitorData) and is
 *   appended to the resolved stream URL as a `pot=` query parameter.
 *
 * Ported from Metrolist (com.metrolist.music.utils.potoken).
 */
class PoTokenResult(
    val playerRequestPoToken: String,
    val streamingDataPoToken: String,
)
