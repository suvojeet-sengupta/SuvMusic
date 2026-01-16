package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Lyrics
import kotlinx.coroutines.launch

@Composable
fun LyricsScreen(
    lyrics: Lyrics?,
    isFetching: Boolean,
    currentTimeProvider: () -> Long,
    artworkUrl: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blurred Background
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(0.6f),
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isFetching) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (lyrics == null || lyrics.lines.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Lyrics not available",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LyricsList(
                        lyrics = lyrics,
                        currentTimeProvider = currentTimeProvider
                    )
                }
            }
            
            // Footer (Source Credit)
            if (lyrics?.sourceCredit != null) {
                Text(
                    text = lyrics.sourceCredit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun LyricsList(
    lyrics: Lyrics,
    currentTimeProvider: () -> Long
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val currentTime = currentTimeProvider()

    // Determine active line index
    var activeLineIndex by remember { mutableStateOf(-1) }
    
    LaunchedEffect(currentTime, lyrics) {
        if (lyrics.isSynced) {
            // Find the current line
            // A line is active if currentTime >= startTime && currentTime < endTime (or next line start)
            val index = lyrics.lines.indexOfLast { it.startTimeMs <= currentTime }
            if (index != activeLineIndex && index >= 0) {
                activeLineIndex = index
                // Scroll to center
                // We want the active line to be roughly in the middle
                // Calculating offset is tricky without item heights, but generic scroll might work
                try {
                     listState.animateScrollToItem(
                        index = index,
                        scrollOffset = -400 // Approximate offset to center, value depends on screen height/item height
                    )
                } catch (e: Exception) {
                    // Ignore scroll errors
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 200.dp), // Large padding to allow scrolling top active to center
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = if (lyrics.isSynced) index == activeLineIndex else true
            
            // Apple Music Style:
            // Active: Scale 1.2, Opacity 1.0, Blur 0
            // Inactive: Scale 1.0, Opacity 0.5, Blur 1.dp (maybe too heavy for list, sticking to opacity)
            
            val alpha by animateFloatAsState(
                targetValue = if (isActive || !lyrics.isSynced) 1f else 0.5f,
                animationSpec = tween(durationMillis = 300), 
                label = "alpha"
            )
            
            val scale by animateFloatAsState(
                targetValue = if (isActive && lyrics.isSynced) 1.1f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "scale"
            )

            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                ),
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    // .scale(scale) // Scaling text layout can be jittery, consider fontSize change or simple opacity
                    .clickable { 
                        // Seek to this line if synced?
                        // onClick(line.startTimeMs) 
                    }
            )
        }
    }
}
