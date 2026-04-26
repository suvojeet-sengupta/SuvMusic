package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType
import com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.data.model.PlayerStyle
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import kotlinx.coroutines.launch

val PlayerStyle.label: String
    get() = when (this) {
        PlayerStyle.YT_MUSIC -> "YT Music (New)"
        PlayerStyle.CLASSIC -> "Classic (SuvMusic)"
        PlayerStyle.LIQUID_GLASS -> "Liquid Glass (iOS)"
    }

/**
 * Appearance settings screen with theme mode and dynamic color options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeModeSheet by remember { mutableStateOf(false) }
    val themeModeSheetState = rememberModalBottomSheetState()
    
    var showAppThemeSheet by remember { mutableStateOf(false) }
    val appThemeSheetState = rememberModalBottomSheetState()

    var showPlayerStyleSheet by remember { mutableStateOf(false) }
    val playerStyleSheetState = rememberModalBottomSheetState()
    
    var showLyricsPositionSheet by remember { mutableStateOf(false) }
    val lyricsPositionSheetState = rememberModalBottomSheetState()
    
    var showLyricsAnimationSheet by remember { mutableStateOf(false) }
    val lyricsAnimationSheetState = rememberModalBottomSheetState()
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
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
            // --- Theme Section ---
            item {
                SettingsSectionTitle("Theme Mode")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppearanceNavigationItem(
                        icon = Icons.Default.DarkMode,
                        title = "Theme Mode",
                        subtitle = uiState.themeMode.label,
                        onClick = { showThemeModeSheet = true }
                    )

                    // Pure Black Mode (Only visible in Dark Mode or System if currently dark)
                    val isDark = when(uiState.themeMode) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }

                    AnimatedVisibility(
                        visible = isDark,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider()
                            AppearanceSwitchItem(
                                icon = Icons.Default.ColorLens,
                                title = "Pure Black",
                                subtitle = "True black background (#000000)",
                                checked = uiState.pureBlackEnabled,
                                onCheckedChange = { viewModel.setPureBlackEnabled(it) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Colors Section ---
            item {
                SettingsSectionTitle("System & Colors")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppearanceSwitchItem(
                        icon = Icons.Default.Palette,
                        title = "Dynamic Colors",
                        subtitle = "Use Android 12+ wallpaper colors",
                        checked = uiState.dynamicColorEnabled,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )

                    AnimatedVisibility(
                        visible = !uiState.dynamicColorEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider()
                            AppearanceNavigationItem(
                                icon = Icons.Default.ColorLens,
                                title = "App Theme",
                                subtitle = uiState.appTheme.label,
                                onClick = { showAppThemeSheet = true }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Player Section ---
            item {
                SettingsSectionTitle("Player")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppearanceSwitchItem(
                        icon = Icons.Default.Animation,
                        title = "Dynamic Background",
                        subtitle = "Animate player background colors",
                        checked = uiState.playerAnimatedBackgroundEnabled,
                        onCheckedChange = { viewModel.setPlayerAnimatedBackgroundEnabled(it) }
                    )
                    
                    HorizontalDivider()
                    
                    AppearanceSwitchItem(
                        icon = Icons.Default.Palette,
                        title = "Album Art Dynamic Colors",
                        subtitle = "Use album art colors for player UI",
                        checked = uiState.albumArtDynamicColorsEnabled,
                        onCheckedChange = { viewModel.setAlbumArtDynamicColorsEnabled(it) }
                    )

                    HorizontalDivider()

                    AppearanceSwitchItem(
                        icon = Icons.Default.Album,
                        title = "Rotating Vinyl Animation",
                        subtitle = "Rotate artwork in vinyl mode",
                        checked = uiState.rotatingVinylAnimationEnabled,
                        onCheckedChange = { viewModel.setRotatingVinylAnimationEnabled(it) }
                    )

                    HorizontalDivider()

                    AppearanceNavigationItem(
                        icon = Icons.Default.Palette,
                        title = "Player Style",
                        subtitle = uiState.playerStyle.label,
                        onClick = { showPlayerStyleSheet = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Performance Section ---
            item {
                SettingsSectionTitle("Performance")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppearanceSwitchItem(
                        icon = Icons.Default.Animation,
                        title = "Force Max Refresh Rate",
                        subtitle = "Enable 90Hz/120Hz for smoother UI",
                        checked = uiState.forceMaxRefreshRateEnabled,
                        onCheckedChange = { viewModel.setForceMaxRefreshRate(it) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Lyrics Section ---
            item {
                SettingsSectionTitle("Lyrics")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppearanceNavigationItem(
                        icon = Icons.Default.FormatAlignLeft,
                        title = "Lyrics Text Position",
                        subtitle = uiState.lyricsTextPosition.label,
                        onClick = { showLyricsPositionSheet = true }
                    )
                    
                    HorizontalDivider()
                    
                    AppearanceNavigationItem(
                        icon = Icons.Default.Animation,
                        title = "Lyrics Animation Style",
                        subtitle = uiState.lyricsAnimationType.label,
                        onClick = { showLyricsAnimationSheet = true }
                    )

                    HorizontalDivider()

                    AppearanceSliderItem(
                        icon = Icons.Default.BlurOn,
                        title = "Lyrics Blur Intensity",
                        value = uiState.lyricsBlur,
                        onValueChange = { viewModel.setLyricsBlur(it) },
                        valueRange = 0f..12f
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    // Theme Mode Bottom Sheet
    if (showThemeModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeModeSheet = false },
            sheetState = themeModeSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                ThemeMode.entries.forEach { mode ->
                    ListItem(
                        headlineContent = { Text(mode.label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    scope.launch {
                                        themeModeSheetState.hide()
                                        showThemeModeSheet = false
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
    
    // App Theme Bottom Sheet
    if (showAppThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppThemeSheet = false },
            sheetState = appThemeSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                AppTheme.entries.forEach { theme ->
                    ListItem(
                        headlineContent = { Text(theme.label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.appTheme == theme,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setAppTheme(theme)
                                    scope.launch {
                                        appThemeSheetState.hide()
                                        showAppThemeSheet = false
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

    // Player Style Bottom Sheet
    if (showPlayerStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerStyleSheet = false },
            sheetState = playerStyleSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Player Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                PlayerStyle.entries.forEach { style ->
                    ListItem(
                        headlineContent = { Text(style.label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.playerStyle == style,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setPlayerStyle(style)
                                    scope.launch {
                                        playerStyleSheetState.hide()
                                        showPlayerStyleSheet = false
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
    
    // Lyrics Position Bottom Sheet
    if (showLyricsPositionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsPositionSheet = false },
            sheetState = lyricsPositionSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Lyrics Text Position",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                val positions = LyricsTextPosition.entries
                positions.forEach { position ->
                    ListItem(
                        headlineContent = { Text(position.label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.lyricsTextPosition == position,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setLyricsTextPosition(position)
                                    scope.launch {
                                        lyricsPositionSheetState.hide()
                                        showLyricsPositionSheet = false
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
    
    // Lyrics Animation Bottom Sheet
    if (showLyricsAnimationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsAnimationSheet = false },
            sheetState = lyricsAnimationSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Lyrics Animation Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                val animationTypes = LyricsAnimationType.entries
                animationTypes.forEach { type ->
                    ListItem(
                        headlineContent = { Text(type.label) },
                        leadingContent = {
                            RadioButton(
                                selected = uiState.lyricsAnimationType == type,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    viewModel.setLyricsAnimationType(type)
                                    scope.launch {
                                        lyricsAnimationSheetState.hide()
                                        showLyricsAnimationSheet = false
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
private fun AppearanceNavigationItem(
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
private fun AppearanceSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    ListItem(
        headlineContent = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.padding(top = 4.dp)
            )
        },
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
        modifier = Modifier.clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AppearanceSwitchItem(
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
