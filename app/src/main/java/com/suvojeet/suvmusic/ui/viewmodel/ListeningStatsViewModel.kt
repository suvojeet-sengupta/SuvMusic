package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.data.local.dao.ArtistStats
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ListeningStatsUiState(
    val totalSongsPlayed: Int = 0,
    val totalListeningTimeMs: Long = 0L,
    val averageDailyMs: Long = 0L,
    val topSongs: List<ListeningHistory> = emptyList(),
    val topArtists: List<ArtistStats> = emptyList(),
    val topArtistThisMonth: ArtistStats? = null,
    val timeOfDayStats: Map<TimeOfDay, Int> = emptyMap(), // Count of songs played per time of day
    val weeklyTrends: List<DailyListening> = emptyList(), // Last 7 days listening time
    val musicPersonality: MusicPersonality = MusicPersonality.NEWCOMER,
    val isLoading: Boolean = true
)

enum class TimeOfDay {
    MORNING,   // 5-11
    AFTERNOON, // 12-16
    EVENING,   // 17-21
    NIGHT      // 22-4
}

enum class MusicPersonality(val title: String, val description: String) {
    EARLY_BIRD("Early Bird", "You love starting your day with music."),
    NIGHT_OWL("Night Owl", "Your best vibes come out after dark."),
    WORKAHOLIC("Focus Master", "Music keeps you in the zone during the day."),
    CHILL_VIBER("Chill Viber", "You enjoy relaxing evenings with tunes."),
    BINGE_LISTENER("Binge Listener", "Once you start, you just can't stop."),
    NEWCOMER("Newcomer", "Just getting started on your musical journey.")
}

data class DailyListening(
    val dayName: String, // Mon, Tue...
    val minutesListen: Long
)

@HiltViewModel
class ListeningStatsViewModel @Inject constructor(
    private val listeningHistoryRepository: ListeningHistoryRepository
) : ViewModel() {
    
    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<ListeningStatsUiState> = refreshTrigger
        .flatMapLatest {
            combine(
                listeningHistoryRepository.getTopSongs(10),
                listeningHistoryRepository.getHistoryForTimePeriod(30)
            ) { topSongs, recentHistory ->
                val globalStats = listeningHistoryRepository.getListeningStats()
                val topArtists = listeningHistoryRepository.getTopArtists(10)
                
                val timeOfDayStats = calculateTimeOfDayStats(recentHistory)
                val weeklyTrends = calculateWeeklyTrends(recentHistory)
                
                val avgDaily = if (weeklyTrends.isNotEmpty()) {
                    weeklyTrends.map { it.minutesListen }.average().toLong() * 60 * 1000
                } else 0L
                
                val personality = determinePersonality(timeOfDayStats, globalStats.totalSongsPlayed, avgDaily)
                
                val monthTopArtist = calculateTopArtistFromHistory(recentHistory)
                
                ListeningStatsUiState(
                    totalSongsPlayed = globalStats.totalSongsPlayed,
                    totalListeningTimeMs = globalStats.totalListeningTimeMs,
                    averageDailyMs = avgDaily,
                    topSongs = topSongs,
                    topArtists = topArtists,
                    topArtistThisMonth = monthTopArtist,
                    timeOfDayStats = timeOfDayStats,
                    weeklyTrends = weeklyTrends,
                    musicPersonality = personality,
                    isLoading = false
                )
            }.catch { e ->
                // Log error or handle it
                emit(ListeningStatsUiState(isLoading = false))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListeningStatsUiState(isLoading = true)
        )
    
    private fun calculateTopArtistFromHistory(history: List<ListeningHistory>): ArtistStats? {
        if (history.isEmpty()) return null
        return history.groupBy { it.artist }
            .map { (artist, songs) -> 
                ArtistStats(artist, songs.sumOf { it.playCount }) 
            }
            .maxByOrNull { it.totalPlays }
    }
    
    private fun calculateTimeOfDayStats(history: List<ListeningHistory>): Map<TimeOfDay, Int> {
        val stats = TimeOfDay.entries.associateWith { 0 }.toMutableMap()
        val calendar = Calendar.getInstance()
        
        history.forEach { item ->
            calendar.timeInMillis = item.lastPlayed
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            val timeOfDay = when (hour) {
                in 5..11 -> TimeOfDay.MORNING
                in 12..16 -> TimeOfDay.AFTERNOON
                in 17..21 -> TimeOfDay.EVENING
                else -> TimeOfDay.NIGHT
            }
            stats[timeOfDay] = (stats[timeOfDay] ?: 0) + 1
        }
        
        return stats
    }
    
    private fun calculateWeeklyTrends(history: List<ListeningHistory>): List<DailyListening> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val todayStart = calendar.timeInMillis
        val msPerDay = 24 * 60 * 60 * 1000L
        
        return (6 downTo 0).map { i ->
            val dayStart = todayStart - (i * msPerDay)
            val dayEnd = dayStart + msPerDay
            
            val playsOnDay = history.filter { it.lastPlayed in dayStart until dayEnd }
            val totalMinutes = playsOnDay.sumOf { it.duration } / 1000 / 60
            
            val dayCal = Calendar.getInstance().apply { timeInMillis = dayStart }
            val dayName = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "?"
            
            DailyListening(dayName, totalMinutes)
        }
    }
    
    private fun determinePersonality(timeStats: Map<TimeOfDay, Int>, totalSongs: Int, avgDailyMs: Long): MusicPersonality {
        if (totalSongs < 10) return MusicPersonality.NEWCOMER
        
        // If average daily listening is more than 2 hours
        if (avgDailyMs > 2 * 60 * 60 * 1000L) return MusicPersonality.BINGE_LISTENER
        
        val topTime = timeStats.maxByOrNull { it.value }?.key ?: return MusicPersonality.NEWCOMER
        
        return when (topTime) {
            TimeOfDay.MORNING -> MusicPersonality.EARLY_BIRD
            TimeOfDay.AFTERNOON -> MusicPersonality.WORKAHOLIC
            TimeOfDay.EVENING -> MusicPersonality.CHILL_VIBER
            TimeOfDay.NIGHT -> MusicPersonality.NIGHT_OWL
        }
    }
    
    fun refresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }
}
