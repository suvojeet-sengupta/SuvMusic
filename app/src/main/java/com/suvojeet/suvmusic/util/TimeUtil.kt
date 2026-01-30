package com.suvojeet.suvmusic.util

import com.suvojeet.suvmusic.data.model.Song
import java.util.concurrent.TimeUnit

object TimeUtil {

    /**
     * Formats duration in milliseconds to a verbose string like "3hrs 21min 32sec".
     */
    fun formatDurationVerbose(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        val sb = StringBuilder()
        if (hours > 0) {
            sb.append("${hours}hrs ")
        }
        if (minutes > 0 || hours > 0) { // Show minutes if there are hours, even if 0? classic format usually skips if 0, but user example "3hrs 21min" implies standard components.
            // if hours > 0 and minutes == 0, maybe just "3hrs 0min"? Or skip?
            // User example: "3hrs 21min 32sec".
            // Let's stick to appending if > 0 for now to keep it clean, or exactly as requested.
            // "3hrs 21min 32sec"
            sb.append("${minutes}min ")
        }
        sb.append("${seconds}sec")

        return sb.toString().trim()
    }

    /**
     * Calculates total duration of songs and returns a formatted string with count.
     * Example: "48 songs • 3hrs 21min 32sec"
     */
    fun formatSongCountAndDuration(songs: List<Song>): String {
        val count = songs.size
        if (count == 0) return "0 songs"
        
        val totalDuration = songs.fold(0L) { acc, song -> acc + song.duration }
        val durationString = formatDurationVerbose(totalDuration)
        
        val songString = if (count == 1) "song" else "songs"
        
        return "$count $songString • $durationString"
    }
}
