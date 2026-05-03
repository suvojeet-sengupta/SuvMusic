package com.suvojeet.suvmusic.core.domain.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Android MusicPlayer — Media3 ExoPlayer-backed (Phase 4.2).
 *
 * Mirrors the Desktop VLCJ actual: pure-Kotlin queue/repeat/shuffle on
 * top, single-track playback below. ExoPlayer plays whatever URL is in
 * [Song.streamUrl] (resolved YouTube/JioSaavn streams) or [Song.localUri]
 * (local/downloaded files); URL resolution is upstream's problem.
 *
 * The richer Android player at `:app/.../player/MusicPlayer.kt` keeps
 * owning spatial audio / audio focus / MediaSession / BT autoplay for the
 * legacy `:app` UI — that work is not exposed through this expect surface.
 * This actual exists so the shared `composeApp` `App()` shell can play
 * audio on Android without depending on `:app`.
 *
 * ExoPlayer needs an Android [Context]; the expect class is no-arg, so
 * the Application calls [MusicPlayer.setApplicationContext] once at
 * startup. This is wired in `attachBaseContext` (earliest possible hook)
 * to avoid races where a [MusicPlayer] is constructed before [Application.onCreate]
 * runs (Compose previews, instrumentation tests, etc.).
 *
 * ExoPlayer is created lazily on first playback request, re-checking
 * [appContext] each time. A MusicPlayer constructed before the Application
 * was attached can therefore still recover once the context is set, instead
 * of being permanently dead.
 */
