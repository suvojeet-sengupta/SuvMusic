package com.suvojeet.suvmusic.lastfm

/** Android host adapter; secrets are supplied by the application BuildConfig. */
class LastFmConfigImpl(
    override val apiKey: String,
    override val sharedSecret: String,
) : LastFmConfig
