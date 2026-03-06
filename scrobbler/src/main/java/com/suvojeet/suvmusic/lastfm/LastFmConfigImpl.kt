package com.suvojeet.suvmusic.lastfm

import javax.inject.Inject
import javax.inject.Singleton
import com.suvojeet.suvmusic.lastfm.BuildConfig

/**
 * Implementation of LastFmConfig using BuildConfig values from the providers module.
 */
@Singleton
class LastFmConfigImpl @Inject constructor() : LastFmConfig {
    override val apiKey: String = BuildConfig.LAST_FM_API_KEY
    override val sharedSecret: String = BuildConfig.LAST_FM_SHARED_SECRET
}
