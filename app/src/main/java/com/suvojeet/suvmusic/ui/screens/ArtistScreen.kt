package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.surfaceColorAtElevation
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.ArtistPreview
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.components.PremiumLoadingScreen
import com.suvojeet.suvmusic.ui.viewmodel.ArtistError
import com.suvojeet.suvmusic.ui.viewmodel.ArtistViewModel
import kotlin.math.min

@Composable
fun ArtistScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSeeAllAlbumsClick: () -> Unit,
    onSeeAllSinglesClick: () -> Unit,
    onArtistClick: (ArtistPreview) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onStartRadio: (String) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    // Calculate scroll offset for sticky header fading
    val headerAlpha by remember {
        derivedStateOf {
            val firstVisibleItemIndex = scrollState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = scrollState.firstVisibleItemScrollOffset
            if (firstVisibleItemIndex == 0) {
                // Fade in alpha as we scroll past the first 200px
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
                val artist = uiState.artist!!

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Immersive Header
                    item {
                        ImmersiveArtistHeader(
                            artist = artist,
                            onPlayAll = {
                                if (artist.songs.isNotEmpty()) {
                                    onSongClick(artist.songs, 0)
                                }
                            },
                            onShuffle = {
                                if (artist.songs.isNotEmpty()) {
                                    val randomIndex = artist.songs.indices.random()
                                    onSongClick(artist.songs, randomIndex)
                                }
                            },
                            onSubscribe = viewModel::toggleSubscribe,
                            isSubscribed = artist.isSubscribed,
                            isSubscribing = uiState.isSubscribing,
                            onStartRadio = { viewModel.startRadio(onStartRadio) }
                        )
                    }

                    // Content Sections
                    
                    // Latest Release
                    val latestRelease = (artist.albums + artist.singles)
                         .maxByOrNull { it.year ?: "0" }
                    if (latestRelease != null) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            LatestReleaseSection(
                                album = latestRelease,
                                isSingle = artist.singles.contains(latestRelease),
                                onClick = { onAlbumClick(latestRelease) }
                            )
                        }
                    }

                    // Top Songs
                    if (artist.songs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = stringResource(R.string.header_top_songs))
                            Spacer(modifier = Modifier.height(8.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        itemsIndexed(artist.songs.take(5)) { index, song ->
                            TopSongRow(
                                index = index + 1,
                                song = song,
                                onClick = { onSongClick(artist.songs, index) }
                            )
                        }
                    }

                    // Discography - Albums
                    if (artist.albums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(
                                title = stringResource(R.string.header_albums),
                                showSeeAll = artist.albums.size > 5,
                                onSeeAllClick = onSeeAllAlbumsClick
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.albums) { album ->
                                    ArtistContentCard(
                                        title = album.title,
                                        subtitle = album.year,
                                        imageUrl = album.thumbnailUrl,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }

                    // Discography - Singles & EPs
                    if (artist.singles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(
                                title = stringResource(R.string.header_singles_eps),
                                showSeeAll = artist.singles.size > 5,
                                onSeeAllClick = onSeeAllSinglesClick
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.singles) { single ->
                                    ArtistContentCard(
                                        title = single.title,
                                        subtitle = single.year,
                                        imageUrl = single.thumbnailUrl,
                                        onClick = { onAlbumClick(single) }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Videos
                    if (artist.videos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = stringResource(R.string.header_videos))
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(artist.videos) { index, video ->
                                    ArtistVideoCard(
                                        video = video,
                                        onClick = { onSongClick(artist.videos, index) } // Assuming video plays like a song
                                    )
                                }
                            }
                        }
                    }

                    // Featured On
                    if (artist.featuredPlaylists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = stringResource(R.string.header_featured_on))
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.featuredPlaylists) { playlist ->
                                    ArtistContentCard(
                                        title = playlist.title,
                                        subtitle = "Playlist", // Or author if available
                                        imageUrl = playlist.thumbnailUrl,
                                        onClick = { onPlaylistClick(playlist) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Fans Also Like
                    if (artist.relatedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = stringResource(R.string.header_fans_also_like))
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artist.relatedArtists) { related ->
                                    ArtistCircleCard(
                                        artist = related,
                                        onClick = { onArtistClick(related) }
                                    )
                                }
                            }
                        }
                    }

                    // About
                    if (!artist.description.isNullOrBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = stringResource(R.string.header_about_artist, artist.name))
                            Spacer(modifier = Modifier.height(16.dp))
                            AboutArtistCard(
                                artist = artist,
                                onClick = { /* Expand bio if needed */ }
                            )
                        }
                    }
                }

                // Sticky Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp) // Height including status bar
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = headerAlpha),
                                    MaterialTheme.colorScheme.background.copy(alpha = min(0.9f, headerAlpha)),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (headerAlpha < 0.5f) Color.Black.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = lerp(Color.White, MaterialTheme.colorScheme.onBackground, headerAlpha)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        AnimatedVisibility(
                            visible = headerAlpha > 0.8f,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImmersiveArtistHeader(
    artist: Artist,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSubscribe: () -> Unit,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    onStartRadio: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp) // Taller, more immersive header
    ) {
        val highResThumbnail = artist.thumbnailUrl?.let { url ->
            url.replace(Regex("w\\d+-h\\d+"), "w1200-h1200") // Get higher res
                .replace(Regex("=w\\d+"), "=w1200")
                .replace(Regex("=s\\d+"), "=s1200")
        }

        // Background Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(highResThumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            
            // Channel/Verified Badge (if applicable, mocking structure)
            if (artist.subscribers != null) {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.padding(bottom = 8.dp)
                 ) {
                     Icon(
                         imageVector = Icons.Default.CheckCircle,
                         contentDescription = null,
                         tint = MaterialTheme.colorScheme.primary, // Dominant color usage
                         modifier = Modifier.size(16.dp)
                     )
                     Spacer(modifier = Modifier.width(6.dp))
                     Text(
                         text = "Verified Artist", // Use string resource in real app if available
                         style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                         fontWeight = FontWeight.Medium
                     )
                 }
            }

            // Artist Name
            Text(
                text = artist.name,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Listeners/Subscribers
            if (artist.subscribers != null) {
                Text(
                    text = "${artist.subscribers} subscribers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Spacing between buttons
            ) {
                 // Play Button (Prominent)
                FloatingActionButton(
                    onClick = onPlayAll,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_play),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Shuffle Button
                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                         imageVector = Icons.Default.Shuffle,
                         contentDescription = stringResource(R.string.action_shuffle),
                         tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Radio Button
                IconButton(
                    onClick = onStartRadio,
                       modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                        .clip(CircleShape)
                ) {
                     Icon(
                         imageVector = Icons.Default.Radio,
                         contentDescription = stringResource(R.string.action_start_radio),
                         tint = MaterialTheme.colorScheme.onBackground
                     )
                }

                // Follow Button (Outlined)
                 OutlinedButton(
                     onClick = onSubscribe,
                     shape = RoundedCornerShape(50),
                     border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                     colors = ButtonDefaults.outlinedButtonColors(
                         contentColor = MaterialTheme.colorScheme.onBackground
                     ),
                     modifier = Modifier.height(48.dp)
                 ) {
                     if (isSubscribing) {
                         CircularProgressIndicator(
                             modifier = Modifier.size(16.dp),
                             strokeWidth = 2.dp,
                             color = MaterialTheme.colorScheme.onBackground
                         )
                     } else {
                         Text(
                             text = if (isSubscribed) stringResource(R.string.action_following).uppercase() else stringResource(R.string.action_follow).uppercase(),
                             fontWeight = FontWeight.Bold,
                             fontSize = 12.sp,
                             letterSpacing = 1.sp
                         )
                     }
                 }
            }
        }
    }
}

