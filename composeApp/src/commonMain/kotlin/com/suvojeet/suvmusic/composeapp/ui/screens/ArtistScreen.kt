package com.suvojeet.suvmusic.composeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.image.DominantColors
import com.suvojeet.suvmusic.composeapp.image.defaultDominantColors
import com.suvojeet.suvmusic.composeapp.ui.AlbumArt
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Song

/**
 * Artist detail screen — stateless port of the visual core of
 * `app/.../ui/screens/ArtistScreen.kt` (1170 lines on Android).
 *
 * Like [AlbumScreen], the Android original is wired through
 * [ArtistViewModel] (Koin) for fetching, related artists, video carousels,
 * and similar. This commonMain shape takes a fully populated [Artist] in
 * hand and emits taps. Caller-owned data flow on both platforms.
 *
 * Sections (in order):
 *  - Hero — circular avatar, name, subscribers, Play / Shuffle CTAs
 *  - Top songs (LazyColumn, top N rows)
 *  - Albums (horizontal scroller of square cards)
 *  - Singles (horizontal scroller, only when populated)
 *  - About — short description + channel info
 */
@Composable
fun ArtistScreen(
    artist: Artist,
    currentSongId: String? = null,
    dominantColors: DominantColors = defaultDominantColors(true),
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ArtistHero(
                    artist = artist,
                    dominantColors = dominantColors,
                    onBackClick = onBackClick,
                    onPlayAll = { onPlayAll(artist.songs) },
                    onShufflePlay = { onShufflePlay(artist.songs) },
                )
            }

            if (artist.songs.isNotEmpty()) {
                item { ArtistSectionTitle("Top songs") }
                itemsTopSongs(
                    songs = artist.songs.take(8),
                    currentSongId = currentSongId,
                    onSongClick = onSongClick,
                )
            }

            if (artist.albums.isNotEmpty()) {
                item { ArtistSectionTitle("Albums") }
                item {
                    AlbumScroller(
                        albums = artist.albums,
                        onAlbumClick = onAlbumClick,
                    )
                }
            }

            if (artist.singles.isNotEmpty()) {
                item { ArtistSectionTitle("Singles & EPs") }
                item {
                    AlbumScroller(
                        albums = artist.singles,
                        onAlbumClick = onAlbumClick,
                    )
                }
            }

            artist.description?.takeIf { it.isNotBlank() }?.let { desc ->
                item { ArtistSectionTitle("About") }
                item { ArtistAbout(text = desc) }
            }
        }
    }
}

@Composable
private fun ArtistHero(
    artist: Artist,
    dominantColors: DominantColors,
    onBackClick: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.primary,
                        dominantColors.secondary,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = dominantColors.onBackground,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = dominantColors.onBackground,
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AlbumArt(
                    thumbnailUrl = artist.thumbnailUrl,
                    contentDescription = artist.name,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = dominantColors.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
                artist.subscribers?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onPlayAll,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dominantColors.accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = onShufflePlay,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                            contentColor = dominantColors.onBackground,
                        ),
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ArtistSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsTopSongs(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (List<Song>, Int) -> Unit,
) {
    items(songs) { song ->
        val index = songs.indexOf(song)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSongClick(songs, index) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (currentSongId == song.id) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (currentSongId == song.id) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.album.isNotBlank()) {
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumScroller(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums) { album ->
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { onAlbumClick(album) },
            ) {
                AlbumArt(
                    thumbnailUrl = album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!album.year.isNullOrBlank()) {
                    Text(
                        text = album.year,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAbout(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
}
