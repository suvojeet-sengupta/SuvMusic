package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkShape
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkSize
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
 * Customization settings screen for player appearance
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
    val dynamicColor by sessionManager.dynamicColorFlow.collectAsStateWithLifecycle(initialValue = true)
    val pureBlack by sessionManager.pureBlackEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val appTheme by sessionManager.appThemeFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.data.model.AppTheme.DEFAULT)
    val themeMode by sessionManager.themeModeFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.data.model.ThemeMode.SYSTEM)
    val currentMiniPlayerStyle by sessionManager.miniPlayerStyleFlow.collectAsStateWithLifecycle(initialValue = com.suvojeet.suvmusic.data.model.MiniPlayerStyle.YT_MUSIC)

    val scope = rememberCoroutineScope()
    
    // Style Selection Dialog/Sheet
    var showMiniPlayerStyleSheet by remember { mutableStateOf(false) }
    var showThemeModeSheet by remember { mutableStateOf(false) }
    var showAppThemeSheet by remember { mutableStateOf(false) }
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
                        icon = Icons.Default.RoundedCorner,
                        title = "Artwork Shape",
                        subtitle = currentArtworkShape.name.replace("_", " ").lowercase().capitalize(),
                        onClick = onArtworkShapeClick
                    )

                    HorizontalDivider()
                    
                    CustomizationNavigationItem(
                        icon = Icons.Default.PhotoSizeSelectActual,
                        title = "Artwork Size",
                        subtitle = currentArtworkSize.name.lowercase().capitalize(),
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
                        value = miniPlayerAlpha,
                        onValueChange = { scope.launch { sessionManager.setMiniPlayerAlpha(it) } }
                    )
                    
                    HorizontalDivider()
                    
                    TransparencySliderItem(
                        title = "Navigation Bar Transparency",
                        value = navBarAlpha,
                        onValueChange = { scope.launch { sessionManager.setNavBarAlpha(it) } }
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
                
                com.suvojeet.suvmusic.data.model.MiniPlayerStyle.entries.forEach { style ->
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
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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
private fun TransparencySliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        )
    }
}

private fun formatSeekbarStyleName(style: SeekbarStyle): String {
    return style.name.replace("_", " ").lowercase().split(" ").joinToString(" ") { it.capitalize() }
}
