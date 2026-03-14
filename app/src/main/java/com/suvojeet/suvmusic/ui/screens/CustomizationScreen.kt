package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkShape
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkSize
import com.suvojeet.suvmusic.data.model.MiniPlayerStyle
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

/**
 * Customization settings screen for player appearance with Material 3 Expressive design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSeekbarStyleClick: () -> Unit = {},
    onArtworkShapeClick: () -> Unit = {},
    onArtworkSizeClick: () -> Unit = {},
    showStyleSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val seekbarStyleString by sessionManager.seekbarStyleFlow.collectAsStateWithLifecycle(initialValue = "WAVE_LINE")
    val artworkShapeString by sessionManager.artworkShapeFlow.collectAsStateWithLifecycle(initialValue = "ROUNDED_SQUARE")
    
    val currentSeekbarStyle = try {
        SeekbarStyle.valueOf(seekbarStyleString)
    } catch (e: Exception) {
        SeekbarStyle.WAVE_LINE
    }
    
    val currentArtworkShape = try {
        ArtworkShape.valueOf(artworkShapeString)
    } catch (e: Exception) {
        ArtworkShape.ROUNDED_SQUARE
    }
    
    val artworkSizeString by sessionManager.artworkSizeFlow.collectAsStateWithLifecycle(initialValue = "LARGE")
    val currentArtworkSize = try {
        ArtworkSize.valueOf(artworkSizeString)
    } catch (e: Exception) {
        ArtworkSize.LARGE
    }

    val miniPlayerAlpha by sessionManager.miniPlayerAlphaFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val navBarAlpha by sessionManager.navBarAlphaFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val iosLiquidGlassEnabled by sessionManager.iosLiquidGlassEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val currentMiniPlayerStyle by sessionManager.miniPlayerStyleFlow.collectAsStateWithLifecycle(initialValue = MiniPlayerStyle.YT_MUSIC)

    val scope = rememberCoroutineScope()
    
    // Style Selection Dialog/Sheet
    var showMiniPlayerStyleSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Customization", fontWeight = FontWeight.Bold) },
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
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
        ) {
            // --- Preview Section ---
            item {
                SettingsSectionTitle("Preview")
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MiniPlayerPreview(alpha = miniPlayerAlpha, style = currentMiniPlayerStyle)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Player Appearance Section ---
            item {
                SettingsSectionTitle("Player Styles")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CustomizationNavigationItem(
                        icon = Icons.Default.Tune,
                        title = "Seekbar Style",
                        subtitle = formatSeekbarStyleName(currentSeekbarStyle),
                        onClick = onSeekbarStyleClick
                    )
                    
                    HorizontalDivider()
                    
                    CustomizationNavigationItem(
                        icon = Icons.Rounded.RoundedCorner,
                        title = "Artwork Shape",
                        subtitle = currentArtworkShape.name.replace("_", " ").lowercase().capitalize(),
                        onClick = onArtworkShapeClick
                    )

                    HorizontalDivider()
                    
                    CustomizationNavigationItem(
                        icon = Icons.Default.PhotoSizeSelectActual,
                        title = "Artwork Size",
                        subtitle = currentArtworkSize.name.lowercase().capitalize(),
                        trailingContent = { ArtworkSizeIndicator(currentArtworkSize) },
                        onClick = onArtworkSizeClick
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- UI Elements Section ---
            item {
                SettingsSectionTitle("UI Components")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CustomizationNavigationItem(
                        icon = Icons.Default.SmartButton,
                        title = "Mini Player Style",
                        subtitle = currentMiniPlayerStyle.label,
                        onClick = { showMiniPlayerStyleSheet = true }
                    )
                    
                    HorizontalDivider()

                    CustomizationSwitchItem(
                        icon = Icons.Default.BlurOn,
                        title = "iOS Liquid Glass",
                        subtitle = "Apply liquid blur effect to player",
                        checked = iosLiquidGlassEnabled,
                        onCheckedChange = { scope.launch { sessionManager.setIosLiquidGlassEnabled(it) } }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Transparency Section ---
            item {
                SettingsSectionTitle("Transparency & Blurs")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TransparencySliderItem(
                        title = "Mini Player Transparency",
                        icon = Icons.Default.Opacity,
                        alpha = miniPlayerAlpha,
                        onAlphaChange = { scope.launch { sessionManager.setMiniPlayerAlpha(it) } }
                    )
                    
                    HorizontalDivider()
                    
                    TransparencySliderItem(
                        title = "Navigation Bar Transparency",
                        icon = Icons.Default.Layers,
                        alpha = navBarAlpha,
                        onAlphaChange = { scope.launch { sessionManager.setNavBarAlpha(it) } }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Mini Player Style Bottom Sheet
    if (showMiniPlayerStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMiniPlayerStyleSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Mini Player Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                MiniPlayerStyle.entries.forEach { style ->
                    ListItem(
                        headlineContent = { Text(style.label) },
                        leadingContent = {
                            RadioButton(
                                selected = currentMiniPlayerStyle == style,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    scope.launch { sessionManager.setMiniPlayerStyle(style) }
                                    scope.launch {
                                        sheetState.hide()
                                        showMiniPlayerStyleSheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
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
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
private fun HorizontalDivider() {
    M3HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}

@Composable
private fun CustomizationNavigationItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingContent?.invoke()
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        },
        modifier = Modifier
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun CustomizationSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = Modifier
            .dpadFocusable(onClick = { onCheckedChange(!checked) }, shape = SquircleShape)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ArtworkSizeIndicator(size: ArtworkSize) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val boxCount = when (size) {
            ArtworkSize.SMALL -> 1
            ArtworkSize.MEDIUM -> 2
            ArtworkSize.LARGE -> 3
        }
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(if (index < boxCount) 10.dp else 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < boxCount) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun TransparencySliderItem(
    title: String,
    icon: ImageVector,
    alpha: Float,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${(alpha * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0f..1f,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun MiniPlayerPreview(alpha: Float, style: MiniPlayerStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Mock background content to show transparency
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                )
            }
        }

        // Mini Player Mockup
        val isPill = style == MiniPlayerStyle.FLOATING_PILL
        val isYT = style == MiniPlayerStyle.YT_MUSIC
        
        val shape = when {
            isPill -> CircleShape
            isYT -> RoundedCornerShape(0.dp)
            else -> SquircleShape
        }
        val horizontalPadding = when {
            isPill -> 24.dp
            isYT -> 12.dp
            else -> 12.dp
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isPill) 0.85f else if (isYT) 1f else 0.95f)
                .height(if (isPill) 56.dp else 64.dp)
                .clip(shape),
            color = Color.Transparent,
            shape = shape
        ) {
            val effectiveAlpha = alpha
            Box(
                modifier = Modifier
                    .then(
                        if (isYT) {
                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = effectiveAlpha))
                        } else {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = effectiveAlpha),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = effectiveAlpha)
                                    )
                                )
                            )
                        }
                    )
                    .padding(horizontal = horizontalPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isPill) 38.dp else if (isYT) 48.dp else 42.dp)
                                .clip(if (isPill) CircleShape else if (isYT) RoundedCornerShape(4.dp) else SquircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.width(100.dp).height(8.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(60.dp).height(6.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(3.dp)))
                        }
                        
                        // Mini controls icons mockup
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            )
                        }
                    }
                    
                    if (isYT || !isPill) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSeekbarStyleName(style: SeekbarStyle): String {
    return style.name.replace("_", " ").lowercase().split(" ").joinToString(" ") { it.capitalize() }
}
