package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.ArtistPreview
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.viewmodel.ArtistError
import com.suvojeet.suvmusic.ui.viewmodel.ArtistViewModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Radio
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun ArtistScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSeeAllAlbumsClick: () -> Unit,
    onSeeAllSinglesClick: () -> Unit,
    onArtistClick: (ArtistPreview) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                PremiumLoadingScreen(
                    thumbnailUrl = null, // No initial thumbnail for artists yet
                    onBackClick = onBackClick
                )
            }
            uiState.error != null -> {
                ArtistErrorView(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadArtist() },
                    onBackClick = onBackClick
                )
            }
            uiState.artist != null -> {
                val artist = uiState.artist!!

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Hero Header
                    item {
                        ArtistHeroHeader(
                            artist = artist,
                            onBackClick = onBackClick,
                            onPlayAll = {
                                if (artist.songs.isNotEmpty()) {
                                    onSongClick(artist.songs.first())
                                }
                            },
                            onShuffle = {
                                onSongClick(artist.songs.random())
                            },
                            onSubscribe = viewModel::toggleSubscribe,
                            isSubscribed = artist.isSubscribed,
                            isSubscribing = uiState.isSubscribing,
                            onStartRadio = { viewModel.startRadio { /* Handle radio start */ } }
                        )
                    }

                    // Latest Release Section
                    val latestRelease = (artist.albums + artist.singles)
                        .maxByOrNull { it.year ?: "0" }
                    if (latestRelease != null) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            LatestReleaseSection(
                                album = latestRelease,
                                isSingle = artist.singles.contains(latestRelease),
                                onClick = { onAlbumClick(latestRelease) }
                            )
                        }
                    }

                    // Top Songs Section
                    if (artist.songs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            ArtistSectionHeader(title = stringResource(R.string.header_top_songs))
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        itemsIndexed(artist.songs.take(5)) { index, song ->
                            TopSongRow(
                                index = index + 1,
                                song = song,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }

                    // Videos Section
                    if (artist.videos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            ArtistSectionHeader(title = stringResource(R.string.header_videos))
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.videos) { video ->
                                    ArtistVideoCard(
                                        video = video,
                                        onClick = { onSongClick(video) }
                                    )
                                }
                            }
                        }
                    }

                    // Albums Section
                    if (artist.albums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            ArtistSectionHeader(
                                title = stringResource(R.string.header_albums),
                                showSeeAll = artist.albums.size > 4,
                                onSeeAllClick = onSeeAllAlbumsClick
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.albums) { album ->
                                    ArtistAlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }

                    // Singles Section
                    if (artist.singles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            ArtistSectionHeader(
                                title = stringResource(R.string.header_singles_eps),
                                showSeeAll = artist.singles.size > 4,
                                onSeeAllClick = onSeeAllSinglesClick
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.singles) { single ->
                                    ArtistAlbumCard(
                                        album = single,
                                        onClick = { onAlbumClick(single) }
                                    )
                                }
                            }
                        }
                    }

                    // Featured Playlists Section
                    if (artist.featuredPlaylists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            ArtistSectionHeader(title = stringResource(R.string.header_featured_on))
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.featuredPlaylists) { playlist ->
                                    FeaturedPlaylistCard(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist) }
                                    )
                                }
                            }
                        }
                    }

                    // Related Artists Section
                    if (artist.relatedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            ArtistSectionHeader(title = stringResource(R.string.header_fans_also_like))
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.relatedArtists) { related ->
                                    RelatedArtistCard(
                                        artist = related,
                                        onClick = { onArtistClick(related) }
                                    )
                                }
                            }
                        }
                    }

                    // About Section
                    if (!artist.description.isNullOrBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            AboutSection(
                                artistName = artist.name,
                                description = artist.description
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistErrorView(
    error: ArtistError,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    val (icon, title, message, buttonText) = when (error) {
        ArtistError.AUTH_REQUIRED -> Quad(
            Icons.Default.Lock,
            stringResource(R.string.error_restricted_title),
            stringResource(R.string.error_restricted_message),
            stringResource(R.string.action_retry)
        )
        ArtistError.NETWORK -> Quad(
            Icons.Default.CloudOff,
            stringResource(R.string.error_connection_title),
            stringResource(R.string.error_connection_message),
            stringResource(R.string.action_retry)
        )
        else -> Quad(
            Icons.Default.Warning,
            stringResource(R.string.error_generic_title),
            stringResource(R.string.error_generic_message),
            stringResource(R.string.action_retry)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Back button always available
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with subtle background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonText)
            }
        }
    }
}

// Helper tuple class
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun ArtistHeroHeader(
    artist: Artist,
    onBackClick: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSubscribe: () -> Unit,
    isSubscribed: Boolean = false,
    isSubscribing: Boolean = false,
    onStartRadio: () -> Unit
) {
    val context = LocalContext.current
    val highResThumbnail = artist.thumbnailUrl?.let { url ->
        url.replace(Regex("w\\d+-h\\d+"), "w800-h800")
            .replace(Regex("=w\\d+"), "=w800")
            .replace(Regex("=s\\d+"), "=s800")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        // Background blur layer
        if (artist.thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(highResThumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Artist Image (centered circle)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (artist.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(highResThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier
                        .size(180.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Artist Name
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Subscriber count
            if (artist.subscribers != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist.subscribers,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    // Play Button
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_play),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {
                    // Shuffle Button
                    FilledTonalButton(
                        onClick = onShuffle,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_shuffle),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {
                    // Radio Button
                    FilledTonalButton(
                        onClick = onStartRadio,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_start_radio),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {
                    // Subscribe Button
                    Button(
                        onClick = onSubscribe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurface,
                            contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(44.dp),
                        enabled = !isSubscribing
                    ) {
                        if (isSubscribing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            val icon = if (isSubscribed) Icons.Default.Check else Icons.Default.Add
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSubscribed) stringResource(R.string.action_following) else stringResource(R.string.action_follow),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LatestReleaseSection(
    album: Album,
    isSingle: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.header_latest_release),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Type badge
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isSingle) stringResource(R.string.badge_single) else stringResource(R.string.badge_album),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (album.year != null) {
                        Text(
                            text = album.year,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.action_play),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistSectionHeader(
    title: String,
    showSeeAll: Boolean = false,
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (showSeeAll) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = stringResource(R.string.action_see_all),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TopSongRow(
    index: Int,
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index number
        Text(
            text = "$index",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Song thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = song.title,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // More button
        IconButton(onClick = { /* TODO: Show options */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        // Album art with shadow
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
        ) {
            if (album.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (album.year != null) {
            Text(
                text = album.year,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutSection(
    artistName: String,
    description: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.header_about_artist, artistName),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize()
        )

        if (description.length > 150) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isExpanded) stringResource(R.string.action_show_less) else stringResource(R.string.action_read_more),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ArtistVideoCard(
    video: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Play icon overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RelatedArtistCard(
    artist: ArtistPreview,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artist.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = artist.name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (artist.subscribers != null) {
            Text(
                text = artist.subscribers,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeaturedPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(playlist.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = playlist.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = playlist.author,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}