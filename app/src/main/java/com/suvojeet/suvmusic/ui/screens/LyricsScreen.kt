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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.content.Intent
import com.suvojeet.suvmusic.ui.utils.LyricsImageGenerator
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
    isDarkTheme: Boolean = true,
    onSeekTo: (Long) -> Unit = {},
    songTitle: String = "",
    artistName: String = "",
    modifier: Modifier = Modifier
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Blurred Background
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(if (isDarkTheme) 0.6f else 0.4f),
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
                            overlayColor.copy(alpha = 0.4f),
                            overlayColor.copy(alpha = 0.7f),
                            overlayColor.copy(alpha = 0.9f)
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
                        .background(textColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor,
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
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Lyrics not available",
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LyricsList(
                        lyrics = lyrics,
                        currentTimeProvider = currentTimeProvider,
                        isDarkTheme = isDarkTheme,
                        onSeekTo = onSeekTo,
                        songTitle = songTitle,
                        artistName = artistName,
                        artworkUrl = artworkUrl
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsList(
    lyrics: Lyrics,
    currentTimeProvider: () -> Long,
    isDarkTheme: Boolean = true,
    onSeekTo: (Long) -> Unit = {},
    songTitle: String,
    artistName: String,
    artworkUrl: String?
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val currentTime = currentTimeProvider()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Determine active line index
    var activeLineIndex by remember { mutableStateOf(-1) }
    
    // Selection Mode State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var isSharing by remember { mutableStateOf(false) }
    
    // Reset selection when lyrics change
    LaunchedEffect(lyrics) {
        isSelectionMode = false
        selectedIndices = emptySet()
    }
    
    LaunchedEffect(currentTime, lyrics) {
        if (lyrics.isSynced && !isSelectionMode) { // Pause scrolling during selection
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = 100.dp, 
                bottom = if (isSelectionMode) 180.dp else 200.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isActive = if (lyrics.isSynced) index == activeLineIndex else true
                val isSelected = selectedIndices.contains(index)
                
                // Opacity logic:
                // Normal mode: Active=1f, Inactive=0.5f
                // Selection mode: Selected=1f, Unselected=0.3f
                val targetAlpha = if (isSelectionMode) {
                    if (isSelected) 1f else 0.3f
                } else {
                    if (isActive || !lyrics.isSynced) 1f else 0.5f
                }
                
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 300), 
                    label = "alpha"
                )
                
                val scale by animateFloatAsState(
                    targetValue = if (isActive && lyrics.isSynced && !isSelectionMode) 1.1f else 1f,
                    animationSpec = tween(durationMillis = 300),
                    label = "scale"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    // Toggle selection
                                    val newSelection = selectedIndices.toMutableSet()
                                    if (isSelected) {
                                        newSelection.remove(index)
                                        if (newSelection.isEmpty()) {
                                            isSelectionMode = false // Exit if empty
                                        }
                                    } else {
                                        if (newSelection.size < 5) {
                                            newSelection.add(index)
                                        } else {
                                            // Max 5 lines reached feedback?
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    selectedIndices = newSelection
                                } else if (lyrics.isSynced && line.startTimeMs > 0) {
                                    onSeekTo(line.startTimeMs)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedIndices = setOf(index)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection Checkbox (Visible only in selection mode)
                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (isSelected) "Selected" else "Unselected",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp).padding(end = 16.dp)
                        )
                    }

                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        ),
                        color = textColor.copy(alpha = alpha),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Share Bar Overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        // Share Action
                        if (!isSharing && selectedIndices.isNotEmpty()) {
                            isSharing = true
                            coroutineScope.launch {
                                // Sort indices to keep lyrics in order
                                val sortedLines = selectedIndices.sorted().map { lyrics.lines[it].text }
                                
                                val uri = LyricsImageGenerator.generateAndShareImage(
                                    context = context,
                                    lyricsLines = sortedLines,
                                    songTitle = songTitle,
                                    artistName = artistName,
                                    artworkUrl = artworkUrl
                                )
                                
                                isSharing = false
                                
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
                                    
                                    // Optional: Exit mode after share?
                                    // isSelectionMode = false 
                                }
                            }
                        }
                    }
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generating...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Share Lyrics (${selectedIndices.size}/5)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
