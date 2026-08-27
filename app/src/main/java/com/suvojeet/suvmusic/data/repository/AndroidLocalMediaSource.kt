package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.core.domain.repository.LocalMediaSource
import com.suvojeet.suvmusic.core.model.Song
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter over the production MediaStore implementation. */
@Singleton
class AndroidLocalMediaSource @Inject constructor(
    private val repository: LocalAudioRepository,
) : LocalMediaSource {
    override suspend fun getAllLocalSongs(): List<Song> = repository.getAllLocalSongs()
    override suspend fun searchLocalSongs(query: String): List<Song> = repository.searchLocalSongs(query)
}
