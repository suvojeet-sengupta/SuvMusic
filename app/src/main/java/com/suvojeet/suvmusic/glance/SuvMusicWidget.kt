package com.suvojeet.suvmusic.glance

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.suvojeet.suvmusic.R
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
                
                // Dynamic background color from PlayerState (default to dark sleek color)
                // Use a fallback if dominantColor is invalid/default (-16777216 is Black)
                val domColor = playerState.dominantColor
                val backgroundColor = if (domColor != -16777216 && domColor != 0) {
                     Color(domColor).copy(alpha = 0.95f) // Slightly transparent/glassy
                } else {
                     Color(0xFF252836).copy(alpha = 0.95f) // Sleek dark blue-grey fallback
                }
                
                val contentColor = Color.White
                val secondaryColor = Color.White.copy(alpha = 0.7f)

                // Main Container
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(backgroundColor))
                        .cornerRadius(24.dp) // Large rounded corners as per image
                        .clickable(actionRunCallback<OpenAppAction>())
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp), // Consistent padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art (Left Side)
                        // Use a solid colored box with music icon as placeholder
                        // This avoids the transparent/checkered look
                        Box(
                            modifier = GlanceModifier
                                .size(80.dp)
                                .cornerRadius(12.dp)
                                .background(ColorProvider(Color(0xFF3D3D50))), // Solid dark purple-grey
                            contentAlignment = Alignment.Center
                        ) {
                             Image(
                                provider = ImageProvider(R.drawable.ic_music_note),
                                contentDescription = "Artwork",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.8f))),
                                modifier = GlanceModifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(16.dp))

                        // Right Side: Info + Controls
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top Row: Title + App Icon
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = GlanceModifier.defaultWeight()
                                ) {
                                    Text(
                                        text = currentSong?.title ?: "Select a song",
                                        style = TextStyle(
                                            color = ColorProvider(contentColor),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = GlanceModifier.height(4.dp))
                                    Text(
                                        text = currentSong?.artist ?: "SuvMusic",
                                        style = TextStyle(
                                            color = ColorProvider(secondaryColor),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        maxLines = 1
                                    )
                                }
                                
                                // Music Note Icon (Top Right)
                                Image(
                                    provider = ImageProvider(R.drawable.ic_music_note), // Ensure this exists
                                    contentDescription = "App Icon",
                                    colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                    modifier = GlanceModifier.size(16.dp).padding(start = 8.dp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            
                            // Bottom Row: Controls (Right Aligned in the column, or spread)
                            // Image shows spacing.
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End, // Align to right like screenshot? 
                                // Actually screenshot shows them spaced out but arguably centered/right-ish.
                                // Let's use SpaceEvenly or SpaceBetween for the control row itself?
                                // Screenshot: Prev - Play - Next are clustered somewhat.
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Spacer to push controls to right if desired, OR just center them in the available width
                                // Let's spread them out slightly.
                                
                                // Previous
                                Image(
                                    provider = ImageProvider(R.drawable.ic_skip_previous),
                                    contentDescription = "Previous",
                                    colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                    modifier = GlanceModifier
                                        .size(32.dp)
                                        .clickable(actionRunCallback<PreviousAction>())
                                )
                                
                                Spacer(modifier = GlanceModifier.width(24.dp))
                                
                                // Play/Pause (Larger)
                                Image(
                                    provider = ImageProvider(
                                        if (playerState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                    ),
                                    contentDescription = "Play/Pause",
                                    colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                    modifier = GlanceModifier
                                        .size(40.dp)
                                        .clickable(actionRunCallback<PlayPauseAction>())
                                )
                                
                                Spacer(modifier = GlanceModifier.width(24.dp))
                                
                                // Next
                                Image(
                                    provider = ImageProvider(R.drawable.ic_skip_next),
                                    contentDescription = "Next",
                                    colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                                    modifier = GlanceModifier
                                        .size(32.dp)
                                        .clickable(actionRunCallback<NextAction>())
                                )
                            }
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

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let { context.startActivity(it) }
    }
}
