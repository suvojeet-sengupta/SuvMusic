package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.ArtistPreview
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.BounceButton
import com.suvojeet.suvmusic.ui.components.NewReleaseCard
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
import com.suvojeet.suvmusic.ui.components.player.MultipleArtistsDialog
import com.suvojeet.suvmusic.ui.components.rememberDominantColors
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.viewmodel.ArtistError
import com.suvojeet.suvmusic.ui.viewmodel.ArtistViewModel
import com.suvojeet.suvmusic.util.dpadFocusable
import kotlin.math.min

@Composable
fun ArtistScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSeeAllAlbumsClick: () -> Unit,
    onSeeAllSinglesClick: () -> Unit,
    onArtistClick: (ArtistPreview) -> Unit,
    onArtistIdClick: (String) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit,
    onStartRadio: (String) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    
    val artist = uiState.artist
    val dominantColors = rememberDominantColors(
        imageUrl = artist?.thumbnailUrl,
        isDarkTheme = isDark
    )

    if (uiState.showMultipleArtistsDialog) {
        MultipleArtistsDialog(
            artists = uiState.currentArtistCredits,
            onArtistClick = onArtistIdClick,
            onDismiss = { viewModel.toggleMultipleArtistsDialog(false) },
            dominantColors = dominantColors
        )
    }

    // Calculate scroll offset for sticky header fading
    val headerAlpha by remember {
        derivedStateOf {
            val firstVisibleItemIndex = scrollState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = scrollState.firstVisibleItemScrollOffset
            if (firstVisibleItemIndex == 0) {
                min(1f, firstVisibleItemScrollOffset / 500f)
            } else {
                1f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                PremiumLoadingScreen(
                    thumbnailUrl = null,
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
                val currentArtist = uiState.artist!!
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Immersive Header
                    item {
                        ImmersiveArtistHeader(
                            artist = currentArtist,
                            dominantColors = dominantColors,
                            onPlayAll = {
                                if (currentArtist.songs.isNotEmpty()) {
                                    onSongClick(currentArtist.songs, 0)
                                }
                            },
                            onShuffle = {
                                if (currentArtist.songs.isNotEmpty()) {
                                    val randomIndex = currentArtist.songs.indices.random()
                                    onSongClick(currentArtist.songs, randomIndex)
                                }
                            },
                            onSubscribe = viewModel::toggleSubscribe,
                            isSubscribed = currentArtist.isSubscribed,
                            isSubscribing = uiState.isSubscribing,
                            onStartRadio = { viewModel.startRadio(onStartRadio) }
                        )
                    }

                    // Content Sections
                    
                    // Latest Release
                    val latestRelease = (currentArtist.albums + currentArtist.singles)
                         .maxByOrNull { it.year ?: "0" }
                    if (latestRelease != null) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            LatestReleaseSection(
                                artistName = currentArtist.name,
                                album = latestRelease,
                                isSingle = currentArtist.singles.contains(latestRelease),
                                dominantColors = dominantColors,
                                onClick = { onAlbumClick(latestRelease) }
                            )
                        }
                    }

                    // Top Songs
                    if (currentArtist.songs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(title = stringResource(R.string.header_top_songs), dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        itemsIndexed(currentArtist.songs.take(5), key = { index, song -> "${song.id}_$index" }) { index, song ->
                            TopSongRow(
                                index = index + 1,
                                song = song,
                                dominantColors = dominantColors,
                                onClick = { onSongClick(currentArtist.songs, index) },
                                onArtistClick = {
                                    viewModel.fetchArtistCreditsAndShow(song.artist, song.source)
                                }
                            )
                        }
                    }

                    // Discography - Albums
                    if (currentArtist.albums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(
                                title = stringResource(R.string.header_albums),
                                showSeeAll = currentArtist.albums.size > 5,
                                dominantColors = dominantColors,
                                onSeeAllClick = onSeeAllAlbumsClick
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(currentArtist.albums, key = { it.id }) { album ->
                                    ArtistContentCard(
                                        title = album.title,
                                        subtitle = album.year,
                                        imageUrl = album.thumbnailUrl,
                                        dominantColors = dominantColors,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }

                    // Discography - Singles & EPs
                    if (currentArtist.singles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(
                                title = stringResource(R.string.header_singles_eps),
                                showSeeAll = currentArtist.singles.size > 5,
                                dominantColors = dominantColors,
                                onSeeAllClick = onSeeAllSinglesClick
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(currentArtist.singles, key = { it.id }) { single ->
                                    ArtistContentCard(
                                        title = single.title,
                                        subtitle = single.year,
                                        imageUrl = single.thumbnailUrl,
                                        dominantColors = dominantColors,
                                        onClick = { onAlbumClick(single) }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Videos
                    if (currentArtist.videos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(title = stringResource(R.string.header_videos), dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(20.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(currentArtist.videos) { index, video ->
                                    ArtistVideoCard(
                                        video = video,
                                        dominantColors = dominantColors,
                                        onClick = { onSongClick(currentArtist.videos, index) }
                                    )
                                }
                            }
                        }
                    }

                    // Featured On
                    if (currentArtist.featuredPlaylists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(title = stringResource(R.string.header_featured_on), dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(20.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(currentArtist.featuredPlaylists, key = { it.id }) { playlist ->
                                    ArtistContentCard(
                                        title = playlist.title,
                                        subtitle = "Playlist",
                                        imageUrl = playlist.thumbnailUrl,
                                        dominantColors = dominantColors,
                                        onClick = { onPlaylistClick(playlist) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Fans Also Like
                    if (currentArtist.relatedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(title = stringResource(R.string.header_fans_also_like), dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(20.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                items(currentArtist.relatedArtists, key = { it.id }) { related ->
                                    ArtistCircleCard(
                                        artist = related,
                                        dominantColors = dominantColors,
                                        onClick = { onArtistClick(related) }
                                    )
                                }
                            }
                        }
                    }

                    // About
                    if (!currentArtist.description.isNullOrBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(title = stringResource(R.string.header_about_artist, currentArtist.name), dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(20.dp))
                            AboutArtistCard(
                                artist = currentArtist,
                                dominantColors = dominantColors,
                                onClick = { /* Expand bio if needed */ }
                            )
                        }
                    }
                }

                // Glassy Sticky Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .graphicsLayer { alpha = headerAlpha }
                        .blur(radius = 20.dp * headerAlpha)
                        .background(
                            lerp(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.85f),
                                headerAlpha
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (headerAlpha < 0.3f) Color.Black.copy(alpha = 0.2f) else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = lerp(Color.White, onSurfaceColor, headerAlpha)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    AnimatedVisibility(
                        visible = headerAlpha > 0.8f,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = currentArtist.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImmersiveArtistHeader(
    artist: Artist,
    dominantColors: DominantColors,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSubscribe: () -> Unit,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    onStartRadio: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        val highResThumbnail = artist.thumbnailUrl?.let { url ->
            url.replace(Regex("w\\d+-h\\d+"), "w1200-h1200")
                .replace(Regex("=w\\d+"), "=w1200")
                .replace(Regex("=s\\d+"), "=s1200")
        }

        // Background Image with Parallax-ready feel
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Multi-layered Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            dominantColors.primary.copy(alpha = 0.4f),
                            dominantColors.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Artist Info & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
        ) {
            
            if (artist.subscribers != null) {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier
                         .clip(RoundedCornerShape(50))
                         .background(dominantColors.accent.copy(alpha = 0.2f))
                         .padding(horizontal = 10.dp, vertical = 4.dp)
                 ) {
                     Icon(
                         imageVector = Icons.Default.Verified,
                         contentDescription = null,
                         tint = dominantColors.accent,
                         modifier = Modifier.size(16.dp)
                     )
                     Spacer(modifier = Modifier.width(6.dp))
                     Text(
                         text = "Official Artist",
                         style = MaterialTheme.typography.labelMedium,
                         color = Color.White.copy(alpha = 0.9f),
                         fontWeight = FontWeight.Bold
                     )
                 }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = artist.name,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    lineHeight = 52.sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (artist.subscribers != null) {
                Text(
                    text = "${artist.subscribers} fans",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BounceButton(
                    onClick = onPlayAll,
                    size = 64.dp,
                    shape = CircleShape,
                    modifier = Modifier.background(dominantColors.accent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_play),
                        tint = if (dominantColors.accent.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShuffle,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                             imageVector = Icons.Default.Shuffle,
                             contentDescription = stringResource(R.string.action_shuffle),
                             tint = Color.White
                        )
                    }
                    
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).width(1.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    IconButton(
                        onClick = onStartRadio,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                         Icon(
                             imageVector = Icons.Default.Radio,
                             contentDescription = stringResource(R.string.action_start_radio),
                             tint = Color.White
                         )
                    }
                }

                Button(
                    onClick = onSubscribe,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) Color.White.copy(alpha = 0.2f) else Color.White,
                        contentColor = if (isSubscribed) Color.White else Color.Black
                    ),
                    modifier = Modifier.height(56.dp).weight(1f)
                ) {
                    if (isSubscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = if (isSubscribed) Color.White else Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSubscribed) stringResource(R.string.action_following) else stringResource(R.string.action_follow),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LatestReleaseSection(
    artistName: String,
    album: Album,
    isSingle: Boolean,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Latest from $artistName",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(if (isSingle) "Single" else "Album")
                            if (album.year != null) append(" • ${album.year}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = dominantColors.accent
                )
            }
        }
    }
}

@Composable
fun TopSongRow(
    index: Int,
    song: Song,
    dominantColors: DominantColors,
    onClick: () -> Unit,
    onArtistClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtistClick() },
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        IconButton(onClick = { /* More options */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun ArtistContentCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    dominantColors: DominantColors,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(164.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(164.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ArtistCircleCard(
    artist: ArtistPreview,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artist.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun ArtistVideoCard(
    video: Song,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Default.PlayCircleFilled,
                     contentDescription = null,
                     tint = Color.White.copy(alpha = 0.9f),
                     modifier = Modifier.size(48.dp)
                 )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Music Video",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AboutArtistCard(
    artist: Artist,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    val context = LocalContext.current
     Box(
         modifier = Modifier
             .fillMaxWidth()
             .padding(horizontal = 20.dp)
             .height(320.dp)
             .clip(RoundedCornerShape(32.dp))
             .clickable(onClick = onClick)
     ) {
         AsyncImage(
             model = ImageRequest.Builder(context)
                 .data(artist.thumbnailUrl)
                 .crossfade(true)
                 .build(),
             contentDescription = null,
             modifier = Modifier.fillMaxSize(),
             contentScale = ContentScale.Crop
         )
         
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(
                     Brush.verticalGradient(
                         colors = listOf(
                             Color.Transparent,
                             Color.Black.copy(alpha = 0.4f),
                             Color.Black.copy(alpha = 0.9f)
                         )
                     )
                 )
         )
         
         Column(
             modifier = Modifier
                 .align(Alignment.BottomStart)
                 .padding(28.dp)
         ) {
             Text(
                 text = "${artist.views ?: "Millions of"} monthly listeners",
                 style = MaterialTheme.typography.titleMedium,
                 fontWeight = FontWeight.Black,
                 color = Color.White
             )
             Spacer(modifier = Modifier.height(12.dp))
             Text(
                 text = artist.description ?: "Biography currently unavailable.",
                 style = MaterialTheme.typography.bodyLarge,
                 color = Color.White.copy(alpha = 0.8f),
                 maxLines = 4,
                 overflow = TextOverflow.Ellipsis,
                 lineHeight = 24.sp
             )
         }
     }
}


@Composable
fun SectionHeader(
    title: String,
    dominantColors: DominantColors,
    showSeeAll: Boolean = false,
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        if (showSeeAll) {
            TextButton(
                onClick = onSeeAllClick,
                colors = ButtonDefaults.textButtonColors(contentColor = dominantColors.accent)
            ) {
                Text(
                    text = stringResource(R.string.action_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
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
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50)
            ) {
                Text(text = buttonText)
            }
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

private fun formatDuration(durationMillis: Long): String {
    val minutes = (durationMillis / 1000) / 60
    val seconds = (durationMillis / 1000) % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
