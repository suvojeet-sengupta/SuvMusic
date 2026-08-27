package com.suvojeet.suvmusic.providers.lyrics

import io.ktor.client.HttpClient

/** Creates the platform HTTP engine without leaking Android/JVM APIs into commonMain. */
expect fun createLyricsHttpClient(): HttpClient
