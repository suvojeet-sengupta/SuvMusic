package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Intent
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType
import com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType
import com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition
import com.suvojeet.suvmusic.util.LyricsPdfGenerator
import com.suvojeet.suvmusic.util.MoodDetector
import com.suvojeet.suvmusic.ui.components.DynamicLyricsBackground
import com.suvojeet.suvmusic.ui.components.BounceButton
import com.suvojeet.suvmusic.ui.utils.LyricsImageGenerator
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    songId: String = "",
    // New parameters
    duration: Long = 0L,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    selectedProvider: com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType = com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.AUTO,
    enabledProviders: Map<com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType, Boolean> = emptyMap(),
    onProviderChange: (com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType) -> Unit = {},
    onImportLyrics: (String) -> Unit = {},
    lyricsTextPosition: LyricsTextPosition = LyricsTextPosition.CENTER,
    lyricsAnimationType: LyricsAnimationType = LyricsAnimationType.WORD,
    lyricsLineSpacing: Float = 1.5f,
    lyricsFontSize: Float = 26f,
    lyricsBlur: Float = 2.5f,
    onLineSpacingChange: (Float) -> Unit = {},
    onFontSizeChange: (Float) -> Unit = {},
    onBlurChange: (Float) -> Unit = {},
    onTextPositionChange: (LyricsTextPosition) -> Unit = {},
    onAnimationTypeChange: (LyricsAnimationType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    // M3E Fast Expressive color transitions
    val animatedBgColor: Color by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "bgColor"
    )
    val animatedTextColor: Color by animateColorAsState(
        targetValue = textColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "textColor"
    )
    
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    // Lyrics Settings State
    var syncOffset by remember { mutableStateOf(0L) }

    // File Picker for Lyrics
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val content = inputStream.bufferedReader().readText()
                    if (content.isNotBlank()) {
                        onImportLyrics(content)
                        // Switch to Local provider automatically
                        onProviderChange(com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.LOCAL)
                        com.suvojeet.suvmusic.util.SnackbarUtil.showSuccess("Lyrics imported successfully")
                    }
                }
            } catch (e: Exception) {
                com.suvojeet.suvmusic.util.SnackbarUtil.showError("Failed to import lyrics: ${e.message}")
            }
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
            .background(animatedBgColor)
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
            // Reorganized Header - More compact to give space for lyrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BounceButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { isPressed ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(animatedTextColor.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Settings",
                                tint = animatedTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    BounceButton(
                        onClick = { showShareSheet = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { isPressed ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(animatedTextColor.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = animatedTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center - Song Info (Title & Artist)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = animatedTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = animatedTextColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Right - Close Button
                BounceButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { isPressed ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(animatedTextColor.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = animatedTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                        LoadingIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
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
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        BounceButton(
                            onClick = { filePicker.launch("*/*") },
                            modifier = Modifier.height(48.dp).padding(horizontal = 32.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { isPressed ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(textColor.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, null, tint = textColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Import Local Lyrics", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = textColor)
                                }
                            }
                        }
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
                        lineSpacingMultiplier = lyricsLineSpacing,
                        blurIntensity = lyricsBlur
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
            
            // Bottom Controls and Seek Bar
            if (duration > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .background(
                            animatedTextColor.copy(alpha = 0.04f),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = animatedTextColor,
                            activeTrackColor = animatedTextColor.copy(alpha = 0.8f),
                            inactiveTrackColor = animatedTextColor.copy(alpha = 0.15f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(if (sliderPosition != null) (sliderPosition!! * duration).toLong() else currentTimeProvider()),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = animatedTextColor.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = animatedTextColor.copy(alpha = 0.5f)
                        )
                    }

                    // Playback Controls Row - Centered and Ergonomic
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BounceButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(52.dp)
                        ) { isPressed ->
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = animatedTextColor.copy(alpha = if (isPressed) 0.6f else 0.9f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        BounceButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) { isPressed ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(animatedTextColor, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = animatedBgColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        BounceButton(
                            onClick = onNext,
                            modifier = Modifier.size(52.dp)
                        ) { isPressed ->
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = animatedTextColor.copy(alpha = if (isPressed) 0.6f else 0.9f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
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
                containerColor = Color.Black.copy(alpha = 0.92f), // More immersive dark
                contentColor = Color.White,
                dragHandle = { 
                    BottomSheetDefaults.DragHandle(
                        color = Color.White.copy(alpha = 0.4f)
                    )
                },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Customize Lyrics",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Appearance Section
                    SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Tune)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(20.dp)
                    ) {
                        // Font Size
                        SettingsSlider(
                            label = "Font Size",
                            value = lyricsFontSize,
                            onValueChange = onFontSizeChange,
                            valueRange = 16f..50f,
                            icon = Icons.Default.FormatSize
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Line Spacing
                        SettingsSlider(
                            label = "Line Spacing",
                            value = lyricsLineSpacing,
                            onValueChange = onLineSpacingChange,
                            valueRange = 1.0f..2.5f,
                            icon = Icons.Default.FormatAlignLeft
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Blur Intensity
                        SettingsSlider(
                            label = "Blur Intensity",
                            value = lyricsBlur,
                            onValueChange = onBlurChange,
                            valueRange = 0f..12f,
                            icon = Icons.Default.BlurOn
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Alignment Selection
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Alignment",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LyricsTextPosition.entries.forEach { position ->
                                val isSelected = lyricsTextPosition == position
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { onTextPositionChange(position) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = position.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sync Correction Section
                    SettingsSectionHeader(title = "Sync Correction", icon = Icons.Default.Tune)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BounceButton(
                            onClick = { syncOffset -= 500L },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { isPressed ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-0.5s", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${if (syncOffset > 0) "+" else ""}${syncOffset}ms",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = if (syncOffset != 0L) MaterialTheme.colorScheme.primary else Color.White
                            )
                            if (syncOffset != 0L) {
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { syncOffset = 0L }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        BounceButton(
                            onClick = { syncOffset += 500L },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { isPressed ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+0.5s", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Local Lyrics Section
                    SettingsSectionHeader(title = "Local Lyrics", icon = Icons.Default.LibraryMusic)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Add your own .lrc or .txt file for this song.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        BounceButton(
                            onClick = { filePicker.launch("*/*") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { isPressed ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Import .lrc / .txt", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Provider Section
                    SettingsSectionHeader(title = "Lyrics Source", icon = Icons.Default.LibraryMusic)
                    
                    var expandedProvider by remember { mutableStateOf(false) }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { expandedProvider = !expandedProvider }
                            .padding(20.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Provider",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = selectedProvider.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                             Icon(
                                imageVector = if(expandedProvider) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        
                        if (expandedProvider) {
                             Spacer(modifier = Modifier.height(16.dp))
                             HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                             Spacer(modifier = Modifier.height(8.dp))
                             
                             com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType.entries.forEach { provider ->
                                val isEnabled = enabledProviders[provider] ?: true
                                val isSelected = provider == selectedProvider
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = isEnabled) {
                                            onProviderChange(provider)
                                            expandedProvider = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                        .alpha(if (isEnabled) 1f else 0.4f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        enabled = isEnabled,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color.White,
                                            unselectedColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
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
                                        com.suvojeet.suvmusic.util.SnackbarUtil.showSuccess(
                                            "Lyrics saved to Documents/SuvMusic"
                                        )

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

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
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
    lineSpacingMultiplier: Float = 1.5f,
    blurIntensity: Float = 2.5f
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    // M3E Fast Expressive text color transition
    val animatedTextColor: Color by animateColorAsState(
        targetValue = textColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "listTextColor"
    )
    
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
            // Find the current line
            val index = lyrics.lines.indexOfLast { it.startTimeMs <= currentTime }
            if (index != activeLineIndex && index >= 0) {
                activeLineIndex = index
                
                // Check if we should auto-scroll
                val timeSinceInteraction = System.currentTimeMillis() - lastUserInteractionTime
                val shouldAutoScroll = timeSinceInteraction > 2000 // Faster auto-resume
                
                if (shouldAutoScroll) {
                    try {
                        isAutoScrolling = true
                        
                        // Calculate offset to position item at 1/3 of the viewport
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val topOffset = viewportHeight / 3 
                        
                        listState.animateScrollToItem(
                            index = index,
                            scrollOffset = -topOffset
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
                top = 120.dp, // Reduced top padding to allow sticking higher
                bottom = 450.dp // High bottom padding to ensure last lines can reach the top target
            ),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.15f to Color.Black,
                            0.85f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                }
        ) {
            itemsIndexed(lyrics.lines, key = { index, _ -> index }) { index, line ->
                val isActive = if (lyrics.isSynced) index == activeLineIndex else true
                val isSelected = selectedIndices.contains(index)
                
                // Opacity logic:
                // Normal mode: Active=1f, Inactive=0.25f + blur
                // Selection mode: Selected=1f, Unselected=0.3f
                val targetAlpha = if (isSelectionMode) {
                    if (isSelected) 1f else 0.3f
                } else {
                    if (isActive) 1f else 0.25f
                }
                
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), 
                    label = "alpha"
                )
                
                val scale by animateFloatAsState(
                    targetValue = if (isActive && lyrics.isSynced && !isSelectionMode) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                    label = "scale"
                )

                val blurRadius by animateFloatAsState(
                    targetValue = if (isActive || isSelectionMode || !lyrics.isSynced || activeLineIndex == -1) 0f else blurIntensity,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                    label = "blur"
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
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else animatedTextColor.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp).padding(end = 16.dp)
                            )
                        }
                        
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = fontSize.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (fontSize * (lineSpacingMultiplier * 1.1f)).sp
                            ),
                            color = animatedTextColor.copy(alpha = alpha),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Synced Lyrics View
                    // Word-by-word or standard line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .blur(blurRadius.dp)
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
                            .padding(horizontal = 32.dp, vertical = 10.dp)
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
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                                        label = "wordAlpha"
                                    )
                                    
                                    Text(
                                        text = word.text + " ",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = (if (isActive) fontSize * 1.05f else fontSize).sp, 
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = (fontSize * (lineSpacingMultiplier * 1.1f)).sp
                                        ),
                                        color = animatedTextColor.copy(alpha = wordAlpha),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        } else {
                            // Standard line rendering
                            val lineAlpha by animateFloatAsState(
                                targetValue = if (isActive || !lyrics.isSynced) 1f else 0.25f, // Dim inactive lines more for better focus
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                                label = "lineAlpha"
                            )
                            
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = (if (isActive && lyrics.isSynced) fontSize * 1.05f else fontSize).sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (fontSize * (lineSpacingMultiplier * 1.1f)).sp
                                ),
                                color = animatedTextColor.copy(alpha = lineAlpha),
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
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
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
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
