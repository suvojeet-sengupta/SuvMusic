package com.suvojeet.suvmusic.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.viewmodel.AboutViewModel

/**
 * Premium Apple Music inspired About Screen with Secure Developer Mode
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
    var showPasswordDialog by remember { mutableStateOf(false) }

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
            
            // App Icon
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
                    painter = painterResource(id = R.drawable.logo),
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
                text = if (isDeveloperMode) "Version 1.0.3 (Dev Unlocked)" else "Version 1.0.3",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
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
            SectionHeader("HIGHLIGHTS")
            SectionContainer {
                FeatureItem(Icons.Outlined.Palette, "Premium Design", "Apple Music inspired UI")
                FeatureItem(Icons.Outlined.Block, "100% Ad-Free", "No interruptions, ever")
                FeatureItem(Icons.Default.CloudDownload, "Offline Downloads", "Save songs for offline listening")
                FeatureItem(Icons.Default.HighQuality, "High Quality Audio", "Up to 320 kbps with HQ Audio")
                FeatureItem(Icons.Default.Bolt, "Blazing Fast", "Optimized for performance", showDivider = false)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Developer Section
            SectionHeader("DEVELOPER")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val uriHandler = LocalUriHandler.current
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFA2D48), Color(0xFFFF6B6B))
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                    IconButton(onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta") }) {
                        Icon(
                            imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.GitHub,
                            contentDescription = "GitHub",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { uriHandler.openUri("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg==") }) {
                        Icon(
                            imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.Instagram,
                            contentDescription = "Instagram",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { uriHandler.openUri("https://t.me/suvojeet_sengupta") }) {
                        Icon(
                            imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                            contentDescription = "Telegram",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Crafted with passion for music lovers who deserve a premium experience without the premium price tag.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Advanced / Developer Mode
            SectionHeader("ADVANCED")
            SectionContainer {
                val icon = if (isDeveloperMode) Icons.Default.LockOpen else Icons.Default.Security
                val title = "Developer Mode"
                val subtitle = if (isDeveloperMode) "Provide access to JioSaavn HQ" else "Tap to unlock extra features"
                val tint = if (isDeveloperMode) Color(0xFF2DFA64) else Color.White
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isDeveloperMode) {
                                showPasswordDialog = true
                            } else {
                                viewModel.disableDeveloperMode()
                                Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    
                    if (isDeveloperMode) {
                        Switch(
                            checked = true,
                            onCheckedChange = { viewModel.disableDeveloperMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2DFA64)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tech Stack
            SectionHeader("BUILT WITH")
            SectionContainer {
                AboutInfoItem("Language", "Kotlin")
                AboutInfoItem("UI Framework", "Jetpack Compose")
                AboutInfoItem("Audio Engine", "Media3 ExoPlayer")
                AboutInfoItem("Data Source", "YouTube + HQ Audio", showDivider = false)
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
    
    // Password Dialog
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Enter Developer Password") },
            text = {
                Column {
                    Text(
                        text = "This feature is for development testing purpose only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            isError = false
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = "Incorrect password",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.tryUnlockDeveloperMode(password)) {
                            showPasswordDialog = false
                            Toast.makeText(context, "JioSaavn HQ Enabled Permanently", Toast.LENGTH_LONG).show()
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = Color.White.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(start = 16.dp, bottom = 10.dp)
    )
}

@Composable
private fun SectionContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E)),
        content = content
    )
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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

