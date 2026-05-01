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
 * startup. Until that happens [isAvailable] is false (mirroring the
 * Desktop "no LibVLC" path) — UI surfaces this so the Play button shows
 * a hint instead of silently doing nothing.
 */
actual class MusicPlayer {
    actual val isAvailable: Boolean

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

    private var queueList: List<Song> = emptyList()
    private var canonicalIndex: Int = -1
    private var shuffleOrder: List<Int> = emptyList()
    private var shuffleCursor: Int = -1

    private val mainHandler = Handler(Looper.getMainLooper())
    private val exoPlayer: ExoPlayer?

    @OptIn(UnstableApi::class)
    private val positionTicker = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            if (player.isPlaying) {
                _positionMs.value = player.currentPosition
            }
            mainHandler.postDelayed(this, POSITION_TICK_MS)
        }
    }

    init {
        val ctx = appContext
        exoPlayer = if (ctx != null) {
            try {
                buildExoPlayer(ctx).also { player ->
                    player.addListener(buildListener())
                    mainHandler.post(positionTicker)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "ExoPlayer init failed: ${t.message}")
                null
            }
        } else {
            Log.w(TAG, "appContext not set — call MusicPlayer.setApplicationContext() at app startup. Playback disabled.")
            null
        }
        isAvailable = exoPlayer != null
    }

    actual fun setQueue(songs: List<Song>, startIndex: Int) {
        queueList = songs
        _queue.value = songs
        canonicalIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        _currentIndex.value = if (songs.isEmpty()) -1 else canonicalIndex
        regenerateShuffleOrder()
        loadCurrent(autoPlay = true)
    }

    actual fun playAt(index: Int) {
        if (index !in queueList.indices) return
        canonicalIndex = index
        regenerateShuffleOrder()
        loadCurrent(autoPlay = true)
    }

    actual fun play() {
        runOnMain { exoPlayer?.play() }
    }

    actual fun pause() {
        runOnMain { exoPlayer?.pause() }
    }

    actual fun togglePlayPause() {
        runOnMain {
            val player = exoPlayer ?: return@runOnMain
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    actual fun next() {
        if (queueList.isEmpty()) return
        val nextIndex = nextIndexInPlayOrder()
        if (nextIndex >= 0) {
            canonicalIndex = nextIndex
            loadCurrent(autoPlay = true)
        }
    }

    actual fun previous() {
        if (queueList.isEmpty()) return
        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = previousIndexInPlayOrder()
        if (prevIndex >= 0) {
            canonicalIndex = prevIndex
            loadCurrent(autoPlay = true)
        }
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
        regenerateShuffleOrder()
    }

    actual fun release() {
        runOnMain {
            mainHandler.removeCallbacks(positionTicker)
            exoPlayer?.release()
        }
    }

    private fun loadCurrent(autoPlay: Boolean) {
        val song = queueList.getOrNull(canonicalIndex)
        _currentSong.value = song
        _currentIndex.value = if (song == null) -1 else canonicalIndex
        _positionMs.value = 0L
        _durationMs.value = 0L
        if (song == null) return
        val mrl = song.streamUrl ?: song.localUri
        if (mrl.isNullOrBlank()) {
            Log.w(TAG, "No playable URL for ${song.title} — skipping load")
            return
        }
        runOnMain {
            val player = exoPlayer ?: return@runOnMain
            player.setMediaItem(MediaItem.fromUri(mrl))
            player.prepare()
            if (autoPlay) player.play()
        }
    }

    private fun onTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> loadCurrent(autoPlay = true)
            RepeatMode.ALL -> next()
            RepeatMode.OFF -> {
                val n = nextIndexInPlayOrder()
                if (n >= 0) {
                    canonicalIndex = n
                    loadCurrent(autoPlay = true)
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun nextIndexInPlayOrder(): Int {
        if (queueList.isEmpty()) return -1
        return if (_shuffleEnabled.value) {
            val nextCursor = shuffleCursor + 1
            when {
                nextCursor < shuffleOrder.size -> {
                    shuffleCursor = nextCursor
                    shuffleOrder[nextCursor]
                }
                _repeatMode.value == RepeatMode.ALL -> {
                    regenerateShuffleOrder()
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

    private fun previousIndexInPlayOrder(): Int {
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

    private fun regenerateShuffleOrder() {
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
            Log.w(TAG, "ExoPlayer error on ${_currentSong.value?.title}: ${error.message}")
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
         * Must be called once from `Application.onCreate()` before the
         * first [MusicPlayer] is constructed. The MusicPlayer expect
         * class is no-arg (KMP requirement); this is how we hand the
         * Android Application context to the actual.
         */
        fun setApplicationContext(context: Context) {
            appContext = context.applicationContext
        }
    }
}
