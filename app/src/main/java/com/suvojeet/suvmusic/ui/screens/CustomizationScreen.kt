package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
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
    var showMiniPlayerStyleSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showThemeModeSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAppThemeSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()

    if (showMiniPlayerStyleSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showMiniPlayerStyleSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Mini Player Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                com.suvojeet.suvmusic.data.model.MiniPlayerStyle.entries.forEach { style ->
                    val isSelected = currentMiniPlayerStyle == style
                    ListItem(
                        headlineContent = { 
                            Text(
                                style.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    scope.launch {
                                        sessionManager.setMiniPlayerStyle(style)
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
            }
        }
    }

    if (showThemeModeSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showThemeModeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                com.suvojeet.suvmusic.data.model.ThemeMode.entries.forEach { mode ->
                    val isSelected = themeMode == mode
                    ListItem(
                        headlineContent = { 
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    scope.launch {
                                        sessionManager.setThemeMode(mode)
                                        sheetState.hide()
                                        showThemeModeSheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showAppThemeSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAppThemeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                com.suvojeet.suvmusic.data.model.AppTheme.entries.forEach { theme ->
                    val isSelected = appTheme == theme
                    ListItem(
                        headlineContent = { 
                            Text(
                                theme.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    scope.launch {
                                        sessionManager.setAppTheme(theme)
                                        sheetState.hide()
                                        showAppThemeSheet = false
                                    }
                                },
                                shape = SquircleShape
                            )
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            
            // Section: App Theme
            item {
                SettingsSectionTitle("App Theme")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CustomizationNavigationItem(
                        title = "Theme Mode",
                        subtitle = when(themeMode) {
                            com.suvojeet.suvmusic.data.model.ThemeMode.DARK -> "Dark"
                            com.suvojeet.suvmusic.data.model.ThemeMode.LIGHT -> "Light"
                            com.suvojeet.suvmusic.data.model.ThemeMode.SYSTEM -> "System"
                        },
                        icon = Icons.Default.DarkMode,
                        onClick = { showThemeModeSheet = true }
                    )

                    HorizontalDivider()

                    CustomizationNavigationItem(
                        title = "App Theme",
                        subtitle = appTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Palette,
                        onClick = { showAppThemeSheet = true }
                    )

                    HorizontalDivider()

                    CustomizationSwitchItem(
                        title = "Dynamic Color",
                        subtitle = "Use colors from your wallpaper (Android 12+)",
                        icon = Icons.Default.Palette,
                        checked = dynamicColor,
                        onCheckedChange = { checked ->
                            scope.launch {
                                sessionManager.setDynamicColor(checked)
                            }
                        }
                    )
                    
                    HorizontalDivider()
                    
                    CustomizationSwitchItem(
                        title = "Pure Black",
                        subtitle = "Use pitch black for dark theme (AMOLED)",
                        icon = Icons.Default.InvertColors,
                        checked = pureBlack,
                        onCheckedChange = { checked ->
                            scope.launch {
                                sessionManager.setPureBlackEnabled(checked)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section: Player Design
            item {
                SettingsSectionTitle("Player Design")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CustomizationNavigationItem(
                        title = "Seekbar Style",
                        subtitle = formatSeekbarStyleName(currentSeekbarStyle),
                        icon = Icons.Default.GraphicEq,
                        onClick = onSeekbarStyleClick
                    )
                    
                    HorizontalDivider()
                    
                    CustomizationNavigationItem(
                        title = "Artwork Shape",
                        subtitle = formatArtworkShapeName(currentArtworkShape),
                        icon = getArtworkShapeIcon(currentArtworkShape),
                        onClick = onArtworkShapeClick
                    )
                    
                    HorizontalDivider()

                    CustomizationNavigationItem(
                        title = "Artwork Size",
                        subtitle = currentArtworkSize.label,
                        icon = Icons.Default.Image,
                        customIcon = { ArtworkSizeIndicator(currentArtworkSize) },
                        onClick = onArtworkSizeClick
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Section: Interface Transparency
            item {
                SettingsSectionTitle("Interface Transparency")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Preview Card
                    MiniPlayerPreview(alpha = miniPlayerAlpha, style = currentMiniPlayerStyle)

                    SettingsCard {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mini Player Opacity
                            TransparencySliderItem(
                                title = "Mini Player Opacity",
                                icon = Icons.Default.MusicNote,
                                alpha = miniPlayerAlpha,
                                onAlphaChange = { opacity ->
                                    scope.launch {
                                        sessionManager.setMiniPlayerAlpha(opacity)
                                    }
                                }
                            )

                            HorizontalDivider()

                            // Nav Bar Opacity
                            TransparencySliderItem(
                                title = "Navigation Bar Opacity",
                                icon = Icons.Default.Square,
                                alpha = navBarAlpha,
                                onAlphaChange = { opacity ->
                                    scope.launch {
                                        sessionManager.setNavBarAlpha(opacity)
                                    }
                                }
                            )

                            HorizontalDivider()

                            CustomizationNavigationItem(
                                title = "Mini Player Style",
                                subtitle = currentMiniPlayerStyle.label,
                                icon = Icons.Default.MusicNote, 
                                onClick = { showMiniPlayerStyleSheet = true }
                            )

                            HorizontalDivider()

                            CustomizationSwitchItem(
                                title = "iOS Liquid Glass Navbar",
                                subtitle = "Adds a glass-like blur effect to the navigation bar",
                                icon = Icons.Default.Circle,
                                checked = iosLiquidGlassEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        sessionManager.setIosLiquidGlassEnabled(checked)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Info text
            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = SquircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "You can also change these settings directly from the player by long-pressing on the seekbar or artwork.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
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
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}

@Composable
private fun CustomizationNavigationItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    customIcon: (@Composable () -> Unit)? = null,
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
                if (customIcon != null) {
                    customIcon()
                } else {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
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
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
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
                colors = androidx.compose.material3.SwitchDefaults.colors(
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
private fun MiniPlayerPreview(alpha: Float, style: com.suvojeet.suvmusic.data.model.MiniPlayerStyle) {
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
        val isPill = style == com.suvojeet.suvmusic.data.model.MiniPlayerStyle.FLOATING_PILL
        val isYT = style == com.suvojeet.suvmusic.data.model.MiniPlayerStyle.YT_MUSIC
        
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
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
                color = MaterialTheme.colorScheme.primary
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
private fun MiniPlayerPreview(alpha: Float, style: com.suvojeet.suvmusic.data.model.MiniPlayerStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
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
        val isPill = style == com.suvojeet.suvmusic.data.model.MiniPlayerStyle.FLOATING_PILL
        val isYT = style == com.suvojeet.suvmusic.data.model.MiniPlayerStyle.YT_MUSIC
        
        val shape = when {
            isPill -> RoundedCornerShape(32.dp)
            isYT -> RoundedCornerShape(0.dp)
            else -> RoundedCornerShape(14.dp)
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
                                .clip(if (isPill) CircleShape else RoundedCornerShape(if (isYT) 4.dp else 8.dp))
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
    return when (style) {
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient"
        SeekbarStyle.MATERIAL -> "Material 3"
    }
}

private fun formatArtworkShapeName(shape: ArtworkShape): String {
    return when (shape) {
        ArtworkShape.ROUNDED_SQUARE -> "Rounded Square"
        ArtworkShape.CIRCLE -> "Circle"
        ArtworkShape.VINYL -> "Vinyl"
        ArtworkShape.SQUARE -> "Square"
    }
}

private fun getArtworkShapeIcon(shape: ArtworkShape): ImageVector {
    return when (shape) {
        ArtworkShape.ROUNDED_SQUARE -> Icons.Rounded.RoundedCorner
        ArtworkShape.CIRCLE -> Icons.Default.Circle
        ArtworkShape.VINYL -> Icons.Default.Album
        ArtworkShape.SQUARE -> Icons.Default.Square
    }
}

// Keeping the preview drawing functions as they might be needed for other screens or sub-screens
// (Though they are not used in the main list anymore, keeping them to prevent breaking if other files depend on them or for future use in detail screens)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveformPreview(
    progress: Float,
    amplitudes: List<Float>,
    activeColor: Color,
    inactiveColor: Color
) {
    // ... (Existing implementation kept safe if needed, or can be removed if strictly unused. 
    // Since this file was replacing content, and I am not including the preview cards in the main screen anymore, 
    // I will include them if they were used by the detail screens which might be in this file.
    // Wait, the previous file had them as private functions at the bottom.
    // The previous file defined `SeekbarStylePreviewCard` which used them. 
    // That component was not used in the main screen in the previous code either? 
    // Ah, it seems `SeekbarStyleScreen` might be a separate file, but `CustomizationScreen` had these previews defined.
    // I will keep them to be safe, but I won't use them in the main view for now as requested "organized" view.)
    
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val barWidth = width / amplitudes.size
    val maxBarHeight = height * 0.8f
    val progressX = progress * width
    
    amplitudes.forEachIndexed { index, amplitude ->
        val x = index * barWidth + barWidth / 2
        val isPast = x < progressX
        val barHeight = amplitude * maxBarHeight
        val topY = centerY - barHeight / 2
        
        drawRoundRect(
            color = if (isPast) activeColor else inactiveColor.copy(alpha = 0.4f),
            topLeft = Offset(x - barWidth * 0.3f, topY),
            size = Size(barWidth * 0.6f, barHeight),
            cornerRadius = CornerRadius(barWidth * 0.3f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveLinePreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val progressX = progress * width
    val amplitude = height * 0.3f
    
    val path = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= width) {
            val y = centerY + sin(x * 0.1f) * amplitude
            lineTo(x, y)
            x += 3f
        }
    }
    
    drawPath(path = path, color = inactiveColor, style = Stroke(width = 2.dp.toPx()))
    
    val playedPath = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        while (x <= progressX) {
            val y = centerY + sin(x * 0.1f) * amplitude
            lineTo(x, y)
            x += 3f
        }
    }
    
    drawPath(path = playedPath, color = activeColor, style = Stroke(width = 2.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClassicPreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val trackHeight = 4.dp.toPx()
    
    drawRoundRect(
        color = inactiveColor.copy(alpha = 0.3f),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
    
    drawRoundRect(
        color = activeColor,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(progress * width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDotsPreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val progressX = progress * width
    val dotCount = 15
    val dotSpacing = width / dotCount
    
    for (i in 0 until dotCount) {
        val x = i * dotSpacing + dotSpacing / 2
        val isPast = x < progressX
        drawCircle(
            color = if (isPast) activeColor else inactiveColor.copy(alpha = 0.4f),
            radius = 3.dp.toPx(),
            center = Offset(x, centerY)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGradientPreview(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val trackHeight = 5.dp.toPx()
    
    drawRoundRect(
        color = inactiveColor.copy(alpha = 0.2f),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
    
    drawRoundRect(
        brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientMiddle, GradientEnd)),
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(progress * width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
}
