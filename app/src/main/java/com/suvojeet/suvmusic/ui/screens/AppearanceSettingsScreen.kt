package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "Appearance",
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            // COLOR THEME Section
            item { M3ESettingsGroupHeader("COLOR THEME") }
            item {
                ThemePaletteRow(
                    themes = AppTheme.entries,
                    selected = uiState.appTheme,
                    onSelect = { viewModel.setAppTheme(it) }
                )
            }

            // DISPLAY Section
            item { M3ESettingsGroupHeader("DISPLAY") }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Dark Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    M3EButtonGroup(
                        options = ThemeMode.entries,
                        selected = uiState.themeMode,
                        onSelect = { viewModel.setThemeMode(it) },
                        label = { it.label }
                    )
                }
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.ColorLens,
                    title = "Material You",
                    subtitle = "Use dynamic wallpaper colors",
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "Pure Black AMOLED",
                    subtitle = "True black in dark mode",
                    checked = uiState.pureBlackEnabled,
                    onCheckedChange = { viewModel.setPureBlackEnabled(it) }
                )
            }

            // PLAYER Section
            item { M3ESettingsGroupHeader("PLAYER") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.Animation,
                    title = "Animated Background",
                    subtitle = "Fluid mesh gradients in player",
                    checked = uiState.playerAnimatedBackgroundEnabled,
                    onCheckedChange = { viewModel.setPlayerAnimatedBackgroundEnabled(it) }
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.FormatAlignLeft,
                    title = "Lyrics Position",
                    subtitle = uiState.lyricsTextPosition.label,
                    onClick = { /* show sheet */ }
                )
            }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Animation,
                    title = "Lyrics Animation",
                    subtitle = uiState.lyricsAnimationType.label,
                    onClick = { /* show sheet */ }
                )
            }
            
            // PERFORMANCE Section
            item { M3ESettingsGroupHeader("PERFORMANCE") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.FlashOn,
                    title = "Force Max Refresh Rate",
                    subtitle = "Smoother UI animations",
                    checked = uiState.forceMaxRefreshRateEnabled,
                    onCheckedChange = { viewModel.setForceMaxRefreshRate(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemePaletteRow(
    themes: List<AppTheme>,
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(themes) { theme ->
            val isSelected = theme == selected
            // Local mapping for theme primary colors
            val themeColor = when (theme) {
                AppTheme.DEFAULT -> Color(0xFFD0BCFF) // Purple70
                AppTheme.OCEAN -> Color(0xFF82D3FF)   // Blue80
                AppTheme.SUNSET -> Color(0xFFFFB74D)  // Orange80
                AppTheme.NATURE -> Color(0xFF81C784)  // Green80
                AppTheme.LOVE -> Color(0xFFF06292)    // Pink80
            }
            
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "theme_scale"
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable { onSelect(theme) }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(themeColor, CircleShape)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = theme.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
