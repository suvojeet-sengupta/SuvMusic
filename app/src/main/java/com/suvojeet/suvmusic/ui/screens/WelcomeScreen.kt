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
    val scope = rememberCoroutineScope()
    
    val currentSource by viewModel.currentSource.collectAsState(initial = com.suvojeet.suvmusic.data.MusicSource.YOUTUBE)
    val sourceSelected by viewModel.sourceSelected.collectAsState()
    
    // Dynamic page count - 4 pages if HQ Audio selected (no login), 5 if YouTube (with login)
    val showLoginPage = currentSource == com.suvojeet.suvmusic.data.MusicSource.YOUTUBE
    val totalPages = if (showLoginPage) 5 else 4
    
    val pagerState = rememberPagerState(pageCount = { totalPages })
    
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
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
                    .fillMaxWidth(),
                userScrollEnabled = !(pagerState.currentPage == 3 && !sourceSelected) // Disable scroll on source page until selected
            ) { page ->
                WelcomePageContent(page = page, viewModel = viewModel, showLoginPage = showLoginPage)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page Indicators
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { iteration ->
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
                val isLastPage = pagerState.currentPage == totalPages - 1
                val isSourcePage = pagerState.currentPage == 3
                
                if (!isLastPage) {
                    // Next button - disabled on source page if not selected
                    val canProceed = !isSourcePage || sourceSelected
                    
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        enabled = canProceed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canProceed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = if (canProceed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                    // Last page action
                    if (showLoginPage) {
                        // YouTube Music selected - show login button
                        Button(
                            onClick = { onLoginClick() },
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
                    } else {
                        // HQ Audio selected - show Get Started button (no login needed)
                        Button(
                            onClick = {
                                viewModel.setOnboardingCompleted()
                                onSkipClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Skip Button - only visible on last page for YouTube Music login
            AnimatedVisibility(
                visible = pagerState.currentPage == totalPages - 1 && showLoginPage,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextButton(
                    onClick = {
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
        }
    }
}


@Composable
fun WelcomePageContent(page: Int, viewModel: WelcomeViewModel, showLoginPage: Boolean) {
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
            4 -> if (showLoginPage) LoginPage() else ReadyPage()
        }
    }
}

@Composable
fun ReadyPage() {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = Color(0xFF4CAF50)
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(
        text = "You're All Set!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "HQ Audio is ready to use. Enjoy high-fidelity music streaming with no login required!",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SourceSelectionPage(viewModel: WelcomeViewModel) {
    val currentSource by viewModel.currentSource.collectAsState(initial = com.suvojeet.suvmusic.data.MusicSource.YOUTUBE)
    val sourceSelected by viewModel.sourceSelected.collectAsState()
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
    
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
            text = "Choose your primary music source to continue.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SourceOptionCard(
            title = "YouTube Music",
            description = "Huge library, official & community tracks",
            selected = sourceSelected && currentSource == com.suvojeet.suvmusic.data.MusicSource.YOUTUBE,
            onClick = { viewModel.setMusicSource(com.suvojeet.suvmusic.data.MusicSource.YOUTUBE) }
        )
        
        // Only show JioSaavn option in developer mode
        if (isDeveloperMode) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SourceOptionCard(
                title = "HQ Audio",
                description = "High Fidelity (320kbps), Bollywood & Regional",
                selected = sourceSelected && currentSource == com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN,
                onClick = { viewModel.setMusicSource(com.suvojeet.suvmusic.data.MusicSource.JIOSAAVN) }
            )
        }
        
        // Hint text when not selected
        if (!sourceSelected) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "↑ Tap to select a source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
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
