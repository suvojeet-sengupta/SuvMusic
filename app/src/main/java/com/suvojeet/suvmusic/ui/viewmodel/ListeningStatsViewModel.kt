package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.local.dao.ArtistStats
import com.suvojeet.suvmusic.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.data.repository.ListeningHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ListeningStatsUiState(
    val totalSongsPlayed: Int = 0,
    val totalListeningTimeMs: Long = 0L,
    val topSongs: List<ListeningHistory> = emptyList(),
    val topArtists: List<ArtistStats> = emptyList(),
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
    
    private val _uiState = MutableStateFlow(ListeningStatsUiState())
    val uiState: StateFlow<ListeningStatsUiState> = _uiState.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Get basic stats
                val globalStats = listeningHistoryRepository.getListeningStats()
                
                // data flows
                val topSongsFlow = listeningHistoryRepository.getTopSongs(10)
                // Get history for analysis (last 30 days for broader trends, but we focus on 7 for chart)
                val recentHistoryFlow = listeningHistoryRepository.getHistoryForTimePeriod(30)
                
                // Combine flows to update UI state derived from these
                combine(
                    topSongsFlow,
                    recentHistoryFlow
                ) { topSongs, recentHistory ->
                    
                    val timeOfDayStats = calculateTimeOfDayStats(recentHistory)
                    val weeklyTrends = calculateWeeklyTrends(recentHistory)
                    val personality = determinePersonality(timeOfDayStats, globalStats.totalSongsPlayed)
                    
                    // Top Artists calculation from repository is a suspend function, so we call it outside or handle differently.
                    // Ideally modify repository to return Flow, but for now we can just fetch it once as it's less dynamic in real-time updates usually.
                    // However, 'combine' implies real-time. Let's launch a separate fetch for artists or just use what we have.
                    // For correctness with the flow, we should rely on the collected data if possible, but calculating top artists from raw history is expensive.
                    // We'll trust the repository's separate call for top artists for now and update it.
                    
                    Triple(topSongs, Triple(timeOfDayStats, weeklyTrends, personality), recentHistory)
                }.collect { (songs, stats, _) ->
                    val (timeOfDay, trends, personality) = stats
                    val artists = listeningHistoryRepository.getTopArtists(10) // Fetch fresh artists
                    
                    _uiState.update {
                        it.copy(
                            totalSongsPlayed = globalStats.totalSongsPlayed, // This might need a flow too for real-time, but acceptable for now
                            totalListeningTimeMs = globalStats.totalListeningTimeMs,
                            topSongs = songs,
                            topArtists = artists,
                            timeOfDayStats = timeOfDay,
                            weeklyTrends = trends,
                            musicPersonality = personality,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                // In production handle error properly
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun calculateTimeOfDayStats(history: List<ListeningHistory>): Map<TimeOfDay, Int> {
        val stats = mutableMapOf(
            TimeOfDay.MORNING to 0,
            TimeOfDay.AFTERNOON to 0,
            TimeOfDay.EVENING to 0,
            TimeOfDay.NIGHT to 0
        )
        
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
            stats[timeOfDay] = stats.getOrDefault(timeOfDay, 0) + 1
        }
        
        return stats
    }
    
    private fun calculateWeeklyTrends(history: List<ListeningHistory>): List<DailyListening> {
        val calendar = Calendar.getInstance()
        // Reset to start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val todayStart = calendar.timeInMillis
        val msPerDay = 24 * 60 * 60 * 1000L
        
        // last 7 days including today
        val trends = mutableListOf<DailyListening>()
        
        for (i in 6 downTo 0) {
            val dayStart = todayStart - (i * msPerDay)
            val dayEnd = dayStart + msPerDay
            
            // Filter listening within this day window
            // Note: This relies on 'lastPlayed'. Ideally we'd need separate play events, 
            // but 'ListeningHistory' aggregates by song. 
            // So we are approximating "active engagement" based on when they LAST played the song.
            // For a perfect chart we would need a 'PlaySession' entity, but for now this is a limitation we accept.
            // We can improve this if we assume 'lastPlayed' is the significant interaction.
            val playsOnDay = history.filter { 
                it.lastPlayed in dayStart until dayEnd 
            }
            
            val totalMinutes = playsOnDay.sumOf { it.duration } / 1000 / 60
            
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = dayStart
            val dayName = dayCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "?"
            
            trends.add(DailyListening(dayName, totalMinutes))
        }
        
        return trends
    }
    
    private fun determinePersonality(timeStats: Map<TimeOfDay, Int>, totalSongs: Int): MusicPersonality {
        if (totalSongs < 10) return MusicPersonality.NEWCOMER
        
        val topTime = timeStats.maxByOrNull { it.value }?.key ?: return MusicPersonality.NEWCOMER
        val totalTracked = timeStats.values.sum()
        
        // If they listen a lot in one session relative to total history
        // calculating "Binge Listener" is hard without session data, so we stick to Time of Day for now.
        
        return when (topTime) {
            TimeOfDay.MORNING -> MusicPersonality.EARLY_BIRD
            TimeOfDay.AFTERNOON -> MusicPersonality.WORKAHOLIC
            TimeOfDay.EVENING -> MusicPersonality.CHILL_VIBER
            TimeOfDay.NIGHT -> MusicPersonality.NIGHT_OWL
        }
    }
    
    fun refresh() {
        val current = _uiState.value
        if (!current.isLoading) {
             // Re-trigger load
             // In a real flow setup, we might not need this if we are observing database changes.
             // But 'loadStats' sets up the collectors again which is fine.
             loadStats()
        }
    }
}
