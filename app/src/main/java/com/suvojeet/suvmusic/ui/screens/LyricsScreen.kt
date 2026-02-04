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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.animateContentSize
import com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType
import com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition
import com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.suvojeet.suvmusic.ui.utils.LyricsPdfGenerator
import com.suvojeet.suvmusic.ui.utils.MoodDetector
import com.suvojeet.suvmusic.ui.components.DynamicLyricsBackground

@OptIn(ExperimentalMaterial3Api::class)
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
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    selectedProvider: com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType = com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.AUTO,
    enabledProviders: Map<com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType, Boolean> = emptyMap(),
    onProviderChange: (com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType) -> Unit = {},
    lyricsTextPosition: LyricsTextPosition = LyricsTextPosition.CENTER,
    lyricsAnimationType: LyricsAnimationType = LyricsAnimationType.WORD,
    lyricsLineSpacing: Float = 1.5f,
    lyricsFontSize: Float = 26f,
    onLineSpacingChange: (Float) -> Unit = {},
    onFontSizeChange: (Float) -> Unit = {},
    onTextPositionChange: (LyricsTextPosition) -> Unit = {},
    onAnimationTypeChange: (LyricsAnimationType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    // Lyrics Settings State
    // Lyrics Settings State
    var syncOffset by remember { mutableStateOf(0L) }
    var keepScreenOn by remember { mutableStateOf(true) } // Default to true for lyrics
    
    // We keep local state for these to allow immediate UI updates, 
    // but ideally they should be persisted or passed back up.
    // For now, we'll use the passed parameters as initial values.
    // var currentTextPosition by remember { mutableStateOf(lyricsTextPosition) }
    // var currentAnimationType by remember { mutableStateOf(lyricsAnimationType) }

    // Keep Screen On Effect
    val currentView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(keepScreenOn) {
        currentView.keepScreenOn = keepScreenOn
        onDispose {
            currentView.keepScreenOn = false
        }
    }
    
    // Formatting helper for seek bar
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    
    // Detect Mood/Style
    val currentStyle = remember(songTitle, artistName, lyrics) {
        val text = lyrics?.lines?.joinToString(" ") { it.text } ?: ""
        MoodDetector.detectStyle(songTitle, artistName, text)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Block volume gesture from parent by consuming vertical drags
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            }
    ) {
        // Dynamic Background
        DynamicLyricsBackground(
            artworkUrl = artworkUrl,
            style = currentStyle,
            isDarkTheme = isDarkTheme
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings Button (Top Left)
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Settings",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share Button (Middle)
                IconButton(
                    onClick = { showShareSheet = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Lyrics",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Close Button (Top Right)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(42.dp)
                        .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Song Info Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp
                    ),
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isFetching) {
                    val loadingMessages = remember {
                        listOf(
                            "Polishing the lyrics...",
                            "Preparing your concert experience...",
                            "Tuning the vocal cords...",
                            "Finding the rhythm...",
                            "Did you hear about the musician who locked their keys in the car? They had to break the window to let the bassist out.",
                            "Why did the singer go to the doctor? Because they had too many 'bars'!",
                            "Teaching the app how to sing...",
                            "Gathering the words...",
                            "Fetching the soul of the song...",
                            "Wait a second, the lyrics are taking a scenic route..."
                        )
                    }
                    val currentMessage = remember { loadingMessages.random() }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = currentMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
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
                    // Effective time provider with sync offset
                    val effectiveTimeProvider = { currentTimeProvider() + syncOffset }
                    
                    LyricsList(
                        lyrics = lyrics,
                        currentTimeProvider = effectiveTimeProvider,
                        isDarkTheme = isDarkTheme,
                        onSeekTo = { pos -> onSeekTo(pos - syncOffset) }, // Adjust seek back
                        songTitle = songTitle,
                        artistName = artistName,
                        artworkUrl = artworkUrl,
                        textPosition = lyricsTextPosition, // Use passed param directly
                        animationType = lyricsAnimationType, // Use passed param directly
                        fontSize = lyricsFontSize,
                        lineSpacingMultiplier = lyricsLineSpacing
                    )
                }
            }
            
            // Footer (Source Credit)
            val credit = lyrics?.sourceCredit
            if (credit != null) {
                // Show provider pill
                Surface(
                    color = textColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = credit,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
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

                        // Playback Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = onPlayPause,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(textColor, androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = backgroundColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

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
        
        // Settings Sheet
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState,
                containerColor = Color.Black.copy(alpha = 0.85f), // Semi-transparent black for immersion
                contentColor = Color.White,
                dragHandle = { 
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Font Size & Spacing Combined Row for compactness
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Font Size
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Size", 
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FormatSize, 
                                    null, 
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Slider(
                                    value = lyricsFontSize,
                                    onValueChange = onFontSizeChange,
                                    valueRange = 16f..50f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Line Spacing
                        Column(modifier = Modifier.weight(1f)) {
                             Text(
                                text = "Spacing", 
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FormatAlignLeft, 
                                    null, 
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Slider(
                                    value = lyricsLineSpacing,
                                    onValueChange = onLineSpacingChange,
                                    valueRange = 1.0f..2.5f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                     colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Alignment & Screen On
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Alignment", 
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(4.dp)
                            ) {
                                LyricsTextPosition.entries.forEach { position ->
                                    val isSelected = lyricsTextPosition == position
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { onTextPositionChange(position) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = position.name.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Screen On Toggle
                         Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Screen On", 
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Switch(
                                checked = keepScreenOn, 
                                onCheckedChange = { keepScreenOn = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color.White.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Sync Correction
                    Text(
                        text = "Sync Correction",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { syncOffset -= 500L },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Text("-0.5s", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${if (syncOffset > 0) "+" else ""}${syncOffset}ms",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (syncOffset != 0L) MaterialTheme.colorScheme.primary else Color.White
                            )
                            if (syncOffset != 0L) {
                                Text(
                                    text = "Tap to reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable { syncOffset = 0L }
                                )
                            }
                        }

                        IconButton(
                            onClick = { syncOffset += 500L },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Text("+0.5s", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Source Provider
                    Text(
                        text = "Source",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    var expandedProvider by remember { mutableStateOf(false) }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable(
                                onClick = { expandedProvider = !expandedProvider },
                                indication = LocalIndication.current,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(16.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                null,
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Provider",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = selectedProvider.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                             Icon(
                                imageVector = if(expandedProvider) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        
                        if (expandedProvider) {
                             Spacer(modifier = Modifier.height(16.dp))
                             HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                             Spacer(modifier = Modifier.height(8.dp))
                             
                             com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.entries.forEach { provider ->
                                val isEnabled = enabledProviders[provider] ?: true
                                val isSelected = provider == selectedProvider
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isEnabled) {
                                            onProviderChange(provider)
                                            expandedProvider = false
                                        }
                                        .padding(vertical = 12.dp)
                                        .alpha(if (isEnabled) 1f else 0.5f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        enabled = isEnabled,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color.White,
                                            unselectedColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                             }
                        }
                    }
                }
            }
        }
        
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Share Lyrics",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Option 1: Share as Text
                    ListItem(
                        headlineContent = { Text("Share as Text") },
                        leadingContent = { 
                            Icon(Icons.Default.Description, contentDescription = null) 
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                sheetState.hide()
                                showShareSheet = false
                                
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
                            }
                        }
                    )
                    
                    // Option 2: Export as PDF
                    ListItem(
                        headlineContent = { Text("Export as PDF") },
                        leadingContent = { 
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null) 
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                // Close sheet first
                                sheetState.hide()
                                showShareSheet = false
                                
                                if (lyrics != null) {
                                    val lines = lyrics.lines.map { it.text }
                                    val uri = LyricsPdfGenerator.generateAndSharePdf(
                                        context,
                                        lines,
                                        songTitle,
                                        artistName
                                    )
                                    
                                    if (uri != null) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Lyrics saved to Documents/SuvMusic",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics PDF"))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LyricsList(
    lyrics: Lyrics,
    currentTimeProvider: () -> Long,
    isDarkTheme: Boolean = true,
    onSeekTo: (Long) -> Unit = {},
    songTitle: String,
    artistName: String,
    artworkUrl: String?,
    textPosition: LyricsTextPosition = LyricsTextPosition.CENTER,
    animationType: LyricsAnimationType = LyricsAnimationType.WORD,
    fontSize: Float = 24f,
    lineSpacingMultiplier: Float = 1.5f
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val currentTime = currentTimeProvider()



    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Determine layout alignment from settings
    val alignment = when (textPosition) {
        LyricsTextPosition.CENTER -> Alignment.Center
        LyricsTextPosition.LEFT -> Alignment.CenterStart
        LyricsTextPosition.RIGHT -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    
    val textAlign = when (textPosition) {
        LyricsTextPosition.CENTER -> TextAlign.Center
        LyricsTextPosition.LEFT -> TextAlign.Start
        LyricsTextPosition.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }
    
    val flowArrangement = when (textPosition) {
        LyricsTextPosition.CENTER -> Arrangement.Center
        LyricsTextPosition.LEFT -> Arrangement.Start
        LyricsTextPosition.RIGHT -> Arrangement.End
        else -> Arrangement.Center
    }
    
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
    
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var lastUserInteractionTime by remember { mutableLongStateOf(0L) }
    var isAutoScrolling by remember { mutableStateOf(false) }

    // Track user interaction
    LaunchedEffect(isDragged, listState.isScrollInProgress) {
        if (isDragged || (listState.isScrollInProgress && !isAutoScrolling)) {
            lastUserInteractionTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(currentTime, lyrics) {
        if (lyrics.isSynced && !isSelectionMode) {
            // Check if we should auto-scroll
            val timeSinceInteraction = System.currentTimeMillis() - lastUserInteractionTime
            val shouldAutoScroll = timeSinceInteraction > 5000 // 5 seconds delay
            
            if (shouldAutoScroll) {
                // Find the current line
                // A line is active if currentTime >= startTime && currentTime < endTime (or next line start)
                val index = lyrics.lines.indexOfLast { it.startTimeMs <= currentTime }
                if (index != activeLineIndex && index >= 0) {
                    activeLineIndex = index
                    // Scroll to center
                    try {
                         isAutoScrolling = true
                         listState.animateScrollToItem(
                            index = index,
                            scrollOffset = -400 // Approximate offset to center
                        )
                        isAutoScrolling = false
                    } catch (e: Exception) {
                        isAutoScrolling = false
                    }
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
                // Normal mode: Active=1f, Inactive=0.3f (Dimmed more for contrast)
                // Selection mode: Selected=1f, Unselected=0.3f
                val targetAlpha = if (isSelectionMode) {
                    if (isSelected) 1f else 0.3f
                } else {
                    if (isActive) 1f else 0.3f
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
                                fontSize = fontSize.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (fontSize * lineSpacingMultiplier).sp
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
                            .combinedClickable(
                                onClick = {
                                    if (lyrics.isSynced && line.startTimeMs > 0) {
                                        onSeekTo(line.startTimeMs)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isSelectionMode = true
                                        selectedIndices = setOf(index)
                                    }
                                }
                            )
                            .padding(horizontal = 32.dp, vertical = 8.dp) // Reduced vertical padding
                    ) {
                        // Use word-by-word ONLY if enabled and available
                        val words = line.words
                        if (words != null && words.isNotEmpty() && lyrics.isSynced && animationType == LyricsAnimationType.WORD) {
                            // Word-by-word rendering
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = flowArrangement,
                                verticalArrangement = Arrangement.Center
                            ) {
                                words.forEach { word ->
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
                                            fontSize = fontSize.sp, 
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = (fontSize * lineSpacingMultiplier).sp
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
                                    fontSize = fontSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (fontSize * lineSpacingMultiplier).sp
                                ),
                                color = textColor.copy(alpha = lineAlpha),
                                textAlign = textAlign,
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
