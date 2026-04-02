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
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.viewmodel.AboutViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

/**
 * About Screen with Material 3 Expressive design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onHowItWorksClick: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("About SuvMusic", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(
                                onClick = onBack,
                                shape = CircleShape,
                            )
                            .padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // === HERO SECTION ===
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
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
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "SuvMusic Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(SquircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "SuvMusic",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = SquircleShape,
                    color = primaryColor.copy(alpha = 0.1f)
                ) {
                    val versionName = com.suvojeet.suvmusic.BuildConfig.VERSION_NAME
                    Text(
                        text = if (isDeveloperMode) "v$versionName • Dev Mode" else "Version $versionName",
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Proudly Made in India 🇮🇳\nUnlimited Music. Ad-Free. Pure Bliss.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // === APP DESCRIPTION SECTION ===
            item {
                SettingsSectionTitle("About")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SuvMusic is a cutting-edge, open-source music player developed in India. Our focus is to deliver a premium, interruption-free music experience by leveraging the vast library of YouTube Music.",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "With support for high-fidelity streaming and advanced audio normalization, SuvMusic ensures a superior auditory journey tailored for audiophiles.",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // === FEATURES SECTION ===
            item {
                SettingsSectionTitle("Features")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FeatureListItem(icon = Icons.Outlined.Palette, title = "Premium Design", subtitle = "Modern, clean M3E interface")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Outlined.Block, title = "100% Ad-Free", subtitle = "No interruptions, ever")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Default.CloudDownload, title = "Offline Mode", subtitle = "Download for offline listening")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Default.HighQuality, title = "High Quality", subtitle = "Up to 256 kbps audio")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Default.Language, title = "Spatial Audio", subtitle = "Audio AR support for headphones")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // === DEVELOPER SECTION ===
            item {
                SettingsSectionTitle("Developer")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val uriHandler = LocalUriHandler.current
                        
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(SquircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            coil3.compose.AsyncImage(
                                model = "https://avatars.githubusercontent.com/u/107928380?v=4",
                                contentDescription = "Suvojeet Sengupta",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(SquircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                error = painterResource(id = R.drawable.logo)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Suvojeet Sengupta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Android Developer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SocialIconBadge(icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.GitHub, onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta") })
                            SocialIconBadge(icon = androidx.compose.material.icons.Icons.Filled.Language, onClick = { uriHandler.openUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") })
                            SocialIconBadge(icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Instagram, onClick = { uriHandler.openUri("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg==") })
                            SocialIconBadge(icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram, onClick = { uriHandler.openUri("https://t.me/suvojeet_sengupta") })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // === TECH STACK SECTION ===
            item {
                SettingsSectionTitle("Built With")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TechListItem("Language", "Kotlin")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TechListItem("UI Framework", "Jetpack Compose")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TechListItem("Audio Engine", "Media3 ExoPlayer")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // === WEBSITE SECTION ===
            item {
                SettingsSectionTitle("Information")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ListItem(
                        headlineContent = { Text("Official Website", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Visit the official SuvMusic website") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(SquircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Language, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = { Icon(androidx.compose.material.icons.Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .dpadFocusable(onClick = { uriHandler.openUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") }, shape = SquircleShape)
                            .clip(SquircleShape),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    ListItem(
                        headlineContent = { Text("Privacy Policy", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Review how SuvMusic handles your data") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(SquircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Security, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = { Icon(androidx.compose.material.icons.Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .dpadFocusable(onClick = { uriHandler.openUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/suvmusic-privacy.html") }, shape = SquircleShape)
                            .clip(SquircleShape),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    ListItem(
                        headlineContent = { Text("How It Works", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Learn how SuvMusic works with YouTube Music") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(SquircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lightbulb, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp), tint = onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .dpadFocusable(onClick = onHowItWorksClick, shape = SquircleShape)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
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
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FeatureListItem(icon: ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun TechListItem(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SocialIconBadge(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun DeveloperModeListItem(isDeveloperMode: Boolean, primaryColor: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("Developer Mode", fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(if (isDeveloperMode) "HQ Audio enabled" else "Tap to unlock extra features") },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(if (isDeveloperMode) Color(0xFF4CAF50).copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isDeveloperMode) Icons.Default.LockOpen else Icons.Default.Security, null, tint = if (isDeveloperMode) Color(0xFF4CAF50) else primaryColor, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            if (isDeveloperMode) {
                Switch(checked = true, onCheckedChange = { onClick() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4CAF50)))
            }
        },
        modifier = Modifier.clickable(onClick = onClick).clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
