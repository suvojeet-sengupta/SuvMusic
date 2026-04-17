package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
                        text = "Version $versionName",
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
                            text = "Suvmusic is the indian music streaming app (YT Music Client) that provides best UI unique features that not present anywhere and full control and better implementation.",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Built with a vision to provide a seamless, high-fidelity auditory journey, SuvMusic bridges the gap between massive content availability and a truly personalized user experience.",
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
                    FeatureListItem(icon = Icons.Outlined.Palette, title = "Best-in-Class UI", subtitle = "Unique and fluid Material 3 Experience")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Outlined.Block, title = "100% Ad-Free", subtitle = "Zero interruptions, complete focus")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Default.HighQuality, title = "High Fidelity", subtitle = "Premium audio quality and full control")
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FeatureListItem(icon = Icons.Default.AutoAwesome, title = "AI Equalizer", subtitle = "Neural processing for perfect sound")
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
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(SquircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(primaryColor, colorScheme.tertiary)
                                    )
                                )
                                .padding(3.dp)
                                .background(colorScheme.surface, SquircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            coil3.compose.AsyncImage(
                                model = "https://avatars.githubusercontent.com/u/107928380?v=4",
                                contentDescription = "Suvojeet Sengupta",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(SquircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                error = painterResource(id = R.drawable.logo)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Suvojeet Sengupta",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Vibe Coder",
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Thinking matters. I'm a self-taught Kotlin learner who believes architecture and creativity are my true + points. Delivering what AI can't—human touch in code.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SocialIconBadge(icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.GitHub, onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta") })
                            SocialIconBadge(icon = Icons.Default.Email, onClick = { uriHandler.openUri("mailto:suvojeet@suvojeetsengupta.in") })
                            SocialIconBadge(icon = Icons.Default.Language, onClick = { uriHandler.openUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") })
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
                                Icon(Icons.Default.Language, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = onSurfaceVariant.copy(alpha = 0.5f)) },
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
                                Icon(Icons.Default.Security, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        },
                        trailingContent = { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = onSurfaceVariant.copy(alpha = 0.5f)) },
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
                            .clip(SquircleShape),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
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
