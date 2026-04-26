package com.suvojeet.suvmusic.core.domain.usecase

import com.suvojeet.suvmusic.core.domain.repository.PlaybackRepository
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Use case for resolving a streamable URL for a song.
 */
class ResolveStreamUrlUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : UseCase<Song, String?> {
    override suspend operator fun invoke(song: Song): String? {
        return playbackRepository.getStreamUrl(song)
    }
}

/**
 * Use case for retrieving lyrics for a song.
 */
class GetLyricsUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : UseCase<Song, String?> {
    override suspend operator fun invoke(song: Song): String? {
        return playbackRepository.getLyrics(song)
    }
}

/**
 * Use case for retrieving recently played songs.
 */
class GetRecentlyPlayedUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : ParameterlessUseCase<Flow<List<Song>>> {
    override suspend operator fun invoke(): Flow<List<Song>> {
        return playbackRepository.getRecentlyPlayed()
    }
}

/**
 * Use case for retrieving recommended songs.
 */
class GetRecommendedSongsUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : ParameterlessUseCase<List<Song>> {
    override suspend operator fun invoke(): List<Song> {
        return playbackRepository.getRecommendedSongs()
    }
}

/**
 * Use case for retrieving songs in a queue.
 */
class GetQueueSongsUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : UseCase<String, List<Song>> {
    override suspend operator fun invoke(queueId: String): List<Song> {
        return playbackRepository.getQueueSongs(queueId)
    }
}

/**
 * Use case for adding songs to the playback queue.
 */
class AddToQueueUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : ConsumerUseCase<List<Song>> {
    override suspend operator fun invoke(songs: List<Song>) {
        playbackRepository.addToQueue(songs)
    }
}

/**
 * Use case for marking a song as played (for listening history).
 */
class MarkSongAsPlayedUseCase constructor(
    private val playbackRepository: PlaybackRepository
) : ConsumerUseCase<Song> {
    override suspend operator fun invoke(song: Song) {
        playbackRepository.markAsPlayed(song)
    }
}
