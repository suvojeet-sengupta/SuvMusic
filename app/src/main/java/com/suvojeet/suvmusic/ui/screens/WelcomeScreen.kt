package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.viewmodel.WelcomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onSkipLogin: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    // Brand Colors for the "Player-like" gradient
    // Using a deep violet/blue as the primary brand color against pitch black
    val brandPrimary = Color(0xFF6200EA) // Deep Purple Accent
    val brandSecondary = Color(0xFF3700B3) // Darker Purple
    val pitchBlack = Color.Black

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var startAnimation by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "contentAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pitchBlack)
    ) {
        // 1. Background Gradient Glow (Inspired by Player Screen)
        // A subtle gradient from top-left/center fading into black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            brandSecondary.copy(alpha = 0.5f), // Top
                            brandPrimary.copy(alpha = 0.2f),   // Middle
                            pitchBlack                         // Bottom
                        ),
                        startY = 0f,
                        endY = 1500f // Extend down a bit
                    )
                )
        )

        // 2. Ambient Blobs for extra "Player" feel (optional, kept subtle)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-50).dp)
                .size(300.dp)
                .blur(100.dp)
                .background(brandPrimary.copy(alpha = 0.3f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Logo / Header (Static)
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .padding(horizontal = 24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SuvMusic",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(contentAlpha),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { page ->
                // Individual Page Content
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    WelcomePageContent(page = page)
                }
            }

            // Bottom Navigation Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pager Indicators
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(4) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            label = "dotWidth"
                        )
                        val color = if (isSelected) brandPrimary else Color.White.copy(alpha = 0.3f)

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                        )
                    }
                }

                // Action Buttons
                Box(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isLastPage = pagerState.currentPage == 3

                    androidx.compose.animation.AnimatedContent(
                        targetState = isLastPage,
                        label = "BottomButtonTransition"
                    ) {
                        lastPage ->
                        if (lastPage) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Login Button
                                Button(
                                    onClick = onLoginClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Login with YouTube",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                TextButton(
                                    onClick = { showSkipDialog = true }
                                ) {
                                    Text(
                                        text = "Continue without login",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // Next Arrow
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brandPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next"
                                    )
                                }
                            }
                        }
                    }
                }
                

            }
        }
    }

    if (showSkipDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            icon = { Icon(Icons.Default.Login, contentDescription = null, tint = brandPrimary) },
            title = { Text("Login Recommended", color = Color.White) },
            text = { 
                Text(
                    "Logging in allows you to sync your library, access personalized recommendations, and manage your playlists. Are you sure you want to continue without logging in?",
                    color = Color.White.copy(alpha = 0.8f)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipDialog = false
                        onLoginClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandPrimary)
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSkipDialog = false
                        onSkipLogin()
                    }
                ) {
                    Text("Continue without login", color = brandPrimary)
                }
            },
            containerColor = Color(0xFF121212),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }
}

@Composable
fun WelcomePageContent(page: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp), // Push up slightly
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> IntroPage()
            1 -> FeaturesPageOne()
            2 -> FeaturesPageTwo()
            3 -> LoginPageContent()
        }
    }
}

@Composable
fun IntroPage() {
    // Large Hero Image / Icon
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }

    Spacer(modifier = Modifier.height(48.dp))

    Text(
        text = "Welcome to\nSuvMusic",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = Color.White,
        lineHeight = 52.sp
    )

    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "Proudly Made in India 🇮🇳",
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFFB388FF),
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Premium Open Source music player. Stream, download, and enjoy high-quality audio with a stunning material design.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun FeaturesPageOne() {
    FeatureHeader(
        icon = Icons.Default.LibraryMusic,
        title = "Music Source"
    )

    Spacer(modifier = Modifier.height(32.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeatureCard(
            icon = Icons.Default.Album,
            title = "YouTube Music",
            desc = "Access the entire YouTube Music library with playlists, mixes, and more."
        )
        FeatureCard(
            icon = Icons.Default.GraphicEq,
            title = "High Quality Audio",
            desc = "Crystal clear playback with support for up to 256kbps."
        )
    }
}

@Composable
fun FeaturesPageTwo() {
    FeatureHeader(
        icon = Icons.Default.MusicNote,
        title = "Premium Features"
    )

    Spacer(modifier = Modifier.height(32.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeatureCard(
            icon = Icons.Default.CloudDownload,
            title = "Offline Playback",
            desc = "Download your favorite tracks and playlists to listen anywhere, anytime."
        )
        FeatureCard(
            icon = Icons.Default.CheckCircle,
            title = "No Ads",
            desc = "Enjoy an uninterrupted listening experience. Completely free, forever."
        )
    }
}

@Composable
fun LoginPageContent() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color(0xFF6200EA).copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Login,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color(0xFFB388FF)
        )
    }

    Spacer(modifier = Modifier.height(40.dp))

    Text(
        text = "Connect Account",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Sign in to sync your library, playlists, and recommendations from YouTube Music.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
fun FeatureHeader(icon: ImageVector, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFB388FF)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun FeatureCard(icon: ImageVector, title: String, desc: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}