actual class MusicPlayer {
    actual val isAvailable: Boolean
        get() = appContext != null

    private val _currentSong = MutableStateFlow<Song?>(null)
    actual val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    actual val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    actual val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    actual val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    actual val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // queueState is mutated under `queueLock` so background callers and the
    // main-thread playback callbacks can't race on canonicalIndex/shuffleOrder.
    private val queueLock = Any()
    @Volatile private var queueList: List<Song> = emptyList()
    @Volatile private var canonicalIndex: Int = -1
    @Volatile private var shuffleOrder: List<Int> = emptyList()
    @Volatile private var shuffleCursor: Int = -1

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var exoPlayer: ExoPlayer? = null
    @Volatile private var listenerAttached: Boolean = false
    @Volatile private var tickerPosted: Boolean = false

    @OptIn(UnstableApi::class)
    private val positionTicker = object : Runnable {
        override fun run() {
            val player = exoPlayer
            if (player != null && player.isPlaying) {
                _positionMs.value = player.currentPosition
            }
            mainHandler.postDelayed(this, POSITION_TICK_MS)
        }
    }

    actual fun setQueue(songs: List<Song>, startIndex: Int) {
        synchronized(queueLock) {
            queueList = songs
            canonicalIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
            regenerateShuffleOrderLocked()
        }
        _queue.value = songs
        _currentIndex.value = if (songs.isEmpty()) -1 else canonicalIndex
        loadCurrent(autoPlay = true)
    }

    actual fun playAt(index: Int) {
        synchronized(queueLock) {
            if (index !in queueList.indices) return
            canonicalIndex = index
            regenerateShuffleOrderLocked()
        }
        loadCurrent(autoPlay = true)
    }

    actual fun play() {
        runOnMain { ensureExoPlayer()?.play() }
    }

    actual fun pause() {
        runOnMain { exoPlayer?.pause() }
    }

    actual fun togglePlayPause() {
        runOnMain {
            val player = ensureExoPlayer() ?: return@runOnMain
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    actual fun next() {
        val nextIndex = synchronized(queueLock) {
            if (queueList.isEmpty()) return
            nextIndexInPlayOrderLocked().also {
                if (it >= 0) canonicalIndex = it
            }
        }
        if (nextIndex >= 0) loadCurrent(autoPlay = true)
    }

    actual fun previous() {
        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = synchronized(queueLock) {
            if (queueList.isEmpty()) return
            previousIndexInPlayOrderLocked().also {
                if (it >= 0) canonicalIndex = it
            }
        }
        if (prevIndex >= 0) loadCurrent(autoPlay = true)
    }

    actual fun seekTo(positionMs: Long) {
        runOnMain {
            val player = exoPlayer
            if (player != null) {
                player.seekTo(positionMs)
            } else {
                _positionMs.value = positionMs
            }
        }
    }

    actual fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    actual fun setShuffleEnabled(enabled: Boolean) {
        if (_shuffleEnabled.value == enabled) return
        _shuffleEnabled.value = enabled
        synchronized(queueLock) { regenerateShuffleOrderLocked() }
    }

    actual fun release() {
        runOnMain {
            mainHandler.removeCallbacks(positionTicker)
            tickerPosted = false
            exoPlayer?.release()
            exoPlayer = null
            listenerAttached = false
        }
    }

    /** Clear the most recent error after the UI has surfaced it. */
    fun clearError() {
        _errorMessage.value = null
    }

    private fun loadCurrent(autoPlay: Boolean) {
        val (song, idx) = synchronized(queueLock) {
            queueList.getOrNull(canonicalIndex) to canonicalIndex
        }
        _currentSong.value = song
        _currentIndex.value = if (song == null) -1 else idx
        _positionMs.value = 0L
        _durationMs.value = 0L
        if (song == null) return
        val mrl = song.streamUrl ?: song.localUri
        if (mrl.isNullOrBlank()) {
            val msg = "No playable URL for \"${song.title}\". Stream may not be resolved yet."
            Log.w(TAG, "$msg — skipping load")
            _errorMessage.value = msg
            _isPlaying.value = false
            return
        }
        runOnMain {
            val player = ensureExoPlayer()
            if (player == null) {
                _errorMessage.value = "Audio engine unavailable. Restart the app and try again."
                return@runOnMain
            }
            player.setMediaItem(MediaItem.fromUri(mrl))
            player.prepare()
            if (autoPlay) player.play()
        }
    }

    private fun onTrackEnded() {
        val target = synchronized(queueLock) {
            when (_repeatMode.value) {
                RepeatMode.ONE -> canonicalIndex
                RepeatMode.ALL -> nextIndexInPlayOrderLocked().also { if (it >= 0) canonicalIndex = it }
                RepeatMode.OFF -> nextIndexInPlayOrderLocked().also { if (it >= 0) canonicalIndex = it }
            }
        }
        if (target >= 0) {
            loadCurrent(autoPlay = true)
        } else {
            _isPlaying.value = false
        }
    }

    private fun nextIndexInPlayOrderLocked(): Int {
        if (queueList.isEmpty()) return -1
        return if (_shuffleEnabled.value) {
            val nextCursor = shuffleCursor + 1
            when {
                nextCursor < shuffleOrder.size -> {
                    shuffleCursor = nextCursor
                    shuffleOrder[nextCursor]
                }
                _repeatMode.value == RepeatMode.ALL -> {
                    regenerateShuffleOrderLocked()
                    shuffleCursor = 0
                    shuffleOrder.firstOrNull() ?: -1
                }
                else -> -1
            }
        } else {
            val candidate = canonicalIndex + 1
            if (candidate < queueList.size) candidate
            else if (_repeatMode.value == RepeatMode.ALL) 0 else -1
        }
    }

    private fun previousIndexInPlayOrderLocked(): Int {
        if (queueList.isEmpty()) return -1
        return if (_shuffleEnabled.value) {
            val prevCursor = shuffleCursor - 1
            if (prevCursor >= 0) {
                shuffleCursor = prevCursor
                shuffleOrder[prevCursor]
            } else -1
        } else {
            val candidate = canonicalIndex - 1
            if (candidate >= 0) candidate else -1
        }
    }

    private fun regenerateShuffleOrderLocked() {
        if (!_shuffleEnabled.value || queueList.isEmpty()) {
            shuffleOrder = emptyList()
            shuffleCursor = -1
            return
        }
        val others = queueList.indices.filter { it != canonicalIndex }.toMutableList()
        others.shuffle(Random(System.nanoTime()))
        shuffleOrder = listOf(canonicalIndex) + others
        shuffleCursor = 0
    }

    @OptIn(UnstableApi::class)
    private fun ensureExoPlayer(): ExoPlayer? {
        exoPlayer?.let { return it }
        val ctx = appContext ?: return null
        return synchronized(this) {
            exoPlayer ?: try {
                buildExoPlayer(ctx).also { player ->
                    if (!listenerAttached) {
                        player.addListener(buildListener())
                        listenerAttached = true
                    }
                    if (!tickerPosted) {
                        mainHandler.post(positionTicker)
                        tickerPosted = true
                    }
                    exoPlayer = player
                }
            } catch (t: Throwable) {
                Log.w(TAG, "ExoPlayer init failed: ${t.message}")
                _errorMessage.value = "Audio engine init failed: ${t.message}"
                null
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildExoPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context.applicationContext).build()
    }

    private fun buildListener(): Player.Listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            if (playbackState == Player.STATE_READY) {
                val dur = player.duration
                _durationMs.value = if (dur > 0) dur else 0L
            }
            if (playbackState == Player.STATE_ENDED) {
                onTrackEnded()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val title = _currentSong.value?.title ?: "track"
            Log.w(TAG, "ExoPlayer error on $title: ${error.message}")
            _errorMessage.value = "Playback failed: ${error.message ?: "unknown error"}"
            _isPlaying.value = false
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    companion object {
        private const val TAG = "MusicPlayer.android"
        private const val POSITION_TICK_MS = 250L

        @Volatile
        private var appContext: Context? = null

        /**
         * Must be called from `Application.attachBaseContext()` (earliest hook)
         * before any [MusicPlayer] is constructed. The MusicPlayer expect class
         * is no-arg (KMP requirement); this is how we hand the Android
         * Application context to the actual.
         *
         * Calling this after a MusicPlayer is already constructed is safe —
         * the player creates its ExoPlayer lazily on first playback request and
         * will pick up the context at that point.
         */
        fun setApplicationContext(context: Context) {
            appContext = context.applicationContext
        }
    }
}
