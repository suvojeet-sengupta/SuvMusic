package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage

/**
 * Video surface that behaves like YouTube on a track change: the next video's
 * thumbnail and a spinner cover the surface until its first frame is drawn, so the
 * user never sees a black box or the previous video's frozen frame.
 */
@Composable
fun VideoSurface(
    player: Player,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
) {
    var mediaId by remember(player) { mutableStateOf(player.currentMediaItem?.mediaId) }
    var artwork by remember(player) { mutableStateOf(player.currentMediaItem?.mediaMetadata?.artworkUri) }
    var frameShown by remember(player) { mutableStateOf(player.videoSize.width > 0 && player.playbackState == Player.STATE_READY) }
    var buffering by remember(player) { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem?.mediaId != mediaId) frameShown = false
                mediaId = mediaItem?.mediaId
                artwork = mediaItem?.mediaMetadata?.artworkUri
            }

            override fun onRenderedFirstFrame() {
                frameShown = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setKeepContentOnPlayerReset(false)
                    this.resizeMode = resizeMode
                    this.player = player
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = !frameShown,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (artwork != null) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                }
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
            }
        }

        if (frameShown && buffering) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
        }
    }
}
