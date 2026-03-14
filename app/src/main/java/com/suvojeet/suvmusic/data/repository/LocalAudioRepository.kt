package com.suvojeet.suvmusic.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.suvojeet.suvmusic.core.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing local audio files.
 */
@Singleton
class LocalAudioRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    companion object {
        private val AUDIO_PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        private val ALBUM_PROJECTION = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        private val ARTIST_PROJECTION = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
        )
    }
    
    /**
     * Get all local audio files with custom sorting.
     */
    suspend fun getAllLocalSongs(sortBy: String = MediaStore.Audio.Media.TITLE): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "$sortBy ASC"
        
        context.contentResolver.query(
            collection,
            AUDIO_PROJECTION,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val path = cursor.getString(dataColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                songs.add(
                    Song.fromLocal(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        albumArtUri = albumArtUri,
                        contentUri = contentUri
                    ).copy(customFolderPath = path?.substringBeforeLast("/"))
                )
            }
        }
        
        songs
    }

    /**
     * Get all local albums.
     */
    suspend fun getAllLocalAlbums(): List<com.suvojeet.suvmusic.core.model.Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<com.suvojeet.suvmusic.core.model.Album>()
        
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"
        
        context.contentResolver.query(
            collection,
            ALBUM_PROJECTION,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumTitle = cursor.getString(albumColumn) ?: "Unknown Album"
                val artistName = cursor.getString(artistColumn) ?: "Unknown Artist"
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    id
                )
                
                albums.add(
                    com.suvojeet.suvmusic.core.model.Album(
                        id = id.toString(),
                        title = albumTitle,
                        artist = artistName,
                        thumbnailUrl = albumArtUri.toString(),
                        year = null,
                        songs = emptyList()
                    )
                )
            }
        }
        albums
    }

    /**
     * Get all local artists.
     */
    suspend fun getAllLocalArtists(): List<com.suvojeet.suvmusic.core.model.Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<com.suvojeet.suvmusic.core.model.Artist>()
        
        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"
        
        context.contentResolver.query(
            collection,
            ARTIST_PROJECTION,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val artistName = cursor.getString(artistColumn) ?: "Unknown Artist"
                
                artists.add(
                    com.suvojeet.suvmusic.core.model.Artist(
                        id = id.toString(),
                        name = artistName,
                        thumbnailUrl = null,
                        channelId = id.toString()
                    )
                )
            }
        }
        artists
    }

    /**
     * Get songs for a specific album.
     */
    suspend fun getSongsByAlbum(albumId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        
        context.contentResolver.query(
            collection,
            AUDIO_PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TRACK} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                
                songs.add(Song.fromLocal(id, title, artist, album, duration, albumArtUri, contentUri))
            }
        }
        songs
    }
    
    /**
     * Get albums for a specific artist.
     */
    suspend fun getAlbumsByArtist(artistId: Long): List<com.suvojeet.suvmusic.core.model.Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<com.suvojeet.suvmusic.core.model.Album>()
        val collection = MediaStore.Audio.Artists.Albums.getContentUri("external", artistId)
        
        context.contentResolver.query(collection, ALBUM_PROJECTION, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
                albums.add(com.suvojeet.suvmusic.core.model.Album(
                    id = id.toString(),
                    title = cursor.getString(albumColumn) ?: "Unknown",
                    artist = cursor.getString(artistColumn) ?: "Unknown",
                    thumbnailUrl = albumArtUri.toString(),
                    year = null,
                    songs = emptyList()
                ))
            }
        }
        albums
    }

    /**
     * Get all songs for a specific artist.
     */
    suspend fun getSongsByArtist(artistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artistId.toString())
        
        context.contentResolver.query(collection, AUDIO_PROJECTION, selection, selectionArgs, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                songs.add(Song.fromLocal(id, cursor.getString(titleColumn) ?: "Unknown", cursor.getString(artistColumn) ?: "Unknown", cursor.getString(albumColumn) ?: "Unknown", cursor.getLong(durationColumn), albumArtUri, contentUri))
            }
        }
        songs
    }

    /**
     * Search local albums.
     */
    suspend fun searchLocalAlbums(query: String): List<com.suvojeet.suvmusic.core.model.Album> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val albums = mutableListOf<com.suvojeet.suvmusic.core.model.Album>()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Albums.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        context.contentResolver.query(collection, ALBUM_PROJECTION, selection, selectionArgs, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
                albums.add(com.suvojeet.suvmusic.core.model.Album(
                    id = id.toString(),
                    title = cursor.getString(albumColumn) ?: "Unknown",
                    artist = cursor.getString(artistColumn) ?: "Unknown",
                    thumbnailUrl = albumArtUri.toString(),
                    year = null,
                    songs = emptyList()
                ))
            }
        }
        albums
    }

    /**
     * Search local artists.
     */
    suspend fun searchLocalArtists(query: String): List<com.suvojeet.suvmusic.core.model.Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val artists = mutableListOf<com.suvojeet.suvmusic.core.model.Artist>()
        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Artists.ARTIST} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        context.contentResolver.query(collection, ARTIST_PROJECTION, selection, selectionArgs, "${MediaStore.Audio.Artists.ARTIST} ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                artists.add(com.suvojeet.suvmusic.core.model.Artist(
                    id = id.toString(),
                    name = cursor.getString(artistColumn) ?: "Unknown",
                    thumbnailUrl = null,
                    channelId = id.toString()
                ))
            }
        }
        artists
    }

    /**
     * Search local songs by title or artist.
     */
    suspend fun searchLocalSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                "(${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)"
        val selectionArgs = arrayOf("%$query%", "%$query%")
        
        val songs = mutableListOf<Song>()
        
        context.contentResolver.query(
            collection,
            AUDIO_PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                songs.add(
                    Song.fromLocal(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        albumArtUri = albumArtUri,
                        contentUri = contentUri
                    )
                )
            }
        }
        
        songs
    }
}
