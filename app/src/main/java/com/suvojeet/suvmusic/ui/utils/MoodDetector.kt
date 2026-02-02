package com.suvojeet.suvmusic.ui.utils

sealed class LyricsStyle {
    object Standard : LyricsStyle()
    object Energetic : LyricsStyle()
    object Chill : LyricsStyle()
    object Sad : LyricsStyle()
    object Happy : LyricsStyle()
    object Romantic : LyricsStyle()
}

object MoodDetector {
    
    fun detectStyle(title: String, artist: String, lyricsText: String): LyricsStyle {
        val lowerLyrics = lyricsText.lowercase()
        val lowerTitle = title.lowercase()
        
        // 1. Check for Energetic keywords
        if (containsAny(lowerLyrics, "dance", "party", "jump", "scream", "loud", "beat", "rhythm", "rock", "crazy")) {
            return LyricsStyle.Energetic
        }
        
        // 2. Check for Sad keywords
        if (containsAny(lowerLyrics, "tears", "cry", "lonely", "miss you", "sad", "pain", "broken", "gone", "die", "hurt")) {
            return LyricsStyle.Sad
        }
        
        // 3. Check for Romantic keywords
        if (containsAny(lowerLyrics, "love", "baby", "heart", "kiss", "forever", "together", "darling")) {
            return LyricsStyle.Romantic
        }
        
        // 4. Check for Chill/Relaxed (Fallback to keywords or specific artists?)
        if (containsAny(lowerLyrics, "sleep", "dream", "sky", "float", "slow", "easy", "calm")) {
            return LyricsStyle.Chill
        }
        
        return LyricsStyle.Standard
    }
    
    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
