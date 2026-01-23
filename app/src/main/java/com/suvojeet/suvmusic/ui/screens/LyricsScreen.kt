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
    // New parameters
    duration: Long = 0L,
    selectedProvider: com.suvojeet.suvmusic.data.model.LyricsProviderType = com.suvojeet.suvmusic.data.model.LyricsProviderType.AUTO,
    onProviderChange: (com.suvojeet.suvmusic.data.model.LyricsProviderType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    val context = LocalContext.current
    var showProviderDialog by remember { mutableStateOf(false) }
    
    // Formatting helper for seek bar
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    
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

        if (showProviderDialog) {
            AlertDialog(
                onDismissRequest = { showProviderDialog = false },
                title = { Text("Lyrics Source") },
                text = {
                    Column {
                        com.suvojeet.suvmusic.data.model.LyricsProviderType.entries.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onProviderChange(provider)
                                        showProviderDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = provider == selectedProvider,
                                    onClick = null // Handled by Row click
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface 
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProviderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider Switch Button (Top Left)
                IconButton(
                    onClick = { showProviderDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(textColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, // Use a more appropriate icon like 'Tune' or 'Settings' if available, but CheckCircle works for now
                        contentDescription = "Switch Provider",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share Button (Middle Left)
                IconButton(
                    onClick = {
                        val shareText = buildString {
                            append("$songTitle - $artistName\n\n")
                            lyrics?.lines?.forEach { line ->
                                append(line.text)
                                append("\n")
                            }
                            append("\nShared via SuvMusic")
                        }
                        
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Lyrics")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(textColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Lyrics",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close Button (Top Right)
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
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Bottom Seek Bar
            if (duration > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                     var sliderPosition by remember { mutableStateOf<Float?>(null) }
                     val progress = sliderPosition ?: (currentTimeProvider().toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                     
                     Slider(
                         value = progress,
                         onValueChange = { sliderPosition = it },
                         onValueChangeFinished = {
                             sliderPosition?.let {
                                 onSeekTo((it * duration).toLong())
                                 sliderPosition = null
                             }
                         },
                         modifier = Modifier.fillMaxWidth().height(20.dp),
                         colors = SliderDefaults.colors(
                             thumbColor = textColor,
                             activeTrackColor = textColor.copy(alpha = 0.8f),
                             inactiveTrackColor = textColor.copy(alpha = 0.2f)
                         )
                     )
                     
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween
                     ) {
                         Text(
                             text = formatTime(if (sliderPosition != null) (sliderPosition!! * duration).toLong() else currentTimeProvider()),
                             style = MaterialTheme.typography.labelSmall,
                             color = textColor.copy(alpha = 0.6f)
                         )
                         Text(
                             text = formatTime(duration),
                             style = MaterialTheme.typography.labelSmall,
                             color = textColor.copy(alpha = 0.6f)
                         )
                     }
                }
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

                if (isSelectionMode) {
                    val isSelected = selectedIndices.contains(index)
                    val alpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.3f, 
                        label = "alpha"
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSelection = selectedIndices.toMutableSet()
                                if (isSelected) {
                                    newSelection.remove(index)
                                    if (newSelection.isEmpty()) isSelectionMode = false 
                                } else {
                                    if (newSelection.size < 5) newSelection.add(index)
                                }
                                selectedIndices = newSelection
                            }
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                } else {
                    // Synced Lyrics View
                    val isActive = lyrics.isSynced && index == activeLineIndex
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.05f else 1f, // Reduced scale for smoother feel
                        animationSpec = tween(durationMillis = 300),
                        label = "scale"
                    )

                    // Word-by-word or standard line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (lyrics.isSynced && line.startTimeMs > 0) {
                                    onSeekTo(line.startTimeMs)
                                }
                            }
                            .padding(horizontal = 32.dp, vertical = 8.dp) // Reduced vertical padding
                    ) {
                        if (line.words != null && line.words.isNotEmpty() && lyrics.isSynced) {
                            // Word-by-word rendering
                            OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                line.words.forEach { word ->
                                    // Word is active if currentTime is within [startTime, endTime]
                                    // OR if it's in the past of the current line but we're still processing this line
                                    val isWordActive = isActive && currentTime >= word.startTimeMs
                                    
                                    val wordAlpha by animateFloatAsState(
                                        targetValue = if (isActive) {
                                            if (currentTime >= word.startTimeMs) 1f else 0.4f
                                        } else 0.3f, // Inactive lines are dimmed
                                        animationSpec = tween(300),
                                        label = "wordAlpha"
                                    )
                                    
                                    val wordColor = if (isActive && currentTime >= word.startTimeMs) {
                                        textColor // Highlighted
                                    } else {
                                        textColor // Alpha handles the dimming
                                    }

                                    // Add glow effect for currently sung word?
                                    // For now, simple opacity transition is cleaner
                                    
                                    Text(
                                        text = word.text,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = 26.sp, // Slightly larger
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 40.sp
                                        ),
                                        color = wordColor.copy(alpha = wordAlpha),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        } else {
                            // Standard line rendering
                            val lineAlpha by animateFloatAsState(
                                targetValue = if (isActive || !lyrics.isSynced) 1f else 0.3f, // Dim inactive lines more
                                animationSpec = tween(300),
                                label = "lineAlpha"
                            )
                            
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 36.sp
                                ),
                                color = textColor.copy(alpha = lineAlpha),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth() // Ensuring full width for text
                            )
                        }
                    }
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
