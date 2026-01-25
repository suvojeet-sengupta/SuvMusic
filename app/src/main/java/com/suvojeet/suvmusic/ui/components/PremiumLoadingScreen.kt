package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun PremiumLoadingScreen(
    thumbnailUrl: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if we are in dark theme based on system background luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val overlayColor = if (isDarkTheme) Color.Black else Color.White

    val loadingMessages = remember {
        listOf(
            "Polishing the audio tracks...",
            "Preparing your VIP experience...",
            "Tidying up the playlist...",
            "Finding the rhythmic soul...",
            "Warming up the speakers...",
            "Gathering the melodies...",
            "Fetching the musical vibes...",
            "Just a moment, music is magic...",
            "Creating a harmonious space...",
            "Almost ready to play..."
        )
    }
    val currentMessage = remember { loadingMessages.random() }

    // Animated dots logic
    val transition = rememberInfiniteTransition(label = "dots")
    val dotCount by transition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    val dots = ".".repeat(dotCount)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Blurred Background
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(if (isDarkTheme) 0.5f else 0.4f),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                overlayColor.copy(alpha = 0.4f),
                                overlayColor.copy(alpha = 0.7f),
                                overlayColor.copy(alpha = 0.9f)
                            )
                        } else {
                            listOf(
                                overlayColor.copy(alpha = 0.3f),
                                overlayColor.copy(alpha = 0.6f),
                                overlayColor.copy(alpha = 0.8f)
                            )
                        }
                    )
                )
        )

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textColor
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentMessage + dots,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }
    }
}
