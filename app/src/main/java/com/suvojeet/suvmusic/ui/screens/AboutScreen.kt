package com.suvojeet.suvmusic.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.ui.components.M3ENavigationItem
import com.suvojeet.suvmusic.core.ui.components.M3EPageHeader
import com.suvojeet.suvmusic.core.ui.components.M3ESettingsGroupHeader
import com.suvojeet.suvmusic.core.ui.components.M3ESettingsItem
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.viewmodel.AboutViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onHowItWorksClick: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDeveloperMode by viewModel.isDeveloperMode.collectAsState(initial = false)
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showPasswordDialog by remember { mutableStateOf(false) }

    val dominantColors = DominantColors(
        primary = MaterialTheme.colorScheme.primaryContainer,
        secondary = MaterialTheme.colorScheme.tertiaryContainer,
        accent = MaterialTheme.colorScheme.primary,
        onBackground = MaterialTheme.colorScheme.onSurface
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "About SuvMusic",
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            MeshGradientBackground(dominantColors = dominantColors)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 80.dp
                )
            ) {
                // Hero Block
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                            label = "logo_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 12.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "App Logo",
                                    modifier = Modifier.padding(20.dp).fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "SuvMusic",
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Unlimited Music. Ad-Free. Pure Bliss.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        SuggestionChip(
                            onClick = { 
                                Toast.makeText(context, "You are on the latest version", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("v${com.suvojeet.suvmusic.BuildConfig.VERSION_NAME}") },
                            icon = { Icon(Icons.Default.NewReleases, null, modifier = Modifier.size(18.dp)) },
                            shape = MaterialTheme.shapes.medium,
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "SuvMusic is a cutting-edge, open-source music player developed in India. Our focus is to deliver a premium, interruption-free music experience by leveraging the vast library of YouTube Music.",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // What's Inside - Feature Cards
                item {
                    M3ESettingsGroupHeader("FEATURES")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2
                    ) {
                        val features = listOf(
                            Triple(Icons.Outlined.Palette, "Premium Design", "Modern, clean interface"),
                            Triple(Icons.Outlined.Block, "100% Ad-Free", "No interruptions, ever"),
                            Triple(Icons.Default.CloudDownload, "Offline Play", "Download & listen anywhere"),
                            Triple(Icons.Default.HighQuality, "High Quality", "Up to 256 kbps audio"),
                            Triple(Icons.Default.Language, "Spatial Audio", "Audio AR support"),
                            Triple(Icons.Default.History, "Synced Lyrics", "Multiple providers")
                        )
                        features.forEach { (icon, title, desc) ->
                            M3EFeatureCard(
                                icon = icon,
                                title = title,
                                desc = desc,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    M3ESettingsGroupHeader("WHY OPUS AUDIO?")
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "SuvMusic streams audio using the Opus codec, widely regarded as a superior modern format.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            QualityPointM3E("Better than 320kbps MP3", "Opus 160kbps offers quality indistinguishable from or better than 320kbps MP3 while saving data.")
                            QualityPointM3E("Superior to AAC", "Opus supports a wider frequency range, delivering deeper bass and crisper highs.")
                        }
                    }
                }

                // LINKS Section
                item {
                    M3ESettingsGroupHeader("PROJECT")
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        M3ENavigationItem(
                            icon = Icons.Default.Code,
                            title = "Source Code",
                            subtitle = "github.com/suvojeet-sengupta/SuvMusic",
                            onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta/SuvMusic") }
                        )
                        M3ENavigationItem(
                            icon = Icons.Default.BugReport,
                            title = "Report a Bug",
                            subtitle = "Help us improve",
                            onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta/SuvMusic/issues") }
                        )
                        M3ENavigationItem(
                            icon = Icons.Default.Lightbulb,
                            title = "How It Works",
                            onClick = onHowItWorksClick
                        )
                    }
                }

                // TEAM Section
                item {
                    M3ESettingsGroupHeader("DEVELOPMENT TEAM")
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        M3EDeveloperCard(
                            name = "Suvojeet Sengupta",
                            role = "Lead Developer",
                            github = "suvojeet-sengupta",
                            onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta") }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialButtonM3E(Icons.Default.Group, "Instagram", Modifier.weight(1f)) {
                            uriHandler.openUri("https://www.instagram.com/suvojeet__sengupta")
                        }
                        SocialButtonM3E(Icons.Default.Send, "Telegram", Modifier.weight(1f)) {
                            uriHandler.openUri("https://t.me/suvojeet_sengupta")
                        }
                    }

                    M3ENavigationItem(
                        icon = Icons.Default.People,
                        title = "All Contributors",
                        subtitle = "Amazing people who helped",
                        onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta/SuvMusic/graphs/contributors") }
                    )
                }

                item {
                    M3ESettingsGroupHeader("BUILT WITH")
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            TechRowM3E("Language", "Kotlin")
                            TechRowM3E("UI Framework", "Jetpack Compose")
                            TechRowM3E("Audio Engine", "Media3 ExoPlayer")
                            TechRowM3E("Data Source", "YouTube Music")
                        }
                    }
                }

                // ADVANCED Section
                item {
                    M3ESettingsGroupHeader("ADVANCED")
                    M3ESettingsItem(
                        icon = if (isDeveloperMode) Icons.Default.LockOpen else Icons.Default.Security,
                        title = "Developer Mode",
                        subtitle = if (isDeveloperMode) "HQ Audio & Extra options enabled" else "Tap to unlock experimental features",
                        iconTint = if (isDeveloperMode) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        onClick = {
                            if (!isDeveloperMode) {
                                showPasswordDialog = true
                            } else {
                                viewModel.disableDeveloperMode()
                                Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        trailingContent = {
                            if (isDeveloperMode) {
                                Switch(checked = true, onCheckedChange = { viewModel.disableDeveloperMode() })
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = "Made with ❤️ in India",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "© 2026 SuvMusic. All rights reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showPasswordDialog) {
        com.suvojeet.suvmusic.ui.components.DeveloperAccessDialog(
            onDismiss = { showPasswordDialog = false },
            onUnlock = { password ->
                if (viewModel.tryUnlockDeveloperMode(password)) {
                    showPasswordDialog = false
                    Toast.makeText(context, "Developer Mode Enabled", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QualityPointM3E(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 14.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TechRowM3E(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = { Text(value, style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SocialButtonM3E(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "social_scale"
    )
    
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EFeatureCard(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "feature_card_scale"
    )

    ElevatedCard(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource, indication = null) { },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(tint.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmallEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EDeveloperCard(
    name: String,
    role: String,
    github: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "dev_card_scale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource, indication = null) { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = "https://avatars.githubusercontent.com/u/$github",
                contentDescription = name,
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