@Composable
fun LatestReleaseSection(
    album: Album,
    isSingle: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.header_latest_release),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                     text = buildString {
                        append(if (isSingle) stringResource(R.string.badge_single) else stringResource(R.string.badge_album))
                        if (album.year != null) append(" • ${album.year}")
                     },
                     style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TopSongRow(
    index: Int,
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        
        // Options like duration or menu could go here
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}


@Composable
fun ArtistContentCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp) // Updated corner radius
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .clip(shape) // Square with rounded corners for albums/singles
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artist.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp) // Wider for 16:9 video look
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.8f),
                contentScale = ContentScale.Crop
            )
            
            // Play icon overlay
            Box(
                modifier = Modifier.fillMaxSize(),
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Video", // or duration if available
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AboutArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
     Box(
         modifier = Modifier
             .fillMaxWidth()
             .padding(horizontal = 20.dp)
             .height(300.dp) // Fixed height for visual consistency
             .clickable(onClick = onClick)
             .clip(RoundedCornerShape(16.dp))
     ) {
         // Background Image (dimmed)
         if (artist.thumbnailUrl != null) {
              val highResThumbnail = artist.thumbnailUrl.replace(Regex("w\\d+-h\\d+"), "w800-h800")
              AsyncImage(
                  model = ImageRequest.Builder(LocalContext.current)
                      .data(highResThumbnail)
                      .crossfade(true)
                      .build(),
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize().blur(radius = 0.dp), // Clear image
                  contentScale = ContentScale.Crop
              )
              
              // Dark gradient overlay for text readability
              Box(
                  modifier = Modifier
                      .fillMaxSize()
                      .background(
                          Brush.verticalGradient(
                              colors = listOf(
                                  Color.Transparent,
                                  Color.Black.copy(alpha = 0.5f),
                                  Color.Black.copy(alpha = 0.8f)
                              )
                          )
                      )
              )
         } else {
              Box(
                  modifier = Modifier
                      .fillMaxSize()
                      .background(MaterialTheme.colorScheme.surfaceVariant)
              )
         }
         
         // Text Content
         Column(
             modifier = Modifier
                 .align(Alignment.BottomStart)
                 .padding(24.dp)
         ) {
             Text(
                 text = "${artist.views ?: "Hundreds of"} monthly listeners", // Mocking data if not available
                 style = MaterialTheme.typography.titleMedium,
                 fontWeight = FontWeight.Bold,
                 color = if (artist.thumbnailUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
             )
             Spacer(modifier = Modifier.height(12.dp))
             Text(
                 text = artist.description ?: "",
                 style = MaterialTheme.typography.bodyMedium,
                 color = if (artist.thumbnailUrl != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                 maxLines = 3,
                 overflow = TextOverflow.Ellipsis,
                 lineHeight = 22.sp
             )
         }
     }
}


@Composable
fun SectionHeader(
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
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), // Larger, more evident headers
            color = MaterialTheme.colorScheme.onBackground
        )
        
        if (showSeeAll) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = stringResource(R.string.action_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Subtler "See all"
                    fontWeight = FontWeight.SemiBold
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

// Helper tuple class
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


private fun formatDuration(durationMillis: Long): String {
    val minutes = (durationMillis / 1000) / 60
    val seconds = (durationMillis / 1000) % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
