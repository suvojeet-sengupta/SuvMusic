package com.suvojeet.suvmusic.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.viewmodel.AboutViewModel

/**
 * Premium Apple Music inspired About Screen
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var tapCount by remember { mutableIntStateOf(0) }
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // App Icon with gradient glow effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFA2D48).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Image(
                    painter = painterResource(id = com.suvojeet.suvmusic.R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // App Name
            Text(
                text = "SuvMusic",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = if (isDeveloperMode) "Version 1.0.2 🔓" else "Version 1.0.2",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    tapCount++
                    when {
                        // Enable developer mode after 7 taps
                        tapCount == 7 && !isDeveloperMode -> {
                            viewModel.enableDeveloperMode()
                            Toast.makeText(context, "🔓 Developer mode enabled", Toast.LENGTH_SHORT).show()
                            tapCount = 0
                        }
                        // Disable developer mode after 7 more taps
                        tapCount == 7 && isDeveloperMode -> {
                            viewModel.disableDeveloperMode()
                            Toast.makeText(context, "🔒 Developer mode disabled", Toast.LENGTH_SHORT).show()
                            tapCount = 0
                        }
                        // Show progress hints
                        tapCount >= 4 && !isDeveloperMode -> {
                            Toast.makeText(context, "${7 - tapCount} more...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Description
            Text(
                text = "A premium music client with Apple Music inspired design. Stream from YouTube Music or HQ Audio (320 kbps). Beautiful, fast, and ad-free.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features Section
            Column(
                modifier = Modifier
                     .fillMaxWidth()
                     .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "HIGHLIGHTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    FeatureItem(
                        icon = Icons.Outlined.Palette,
                        title = "Premium Design",
                        subtitle = "Apple Music inspired UI",
                        showDivider = true
                    )
                    FeatureItem(
                        icon = Icons.Outlined.Block,
                        title = "100% Ad-Free",
                        subtitle = "No interruptions, ever",
                        showDivider = true
                    )
                    FeatureItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Offline Downloads",
                        subtitle = "Save songs for offline listening",
                        showDivider = true
                    )
                    FeatureItem(
                        icon = Icons.Default.HighQuality,
                        title = "High Quality Audio",
                        subtitle = "Up to 320 kbps with HQ Audio",
                        showDivider = true
                    )
                    FeatureItem(
                        icon = Icons.Default.Bolt,
                        title = "Blazing Fast",
                        subtitle = "Optimized for performance",
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Developer Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "DEVELOPER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFA2D48),
                                        Color(0xFFFF6B6B)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = "https://photos.fife.usercontent.google.com/pw/AP1GczMBNMmsCeKjdZ3Tr0-H6j-c62sKTKRtnWBHPcMMZoeWFmYmFsQQa2TD_A=w1289-h859-s-no-gm?authuser=0",
                            contentDescription = "Suvojeet Sengupta",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Suvojeet Sengupta",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Android Developer",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Social Links
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // GitHub
                        IconButton(onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta") }) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Instagram
                        IconButton(onClick = { uriHandler.openUri("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg==") }) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Instagram",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Telegram
                        IconButton(onClick = { uriHandler.openUri("https://t.me/suvojeet_sengupta") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Telegram",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Crafted with passion for music lovers who deserve a premium experience without the premium price tag.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tech Stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "BUILT WITH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    AboutInfoItem(
                        label = "Language",
                        value = "Kotlin",
                        showDivider = true
                    )
                    AboutInfoItem(
                        label = "UI Framework",
                        value = "Jetpack Compose",
                        showDivider = true
                    )
                    AboutInfoItem(
                        label = "Audio Engine",
                        value = "Media3 ExoPlayer",
                        showDivider = true
                    )
                    AboutInfoItem(
                        label = "Data Source",
                        value = "YouTube + HQ Audio",
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Footer
            Text(
                text = "Made with ❤️ in India",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "© 2026 Suvojeet Sengupta",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFA2D48).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFA2D48),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 66.dp),
                color = Color.White.copy(alpha = 0.1f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun AboutInfoItem(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = Color.White.copy(alpha = 0.1f),
                thickness = 0.5.dp
            )
        }
    }
}

