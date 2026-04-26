package com.suvojeet.suvmusic.core.data.repository

import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.entity.LibraryEntity
import com.suvojeet.suvmusic.core.data.local.entity.PlaylistSongEntity
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.LibraryItem
import com.suvojeet.suvmusic.core.model.LibraryItemType
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao
) : LibraryRepository {

    override suspend fun savePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        val entity = LibraryEntity(
            id = playlist.id,
            title = playlist.title,
            subtitle = playlist.author,
            thumbnailUrl = playlist.thumbnailUrl,
            type = "PLAYLIST"
        )
        libraryDao.insertItem(entity)
        
        if (playlist.songs.isNotEmpty()) {
            savePlaylistSongs(playlist.id, playlist.songs)
        }
    }

    override suspend fun savePlaylistSongs(playlistId: String, songs: List<Song>) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val entities = songs.mapIndexed { index, song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                source = song.source.name,
                localUri = song.localUri,
                releaseDate = song.releaseDate,
                addedAt = if (song.addedAt > 0) song.addedAt else currentTime + index,
                order = index
            )
        }
        libraryDao.replacePlaylistSongs(playlistId, entities)
    }

    override suspend fun appendPlaylistSongs(playlistId: String, songs: List<Song>, startOrder: Int) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val entities = songs.mapIndexed { index, song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                source = song.source.name,
                localUri = song.localUri,
                releaseDate = song.releaseDate,
                addedAt = if (song.addedAt > 0) song.addedAt else currentTime + index,
                order = startOrder + index
            )
        }
        libraryDao.insertPlaylistSongs(entities)
    }

    override suspend fun getCachedPlaylistSongs(playlistId: String): List<Song> = withContext(Dispatchers.IO) {
        return@withContext libraryDao.getPlaylistSongs(playlistId).map { entity ->
            Song(
                id = entity.songId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album ?: "",
                thumbnailUrl = entity.thumbnailUrl,
                duration = entity.duration,
                source = try { SongSource.valueOf(entity.source) } catch (e: IllegalArgumentException) { SongSource.YOUTUBE },
                localUri = entity.localUri,
                releaseDate = entity.releaseDate,
                addedAt = entity.addedAt
            )
        }
    }

    override fun getCachedPlaylistSongsFlow(playlistId: String): Flow<List<Song>> {
        return libraryDao.getPlaylistSongsFlow(playlistId).map { entities ->
            entities.map { entity ->
                Song(
                    id = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album ?: "",
                    thumbnailUrl = entity.thumbnailUrl,
                    duration = entity.duration,
                    source = try { SongSource.valueOf(entity.source) } catch (e: IllegalArgumentException) { SongSource.YOUTUBE },
                    localUri = entity.localUri,
                    releaseDate = entity.releaseDate,
                    addedAt = entity.addedAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getPlaylistSongCountFlow(playlistId: String): Flow<Int> {
        return libraryDao.getPlaylistSongCountFlow(playlistId).flowOn(Dispatchers.IO)
    }

    override suspend fun isSongInPlaylist(playlistId: String, songId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext libraryDao.isSongInPlaylist(playlistId, songId)
    }

    override suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailUrl: String?) = withContext(Dispatchers.IO) {
        libraryDao.updatePlaylistThumbnail(playlistId, thumbnailUrl)
    }

    override suspend fun updatePlaylistName(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        libraryDao.updatePlaylistName(playlistId, name)
    }

    override suspend fun replacePlaylistSongs(playlistId: String, songs: List<Song>) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val entities = songs.mapIndexed { index, song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                source = song.source.name,
                localUri = song.localUri,
                releaseDate = song.releaseDate,
                addedAt = if (song.addedAt > 0) song.addedAt else currentTime + index,
                order = index
            )
        }
        libraryDao.replacePlaylistSongs(playlistId, entities)
    }

    override suspend fun removePlaylist(playlistId: String) {
        libraryDao.deleteItem(playlistId)
        libraryDao.deletePlaylistSongs(playlistId)
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        libraryDao.deleteSongFromPlaylist(playlistId, songId)
    }

    override suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        val currentSongs = getCachedPlaylistSongs(playlistId)
        appendPlaylistSongs(playlistId, listOf(song), currentSongs.size)
    }

    override suspend fun saveAlbum(album: Album) {
        val entity = LibraryEntity(
            id = album.id,
            title = album.title,
            subtitle = album.artist,
            thumbnailUrl = album.thumbnailUrl,
            type = "ALBUM"
        )
        libraryDao.insertItem(entity)
    }

    override suspend fun removeAlbum(albumId: String) {
        libraryDao.deleteItem(albumId)
    }

    override fun isPlaylistSaved(playlistId: String): Flow<Boolean> {
        return libraryDao.isItemSavedFlow(playlistId)
    }

    override fun isAlbumSaved(albumId: String): Flow<Boolean> {
        return libraryDao.isItemSavedFlow(albumId)
    }

    override suspend fun getPlaylistById(id: String): LibraryItem? {
        return libraryDao.getItem(id)?.toDomainModel()
    }

    override fun getSavedPlaylists(): Flow<List<com.suvojeet.suvmusic.core.model.PlaylistDisplayItem>> {
        return libraryDao.getItemsWithTypeAndCount("PLAYLIST").map { list ->
            list.map { entity ->
                com.suvojeet.suvmusic.core.model.PlaylistDisplayItem(
                    id = entity.id,
                    name = entity.title,
                    url = "https://music.youtube.com/playlist?list=${entity.id}",
                    uploaderName = entity.subtitle ?: "",
                    thumbnailUrl = entity.thumbnailUrl,
                    songCount = entity.songCount
                )
            }
        }
    }

    override fun getSavedAlbums(): Flow<List<LibraryItem>> {
        return libraryDao.getItemsByType("ALBUM").map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun saveArtist(artist: Artist) {
        val entity = LibraryEntity(
            id = artist.id,
            title = artist.name,
            subtitle = artist.subscribers ?: "",
            thumbnailUrl = artist.thumbnailUrl,
            type = "ARTIST"
        )
        libraryDao.insertItem(entity)
    }

    override suspend fun removeArtist(artistId: String) {
        libraryDao.deleteItem(artistId)
    }

    override fun getSavedArtists(): Flow<List<LibraryItem>> {
        return libraryDao.getItemsByType("ARTIST").map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun isArtistSaved(artistId: String): Flow<Boolean> {
        return libraryDao.isItemSavedFlow(artistId)
    }
    
    private fun LibraryEntity.toDomainModel(): LibraryItem {
        return LibraryItem(
            id = id,
            title = title,
            subtitle = subtitle ?: "",
            thumbnailUrl = thumbnailUrl,
            type = try { LibraryItemType.valueOf(type) } catch(e: IllegalArgumentException) { LibraryItemType.UNKNOWN },
            timestamp = timestamp
        )
    }
}
