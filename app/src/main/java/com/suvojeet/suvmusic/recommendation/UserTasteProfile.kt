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
    
    /** Songs the user has disliked — strongly penalized or excluded */
    val dislikedSongIds: Set<String> = emptySet(),
    
    /** Artists of liked songs — boosted in scoring */
    val likedArtists: Set<String> = emptySet(),
    
    /** Artists of disliked songs — penalized in scoring */
    val dislikedArtists: Set<String> = emptySet(),
    
    /** Recently played song IDs for recency boost & dedup */
    val recentSongIds: List<String> = emptyList(),
    
    /** Top played song IDs for familiarity scoring */
    val topPlayedSongIds: Set<String> = emptySet(),
    
    /** Preferred source (YOUTUBE, JIOSAAVN, LOCAL) distribution */
    val sourceDistribution: Map<String, Float> = emptyMap(),
    
    /** Total songs in history — indicates how much data we have */
    val totalSongsInHistory: Int = 0,
    
    /** Whether the user has enough history for personalization */
    val hasEnoughData: Boolean = false,

    // ---- Genre Vector Fields (Phase 2) ----

    /**
     * 20-dimension genre affinity vector built from full listening history.
     * Each dimension maps to a genre in [GenreTaxonomy.GENRES].
     * Values normalized to [0, 1].
     */
    val genreAffinityVector: FloatArray = FloatArray(GenreTaxonomy.GENRE_COUNT),

    /**
     * Genre vector built from only the last 10 songs — captures session/mood recency.
     * Enables "what I'm in the mood for right now" scoring.
     */
    val recentGenreVector: FloatArray = FloatArray(GenreTaxonomy.GENRE_COUNT),

    /**
     * Genre vector from frequently-skipped songs — negative signal.
     * Genres the user tends to skip get penalized during scoring.
     */
    val skipGenreVector: FloatArray = FloatArray(GenreTaxonomy.GENRE_COUNT)
) {
    // Custom equals/hashCode to handle FloatArray comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserTasteProfile) return false
        return artistAffinities == other.artistAffinities &&
                timeOfDayWeights == other.timeOfDayWeights &&
                avgCompletionRate == other.avgCompletionRate &&
                frequentlySkippedIds == other.frequentlySkippedIds &&
                likedSongIds == other.likedSongIds &&
                dislikedSongIds == other.dislikedSongIds &&
                likedArtists == other.likedArtists &&
                dislikedArtists == other.dislikedArtists &&
                recentSongIds == other.recentSongIds &&
                topPlayedSongIds == other.topPlayedSongIds &&
                sourceDistribution == other.sourceDistribution &&
                totalSongsInHistory == other.totalSongsInHistory &&
                hasEnoughData == other.hasEnoughData &&
                genreAffinityVector.contentEquals(other.genreAffinityVector) &&
                recentGenreVector.contentEquals(other.recentGenreVector) &&
                skipGenreVector.contentEquals(other.skipGenreVector)
    }

    override fun hashCode(): Int {
        var result = artistAffinities.hashCode()
        result = 31 * result + genreAffinityVector.contentHashCode()
        result = 31 * result + recentGenreVector.contentHashCode()
        result = 31 * result + skipGenreVector.contentHashCode()
        return result
    }
}
