package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType
import com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Appearance settings screen with theme mode and dynamic color options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeModeSheet by remember { mutableStateOf(false) }
    val themeModeSheetState = rememberModalBottomSheetState()
    
    var showAppThemeSheet by remember { mutableStateOf(false) }
    val appThemeSheetState = rememberModalBottomSheetState()
    
    var showLyricsPositionSheet by remember { mutableStateOf(false) }
    val lyricsPositionSheetState = rememberModalBottomSheetState()
    
    var showLyricsAnimationSheet by remember { mutableStateOf(false) }
    val lyricsAnimationSheetState = rememberModalBottomSheetState()
    
    val scope = rememberCoroutineScope()
    
    // Background Gradient (Consistent with other screens)
    val backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
            ) {
                // --- Theme Section ---
                item {
                    SettingsSectionTitle("Theme Mode")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
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
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AppearanceSwitchItem(
                            icon = Icons.Default.Animation,
                            title = "Dynamic Background",
                            subtitle = "Animate player background colors",
                            checked = uiState.playerAnimatedBackgroundEnabled,
                            onCheckedChange = { viewModel.setPlayerAnimatedBackgroundEnabled(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Performance Section ---
                item {
                    SettingsSectionTitle("Performance")
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AppearanceSwitchItem(
                            icon = Icons.Default.Animation, // Using Animation icon or similar
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
                    GlassmorphicCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AppearanceNavigationItem(
                            icon = Icons.Default.FormatAlignLeft,
                            title = "Lyrics Text Position",
                            subtitle = uiState.lyricsTextPosition.label,
                            onClick = { showLyricsPositionSheet = true }
                        )
                        
                        // Use Divider instead of HorizontalDivider if not resolved
                        androidx.compose.material3.HorizontalDivider()
                        
                        AppearanceNavigationItem(
                            icon = Icons.Default.Animation,
                            title = "Lyrics Animation Style",
                            subtitle = uiState.lyricsAnimationType.label,
                            onClick = { showLyricsAnimationSheet = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    
    // Theme Mode Bottom Sheet
    if (showThemeModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeModeSheet = false },
            sheetState = themeModeSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
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
                            .clickable {
                                viewModel.setThemeMode(mode)
                                scope.launch {
                                    themeModeSheetState.hide()
                                    showThemeModeSheet = false
                                }
                            },
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
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
                            .clickable {
                                viewModel.setAppTheme(theme)
                                scope.launch {
                                    appThemeSheetState.hide()
                                    showAppThemeSheet = false
                                }
                            },
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Lyrics Text Position",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
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
                            .clickable {
                                viewModel.setLyricsTextPosition(position)
                                scope.launch {
                                    lyricsPositionSheetState.hide()
                                    showLyricsPositionSheet = false
                                }
                            },
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Lyrics Animation Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
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
                            .clickable {
                                viewModel.setLyricsAnimationType(type)
                                scope.launch {
                                    lyricsAnimationSheetState.hide()
                                    showLyricsAnimationSheet = false
                                }
                            },
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
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        tonalElevation = 0.dp
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
private fun AppearanceNavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AppearanceSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
