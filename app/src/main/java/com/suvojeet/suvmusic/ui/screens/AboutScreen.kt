package com.suvojeet.suvmusic.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
 * Premium Polished About Screen with Dynamic Colors
 * Clean, organized, and professional design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onHowItWorksClick: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    // Dynamic colors
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val surfaceColor = colorScheme.surface
    val surfaceContainerColor = colorScheme.surfaceContainer
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = surfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // === HERO SECTION ===
            // App Logo with subtle glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "SuvMusic Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // App Name
            Text(
                text = "SuvMusic",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = onSurfaceColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Version
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = primaryColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (isDeveloperMode) "v1.0.3 • Dev Mode" else "Version 1.0.3",
                    style = MaterialTheme.typography.labelMedium,
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Tagline
            Text(
                text = "Premium music streaming with beautiful design.\nAd-free. High quality. Always.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // === FEATURES SECTION ===
            SectionTitle("Features", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
                    FeatureRow(
                        icon = Icons.Outlined.Palette,
                        title = "Premium Design",
                        subtitle = "Modern, clean interface",
                        accentColor = primaryColor
                    )
                    FeatureRow(
                        icon = Icons.Outlined.Block,
                        title = "100% Ad-Free",
                        subtitle = "No interruptions, ever",
                        accentColor = primaryColor
                    )
                    FeatureRow(
                        icon = Icons.Default.CloudDownload,
                        title = "Offline Mode",
                        subtitle = "Download for offline listening",
                        accentColor = primaryColor
                    )
                    FeatureRow(
                        icon = Icons.Default.HighQuality,
                        title = "High Quality",
                        subtitle = "Up to 320 kbps audio",
                        accentColor = primaryColor
                    )
                    FeatureRow(
                        icon = Icons.Default.Bolt,
                        title = "Fast & Smooth",
                        subtitle = "Optimized performance",
                        accentColor = primaryColor,
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // === DEVELOPER SECTION ===
            SectionTitle("Developer", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val uriHandler = LocalUriHandler.current
                    
                    // Developer Photo
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = "https://avatars.githubusercontent.com/u/suvojeet-sengupta",
                            contentDescription = "Suvojeet Sengupta",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            error = coil.compose.rememberAsyncImagePainter(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(R.drawable.logo)
                                    .build()
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Suvojeet Sengupta",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = onSurfaceColor
                    )
                    
                    Text(
                        text = "Android Developer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Social Links
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialButton(
                            icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.GitHub,
                            label = "GitHub",
                            color = primaryColor,
                            onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta") }
                        )
                        SocialButton(
                            icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Instagram,
                            label = "Instagram",
                            color = primaryColor,
                            onClick = { uriHandler.openUri("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg==") }
                        )
                        SocialButton(
                            icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                            label = "Telegram",
                            color = primaryColor,
                            onClick = { uriHandler.openUri("https://t.me/suvojeet_sengupta") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Crafted with passion for music lovers who deserve a premium experience.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // === TECH STACK SECTION ===
            SectionTitle("Built With", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
                    InfoRow("Language", "Kotlin")
                    InfoRow("UI Framework", "Jetpack Compose")
                    InfoRow("Audio Engine", "Media3 ExoPlayer")
                    InfoRow("Data Source", "YouTube + HQ Audio", showDivider = false)
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            // === SOURCE SECTION ===
            SectionTitle("Source", primaryColor)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                val uriHandler = LocalUriHandler.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/suvojeet-sengupta/SuvMusic") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.GitHub,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "GitHub Repository",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = onSurfaceColor
                        )
                        Text(
                            text = "View source code & report issues",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            // === HOW IT WORKS SECTION ===
            SectionTitle("Learn More", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHowItWorksClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "How It Works",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = onSurfaceColor
                        )
                        Text(
                            text = "Learn how SuvMusic works with YouTube Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            // === ADVANCED SECTION ===
            SectionTitle("Advanced", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                DeveloperModeRow(
                    isDeveloperMode = isDeveloperMode,
                    primaryColor = primaryColor,
                    onSurfaceColor = onSurfaceColor,
                    onSurfaceVariant = onSurfaceVariant,
                    onClick = {
                        if (!isDeveloperMode) {
                            showPasswordDialog = true
                        } else {
                            viewModel.disableDeveloperMode()
                            Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // === FOOTER ===
            Text(
                text = "Made with ❤️ in India",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "© 2026 Suvojeet Sengupta",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant.copy(alpha = 0.4f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Password Dialog
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { 
                Text(
                    "Developer Access",
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = {
                Column {
                    Text(
                        text = "Enter password to unlock developer features.",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (isError) {
                        Text(
                            text = "Incorrect password",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        if (viewModel.tryUnlockDeveloperMode(password)) {
                            showPasswordDialog = false
                            Toast.makeText(context, "Developer Mode Enabled", Toast.LENGTH_SHORT).show()
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
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// === COMPOSABLE COMPONENTS ===

@Composable
private fun SectionTitle(title: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SocialButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    Surface(
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun DeveloperModeRow(
    isDeveloperMode: Boolean,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariant: Color,
    onClick: () -> Unit
) {
    val icon = if (isDeveloperMode) Icons.Default.LockOpen else Icons.Default.Security
    val iconColor by animateColorAsState(
        targetValue = if (isDeveloperMode) Color(0xFF4CAF50) else primaryColor,
        label = "iconColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Developer Mode",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = onSurfaceColor
            )
            Text(
                text = if (isDeveloperMode) "HQ Audio enabled" else "Tap to unlock",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant
            )
        }
        
        if (isDeveloperMode) {
            Switch(
                checked = true,
                onCheckedChange = { onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50)
                )
            )
        }
    }
}
