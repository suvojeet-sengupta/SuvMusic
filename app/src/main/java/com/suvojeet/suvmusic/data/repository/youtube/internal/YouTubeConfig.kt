package com.suvojeet.suvmusic.data.repository.youtube.internal

/**
 * Central configuration for YouTube Music API.
 * Update these versions when YouTube deprecates old client versions.
 */
object YouTubeConfig {
    const val CLIENT_NAME = "WEB_REMIX"
    const val CLIENT_VERSION = "1.20240101.01.00" // Updated from 1.20230102.01.00
    
    const val WEB_CLIENT_NAME = "WEB"
    const val WEB_CLIENT_VERSION = "2.20240101.00.00" // For comments and other non-music specific features
    
    const val DEFAULT_HL = "en"
    const val DEFAULT_GL = "US"
    
    const val BASE_URL = "https://music.youtube.com/youtubei/v1"
    const val PUBLIC_BASE_URL = "https://music.youtube.com/youtubei/v1"
}
