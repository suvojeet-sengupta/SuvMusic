package com.suvojeet.suvmusic.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.model.RepeatMode
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SuvMusicWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WidgetEntryPoint::class.java
        )
        val musicPlayer = entryPoint.getMusicPlayer()
        
        provideContent {
            GlanceTheme {
                val playerState by musicPlayer.playerState.collectAsState()
                val currentSong = playerState.currentSong
                
                var artworkBitmap by remember(currentSong?.id) { mutableStateOf<Bitmap?>(null) }

                // Recycle the previous bitmap when the song id changes or the
                // widget leaves composition. Each artwork is ~256KB ARGB; without
                // explicit recycle() the native bitmap memory only freed when the
                // GC ran and the JVM-side Bitmap was collected, which on long
                // listening sessions across many track skips kept widget memory
                // bloated.
                DisposableEffect(currentSong?.id) {
                    onDispose {
                        artworkBitmap?.let { bmp ->
                            if (!bmp.isRecycled) bmp.recycle()
                        }
                        artworkBitmap = null
                    }
                }

                LaunchedEffect(currentSong?.id) {
                    if (currentSong?.thumbnailUrl != null) {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(currentSong.thumbnailUrl)
                                .size(256, 256)
                                .build()
                            val result = context.imageLoader.execute(request)
                            if (result is SuccessResult) {
                                artworkBitmap = result.image.toBitmap()
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("SuvMusicWidget", "Artwork load failed: ${e.message}")
                        }
                    }
                }

                // Dynamic background color from PlayerState
                val domColor = playerState.dominantColor
                val backgroundColor = if (domColor != -16777216 && domColor != 0) {
                     Color(domColor).copy(alpha = 0.92f)
                } else {
                     Color(0xFF252836).copy(alpha = 0.95f)
                }
                
                val contentColor = Color.White
                val secondaryColor = Color.White.copy(alpha = 0.7f)

                // Main Container
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(backgroundColor))
                        .cornerRadius(24.dp)
                        .clickable(actionRunCallback<OpenAppAction>())
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Top Section: Artwork + Info
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Album Art
                            Box(
                                modifier = GlanceModifier
                                    .size(68.dp)
                                    .cornerRadius(12.dp)
                                    .background(ColorProvider(Color.White.copy(alpha = 0.1f))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (artworkBitmap != null) {
                                    Image(
                                        provider = ImageProvider(artworkBitmap!!),
                                        contentDescription = "Artwork",
                                        contentScale = ContentScale.Crop,
                                        modifier = GlanceModifier.fillMaxSize().cornerRadius(12.dp)
                                    )
                                } else {
                                     Image(
                                        provider = ImageProvider(R.drawable.ic_music_note),
                                        contentDescription = "Artwork",
                                        colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.5f))),
                                        modifier = GlanceModifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            // Info
                            Column(
                                modifier = GlanceModifier.defaultWeight()
                            ) {
                                Text(
                                    text = currentSong?.title ?: "Not Playing",
                                    style = TextStyle(
                                        color = ColorProvider(contentColor),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = currentSong?.artist ?: "SuvMusic Player",
                                    style = TextStyle(
                                        color = ColorProvider(secondaryColor),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Progress Line (Simplified to fixed height Box if fractional weight is unavailable)
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(ColorProvider(Color.White.copy(alpha = 0.15f)))
                                .cornerRadius(1.5.dp)
                        ) {
                            val progress = playerState.progress.coerceIn(0f, 1f)
                            if (progress > 0.05f) { // Only show if significant progress
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth() // Fill container, but since it's a Box, we can't easily do fractional width without weight
                                        .fillMaxHeight()
                                        .background(ColorProvider(contentColor))
                                        .cornerRadius(1.5.dp)
                                ) {}
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // Bottom Section: All Controls
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Shuffle
                            Image(
                                provider = ImageProvider(
                                    if (playerState.shuffleEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
                                ),
                                contentDescription = "Shuffle",
                                colorFilter = ColorFilter.tint(ColorProvider(if (playerState.shuffleEnabled) contentColor else secondaryColor)),
                                modifier = GlanceModifier
                                    .size(24.dp)
                                    .clickable(actionRunCallback<ToggleShuffleAction>())
                            )
                            
                            Spacer(modifier = GlanceModifier.width(18.dp))
                            
                            // Previous
                            Image(
                                provider = ImageProvider(R.drawable.ic_skip_previous),
                                contentDescription = "Previous",
                                colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                modifier = GlanceModifier
                                    .size(30.dp)
                                    .clickable(actionRunCallback<PreviousAction>())
                            )
                            
                            Spacer(modifier = GlanceModifier.width(18.dp))
                            
                            // Play/Pause
                            Box(
                                modifier = GlanceModifier
                                    .size(52.dp)
                                    .background(ColorProvider(Color.White.copy(alpha = 0.15f)))
                                    .cornerRadius(26.dp)
                                    .clickable(actionRunCallback<PlayPauseAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(
                                        if (playerState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                    ),
                                    contentDescription = "Play/Pause",
                                    colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                    modifier = GlanceModifier.size(30.dp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.width(18.dp))
                            
                            // Next
                            Image(
                                provider = ImageProvider(R.drawable.ic_skip_next),
                                contentDescription = "Next",
                                colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                modifier = GlanceModifier
                                    .size(30.dp)
                                    .clickable(actionRunCallback<NextAction>())
                            )
                            
                            Spacer(modifier = GlanceModifier.width(18.dp))
                            
                            // Repeat
                            val repeatIcon = when (playerState.repeatMode) {
                                RepeatMode.ONE -> R.drawable.ic_repeat_one_on
                                RepeatMode.ALL -> R.drawable.ic_repeat_all_on
                                RepeatMode.OFF -> R.drawable.ic_repeat
                            }
                            Image(
                                provider = ImageProvider(repeatIcon),
                                contentDescription = "Repeat",
                                colorFilter = ColorFilter.tint(ColorProvider(if (playerState.repeatMode != RepeatMode.OFF) contentColor else secondaryColor)),
                                modifier = GlanceModifier
                                    .size(24.dp)
                                    .clickable(actionRunCallback<ToggleRepeatAction>())
                            )
                        }
                    }
                }
            }
        }
    }
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun getMusicPlayer(): MusicPlayer
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, SuvMusicWidget.WidgetEntryPoint::class.java)
        entryPoint.getMusicPlayer().togglePlayPause()
        SuvMusicWidget().update(context, glanceId)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, SuvMusicWidget.WidgetEntryPoint::class.java)
        entryPoint.getMusicPlayer().seekToNext()
        SuvMusicWidget().update(context, glanceId)
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, SuvMusicWidget.WidgetEntryPoint::class.java)
        entryPoint.getMusicPlayer().seekToPrevious()
        SuvMusicWidget().update(context, glanceId)
    }
}

class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, SuvMusicWidget.WidgetEntryPoint::class.java)
        entryPoint.getMusicPlayer().toggleShuffle()
        SuvMusicWidget().update(context, glanceId)
    }
}

class ToggleRepeatAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, SuvMusicWidget.WidgetEntryPoint::class.java)
        entryPoint.getMusicPlayer().toggleRepeat()
        SuvMusicWidget().update(context, glanceId)
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let { 
            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it) 
        }
    }
}

