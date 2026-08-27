package com.suvojeet.suvmusic.providers.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createLyricsHttpClient(): HttpClient = HttpClient(CIO)
