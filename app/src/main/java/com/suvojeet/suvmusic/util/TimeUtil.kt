package com.suvojeet.suvmusic.util

import com.suvojeet.suvmusic.core.model.Song
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
        if (minutes > 0 || hours > 0) {
            sb.append("${minutes}min ")
        }
        sb.append("${seconds}sec")

        return sb.toString().trim()
    }

    /**
     * Formats duration in milliseconds to "mm:ss" or "hh:mm:ss"
     */
    fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
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
