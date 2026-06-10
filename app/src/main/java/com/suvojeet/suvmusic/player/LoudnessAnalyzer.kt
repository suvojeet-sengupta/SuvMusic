package com.suvojeet.suvmusic.player

import com.suvojeet.suvmusic.data.SessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Spotify-style volume normalization.
 *
 * Tracks the running RMS of the currently playing song by sampling the
 * native engine's RMS readout, and persists the per-track average so we can
 * compute a stable per-track gain offset on subsequent plays. Computed in
 * [gainOffsetForRms] as `targetRms / measuredRms` expressed in dB and
 * clamped to a safe range so loud tracks aren't clipped and soft tracks
 * don't get pushed past the limiter's safety threshold.
 *
 * The analyzer is intentionally passive: it does not apply any gain
 * itself. [MusicPlayerService] reads [getCachedGainDb] when a song starts
 * and feeds it to [SpatialAudioProcessor.setLimiterConfig] as the
 * normalization makeup gain.
 */
@Singleton
class LoudnessAnalyzer @Inject constructor(
    private val nativeSpatialAudio: NativeSpatialAudio,
    private val sessionManager: SessionManager,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var samplingJob: Job? = null

    // Cached map; populated from SessionManager on first read and refreshed
    // when we commit a new measurement.
    @Volatile private var cache: Map<String, Float> = emptyMap()
    @Volatile private var cacheLoaded = false
    // Completed once the persisted cache has been read. Lets the first lookup
    // after cold start wait for real measurements instead of returning 0 dB.
    private val cacheReady = CompletableDeferred<Unit>()

    // Running stats for the song currently being measured.
    @Volatile private var currentSongId: String? = null
    @Volatile private var rmsAccumulator: Double = 0.0
    @Volatile private var rmsSampleCount: Int = 0
    @Volatile private var hasCommittedThisSong = false

    /**
     * Target perceived loudness (RMS amplitude in 0..1). 0.18 corresponds
     * roughly to -15 dBFS RMS, in the same ballpark as Spotify's -14 LUFS
     * normalization target. Conservative enough that the limiter rarely
     * needs to clamp peaks for typical mastered music.
     */
    private val targetRms = 0.18f

    /** Hard caps on the per-track gain so we never clip badly mastered audio. */
    private val maxGainDb = 6f
    private val minGainDb = -8f

    init {
        scope.launch {
            cache = sessionManager.getLoudnessCache()
            cacheLoaded = true
            cacheReady.complete(Unit)
        }
    }

    /**
     * Returns the per-track gain in dB to bring the cached RMS up to the
     * normalization target. Returns 0f if we have no measurement yet — the
     * caller should keep its baseline makeup gain in that case so the user
     * still hears a uniform volume across the catalogue once measurements
     * accumulate.
     */
    fun getCachedGainDb(songId: String?): Float {
        songId ?: return 0f
        val rms = cache[songId] ?: return 0f
        return gainOffsetForRms(rms)
    }

    /**
     * Cache-aware variant of [getCachedGainDb]. Suspends until the persisted
     * cache has loaded so the very first song after a cold start gets its real
     * normalization gain rather than 0 dB — otherwise that first track plays at
     * an audibly different level from every song that follows.
     */
    suspend fun getCachedGainDbAwait(songId: String?): Float {
        songId ?: return 0f
        cacheReady.await()
        val rms = cache[songId] ?: return 0f
        return gainOffsetForRms(rms)
    }

    fun gainOffsetForRms(rms: Float): Float {
        if (rms <= 0.0001f) return 0f
        val ratio = targetRms / rms
        val db = 20f * log10(ratio.toDouble()).toFloat()
        return min(maxGainDb, max(minGainDb, db))
    }

    /**
     * Begin tracking a song. Cancels any in-flight measurement for the
     * previous track. Safe to call repeatedly with the same id.
     */
    fun onSongStart(songId: String?) {
        if (songId == null) {
            stopMeasuring()
            return
        }
        if (currentSongId == songId) return

        // Commit the previous song's measurement (if any) before switching.
        commitPendingIfReady()

        currentSongId = songId
        rmsAccumulator = 0.0
        rmsSampleCount = 0
        hasCommittedThisSong = false

        // Skip measurement if we already have a cached value — saves CPU
        // and keeps the average stable across replays.
        if (cache.containsKey(songId)) {
            samplingJob?.cancel()
            samplingJob = null
            return
        }

        samplingJob?.cancel()
        samplingJob = scope.launch {
            try {
                // Skip the first second (intro / silence) where RMS is
                // biased low and would push the gain estimate too high.
                delay(1_000)
                while (true) {
                    val sample = try { nativeSpatialAudio.getRmsLevel() } catch (_: Exception) { 0f }
                    if (sample > 0.005f) {
                        rmsAccumulator += sample.toDouble().pow(2.0)
                        rmsSampleCount += 1
                    }
                    // Commit once we have ~30s of audio (300 samples @ 100ms).
                    if (rmsSampleCount >= 300 && !hasCommittedThisSong) {
                        commitPendingIfReady()
                    }
                    delay(100)
                }
            } catch (_: Exception) { /* job cancelled */ }
        }
    }

    fun onSongEnd() {
        commitPendingIfReady()
        stopMeasuring()
    }

    private fun stopMeasuring() {
        samplingJob?.cancel()
        samplingJob = null
        currentSongId = null
        rmsAccumulator = 0.0
        rmsSampleCount = 0
        hasCommittedThisSong = false
    }

    private fun commitPendingIfReady() {
        if (hasCommittedThisSong) return
        val id = currentSongId ?: return
        val n = rmsSampleCount
        // Need at least ~5s of valid samples (50 readings @ 100ms) before
        // we trust the average enough to publish.
        if (n < 50) return
        val meanSquared = rmsAccumulator / n
        val rms = kotlin.math.sqrt(meanSquared).toFloat()
        if (rms <= 0f || rms.isNaN()) return

        hasCommittedThisSong = true
        scope.launch {
            sessionManager.setLoudnessForSong(id, rms)
            cache = sessionManager.getLoudnessCache()
        }
    }
}
