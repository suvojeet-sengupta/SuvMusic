package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.theme.Cyan40
import com.suvojeet.suvmusic.ui.theme.Purple40
import com.suvojeet.suvmusic.ui.viewmodel.WelcomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onSkipClick: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        // Removed automatic completion logic
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient Background Glower
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(300.dp)
                .padding(top = 50.dp, end = 20.dp)
                .blur(80.dp)
                .background(Purple40.copy(alpha = 0.3f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(300.dp)
                .padding(bottom = 50.dp, start = 20.dp)
                .blur(80.dp)
                .background(Cyan40.copy(alpha = 0.3f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                WelcomePageContent(page = page, viewModel = viewModel)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page Indicators
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { iteration ->
                    val color = if (pagerState.currentPage == iteration) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
            
            // Navigation Actions
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pagerState.currentPage < 4) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            // On Login, we DON'T mark complete yet. Navigation handles it on success.
                            onLoginClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Login with YouTube Music",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Skip Button (Always visible logic or only on last page? 
            // User requested "Next Next" but also "Skip". Let's keep Skip available.)
            AnimatedVisibility(
                visible = pagerState.currentPage == 4,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextButton(
                    onClick = {
                        // Mark as complete when explicitly skipping
                        viewModel.setOnboardingCompleted()
                        onSkipClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for now",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Allow skipping from earlier pages too? Maybe just a small "Skip" at top right or bottom?
            // Let's stick to user request flow: "welcpme screen ko multiple steps mein kr next next kr paye"
            // Usually "Skip" is an alternative to the whole flow. 

        }
    }
}

@Composable
fun WelcomePageContent(page: Int, viewModel: WelcomeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> IntroPage()
            1 -> FeaturesPageOne()
            2 -> FeaturesPageTwo()
            3 -> SourceSelectionPage(viewModel)
            4 -> LoginPage()
        }
    }
}

@Composable
fun SourceSelectionPage(viewModel: WelcomeViewModel) {
    val currentSource by viewModel.currentSource.collectAsState(initial = com.suvojeet.suvmusic.data.MusicSource.YOUTUBE)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Select Default Source",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Choose your primary music source. This customizes your search & home experience.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SourceOptionCard(
            title = "YouTube Music",
            description = "Huge library, official & community tracks",
            selected = currentSource == com.suvojeet.suvmusic.data.MusicSource.YOUTUBE,
            onClick = { viewModel.setMusicSource(com.suvojeet.suvmusic.data.MusicSource.YOUTUBE) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SourceOptionCard(
            title = "HQ Audio",
            description = "High Fidelity (320kbps), Bollywood & Regional",
            selected = currentSource == com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN,
            onClick = { viewModel.setMusicSource(com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN) }
        )
    }
}

@Composable
fun SourceOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IntroPage() {
    Surface(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = com.suvojeet.suvmusic.R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(
        text = "Welcome to SuvMusic",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Developed by Suvojeet Sengupta",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        text = "A premium, native client blending YouTube Music & Cloud Audio in a stunning Material 3 interface.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun FeaturesPageOne() {
    Image(
        painter = painterResource(id = com.suvojeet.suvmusic.R.drawable.logo),
        contentDescription = null,
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
    )
    
    Spacer(modifier = Modifier.height(40.dp))
    
    FeatureItemLarge(
        icon = Icons.Default.Album,
        title = "Dual Music Sources",
        desc = "The vast library of YouTube Music combined with HQ Audio's catalog."
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    FeatureItemLarge(
        icon = Icons.Default.GraphicEq,
        title = "Hi-Fi Audio",
        desc = "Stream in crystal clear 320kbps MP3 (HQ Audio) or high-quality AAC."
    )
}

@Composable
fun FeaturesPageTwo() {
    Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = null,
        modifier = Modifier.size(60.dp),
        tint = MaterialTheme.colorScheme.secondary
    )
    
    Spacer(modifier = Modifier.height(40.dp))
    
    FeatureItemLarge(
        icon = Icons.Default.MusicNote,
        title = "Premium Experience",
        desc = "Synchronized lyrics, detailed song credits, and adaptive themes."
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    FeatureItemLarge(
        icon = Icons.Default.CloudDownload,
        title = "Offline & Ad-Free",
        desc = "Download your favorites for offline listening. No ads, just music."
    )
}

@Composable
fun LoginPage() {
    Icon(
        imageVector = Icons.Default.Login,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.tertiary
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(
        text = "Get Started",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Sign in to access your YouTube Music library and playlists. HQ Audio works out of the box!",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun FeatureItemLarge(icon: ImageVector, title: String, desc: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
