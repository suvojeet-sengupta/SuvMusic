package com.suvojeet.suvmusic.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

                // What's Inside - Feature Cards
                item {
                    M3ESettingsGroupHeader("WHAT'S INSIDE")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2
                    ) {
                        val features = listOf(
                            Triple(Icons.Default.MusicNote, "YouTube Music", "Millions of songs"),
                            Triple(Icons.Default.OfflinePin, "Offline Play", "Download & listen"),
                            Triple(Icons.Default.Lyrics, "Synced Lyrics", "Multiple providers"),
                            Triple(Icons.Default.GraphicEq, "Equalizer", "10-band EQ"),
                            Triple(Icons.Default.Group, "Listen Together", "Real-time sync"),
                            Triple(Icons.Default.AutoAwesome, "AI Queue", "Smart recommendations")
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
                        M3ENavigationItem(
                            icon = Icons.Default.Favorite,
                            title = "Support the Project",
                            subtitle = "Donate to keep us alive",
                            onClick = { /* Support link */ }
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
                    M3ENavigationItem(
                        icon = Icons.Default.People,
                        title = "All Contributors",
                        subtitle = "Amazing people who helped",
                        onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta/SuvMusic/graphs/contributors") }
                    )
                }

                // LEGAL Section
                item {
                    M3ESettingsGroupHeader("LEGAL")
                    M3ENavigationItem(
                        icon = Icons.Default.Gavel,
                        title = "Open Source Licenses",
                        onClick = { /* Show licenses */ }
                    )
                    M3ENavigationItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta/SuvMusic/blob/main/PRIVACY.md") }
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
