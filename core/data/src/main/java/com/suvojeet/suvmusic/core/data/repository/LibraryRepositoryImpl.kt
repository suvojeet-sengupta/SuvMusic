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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao
) : LibraryRepository {

    override suspend fun savePlaylist(playlist: Playlist) {
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

    override suspend fun savePlaylistSongs(playlistId: String, songs: List<Song>) {
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
                localUri = song.localUri?.toString(),
                order = index
            )
        }
        libraryDao.deletePlaylistSongs(playlistId)
        libraryDao.insertPlaylistSongs(entities)
    }

    override suspend fun appendPlaylistSongs(playlistId: String, songs: List<Song>, startOrder: Int) {
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
                localUri = song.localUri?.toString(),
                order = startOrder + index
            )
        }
        libraryDao.insertPlaylistSongs(entities)
    }

    override suspend fun getCachedPlaylistSongs(playlistId: String): List<Song> {
        return libraryDao.getPlaylistSongs(playlistId).map { entity ->
            Song(
                id = entity.songId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album ?: "",
                thumbnailUrl = entity.thumbnailUrl,
                duration = entity.duration,
                source = try { SongSource.valueOf(entity.source) } catch (e: Exception) { SongSource.YOUTUBE },
                localUri = entity.localUri?.let { android.net.Uri.parse(it) }
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
                    source = try { SongSource.valueOf(entity.source) } catch (e: Exception) { SongSource.YOUTUBE },
                    localUri = entity.localUri?.let { android.net.Uri.parse(it) }
                )
            }
        }
    }

    override fun getPlaylistSongCountFlow(playlistId: String): Flow<Int> {
        return libraryDao.getPlaylistSongCountFlow(playlistId)
    }

    override suspend fun replacePlaylistSongs(playlistId: String, songs: List<Song>) {
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
                localUri = song.localUri?.toString(),
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

    override fun getSavedPlaylists(): Flow<List<LibraryItem>> {
        return libraryDao.getItemsByType("PLAYLIST").map { list ->
            list.map { it.toDomainModel() }
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
            subtitle = subtitle,
            thumbnailUrl = thumbnailUrl,
            type = try { LibraryItemType.valueOf(type) } catch(e: Exception) { LibraryItemType.UNKNOWN },
            timestamp = timestamp
        )
    }
}
