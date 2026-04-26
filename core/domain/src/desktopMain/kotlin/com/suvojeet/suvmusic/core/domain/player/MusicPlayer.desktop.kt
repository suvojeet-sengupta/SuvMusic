package com.suvojeet.suvmusic.core.domain.player

import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import kotlin.random.Random

/**
 * Desktop MusicPlayer — VLCJ-backed (Phase 4.1).
 *
 * Audio backend: LibVLC via VLCJ. Plays whatever URL is in
 * [Song.streamUrl] (for already-resolved YouTube/JioSaavn streams) or
 * [Song.localUri] (for local/downloaded files). YouTube extraction itself
 * is NOT done here — that's the job of upstream code preparing the Song
 * (the existing :extractor module does this on Android; equivalent for
 * Desktop arrives in Phase 4.3 alongside the consumer rewiring).
 *
 * Queue / repeat / shuffle are managed in pure Kotlin; VLCJ is only used
 * for single-track playback. Track transitions (next/previous, end-of-
 * track auto-advance) happen here, then the next track URL is handed to
 * VLCJ.
 *
 * LibVLC discovery: relies on [NativeDiscovery] finding a system-installed
 * VLC. If LibVLC isn't found at construction time, the class logs a
 * warning and behaves like the chunk-4.0 stub (state flows still emit but
 * nothing actually plays). Lets the desktop window run end-to-end even on
 * machines without VLC, so the rest of the UI is testable.
 */
actual class MusicPlayer {
    // --- Public state flows --------------------------------------------------

    /**
     * Set in the init block once VLCJ wiring completes. Stays false on
     * machines without VLC media player installed (LibVLC missing).
     */
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

    // --- Queue management (pure Kotlin) -------------------------------------

    private var queueList: List<Song> = emptyList()

    /**
     * Index into [queue] in linear order. -1 when no queue is loaded.
     * Always reflects the canonical track regardless of shuffle.
     */
    private var canonicalIndex: Int = -1

    /**
     * When shuffle is on, this is the order in which tracks play. Each
     * entry is an index into [queue]. Regenerated whenever the queue or
     * the shuffle flag changes; preserves the current track at position 0.
     */
    private var shuffleOrder: List<Int> = emptyList()

    /** Position within [shuffleOrder] when shuffle is on; mirrors [canonicalIndex] when off. */
    private var shuffleCursor: Int = -1

    // --- VLCJ wiring --------------------------------------------------------

    private val factory: MediaPlayerFactory? = try {
        if (NativeDiscovery().discover()) {
            MediaPlayerFactory()
        } else {
            log("LibVLC not found via NativeDiscovery — playback disabled. Install VLC media player.")
            null
        }
    } catch (t: Throwable) {
        log("MediaPlayerFactory creation failed: ${t.message} — playback disabled.")
        null
    }

    private val mediaPlayer: MediaPlayer? = factory?.mediaPlayers()?.newMediaPlayer()?.also { player ->
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                _isPlaying.value = true
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                _positionMs.value = newTime
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                _durationMs.value = newLength
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                // Auto-advance honours repeat mode.
                onTrackEnded()
            }

            override fun error(mediaPlayer: MediaPlayer) {
                log("VLCJ playback error on ${_currentSong.value?.title}")
                _isPlaying.value = false
            }
        })
    }

    private val isVlcReady: Boolean get() = mediaPlayer != null

    init {
        isAvailable = isVlcReady
    }

    // --- Public API ---------------------------------------------------------

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
        // Re-anchor the shuffle order on the new track so future next/prev
        // pull from a fresh order starting here.
        regenerateShuffleOrder()
        loadCurrent(autoPlay = true)
    }

    actual fun play() {
        if (!isVlcReady) return
        mediaPlayer?.controls()?.play()
    }

    actual fun pause() {
        if (!isVlcReady) return
        mediaPlayer?.controls()?.pause()
    }

    actual fun togglePlayPause() {
        if (!isVlcReady) return
        if (_isPlaying.value) pause() else play()
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
        // Mirrors typical behaviour: if past 3s, restart current track instead of jumping back.
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
        if (!isVlcReady) {
            _positionMs.value = positionMs
            return
        }
        mediaPlayer?.controls()?.setTime(positionMs)
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
        mediaPlayer?.release()
        factory?.release()
    }

    // --- Internals ---------------------------------------------------------

    private fun loadCurrent(autoPlay: Boolean) {
        val song = queueList.getOrNull(canonicalIndex)
        _currentSong.value = song
        _currentIndex.value = if (song == null) -1 else canonicalIndex
        _positionMs.value = 0L
        _durationMs.value = 0L
        if (song == null) return
        val mrl = song.streamUrl ?: song.localUri
        if (mrl.isNullOrBlank()) {
            log("No playable URL for ${song.title} — skipping load")
            return
        }
        if (!isVlcReady) return
        mediaPlayer?.media()?.let { media ->
            if (autoPlay) media.play(mrl) else media.prepare(mrl)
        }
    }

    private fun onTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Replay current track from the start.
                loadCurrent(autoPlay = true)
            }
            RepeatMode.ALL -> {
                next() // wraps automatically because nextIndexInPlayOrder loops in ALL mode
            }
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
        if (_shuffleEnabled.value) {
            val nextCursor = shuffleCursor + 1
            return when {
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
            return if (candidate < queueList.size) candidate
            else if (_repeatMode.value == RepeatMode.ALL) 0 else -1
        }
    }

    private fun previousIndexInPlayOrder(): Int {
        if (queueList.isEmpty()) return -1
        if (_shuffleEnabled.value) {
            val prevCursor = shuffleCursor - 1
            return if (prevCursor >= 0) {
                shuffleCursor = prevCursor
                shuffleOrder[prevCursor]
            } else -1
        } else {
            val candidate = canonicalIndex - 1
            return if (candidate >= 0) candidate else -1
        }
    }

    private fun regenerateShuffleOrder() {
        if (!_shuffleEnabled.value || queue.isEmpty()) {
            shuffleOrder = emptyList()
            shuffleCursor = -1
            return
        }
        val others = queue.indices.filter { it != canonicalIndex }.toMutableList()
        others.shuffle(Random(System.nanoTime()))
        shuffleOrder = listOf(canonicalIndex) + others
        shuffleCursor = 0
    }

    private fun log(message: String) {
        println("[MusicPlayer.vlcj] $message")
    }
}
