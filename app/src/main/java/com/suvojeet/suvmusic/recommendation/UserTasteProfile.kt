package com.suvojeet.suvmusic.recommendation

/**
 * Represents a user's musical taste profile built from listening history.
 * Used to score and rank recommendations for personalization.
 */
data class UserTasteProfile(
    /** Top artists by weighted play count (artist -> score) */
    val artistAffinities: Map<String, Float> = emptyMap(),
    
    /** Preferred time-of-day listening patterns (hour 0-23 -> weight) */
    val timeOfDayWeights: Map<Int, Float> = emptyMap(),
    
    /** Average song completion rate (0-100) */
    val avgCompletionRate: Float = 0f,
    
    /** Songs the user tends to skip (songId set) */
    val frequentlySkippedIds: Set<String> = emptySet(),
    
    /** Songs the user has liked */
    val likedSongIds: Set<String> = emptySet(),
    
    /** Recently played song IDs for recency boost & dedup */
    val recentSongIds: List<String> = emptyList(),
    
    /** Top played song IDs for familiarity scoring */
    val topPlayedSongIds: Set<String> = emptySet(),
    
    /** Preferred source (YOUTUBE, JIOSAAVN, LOCAL) distribution */
    val sourceDistribution: Map<String, Float> = emptyMap(),
    
    /** Total songs in history — indicates how much data we have */
    val totalSongsInHistory: Int = 0,
    
    /** Whether the user has enough history for personalization */
    val hasEnoughData: Boolean = false
)
