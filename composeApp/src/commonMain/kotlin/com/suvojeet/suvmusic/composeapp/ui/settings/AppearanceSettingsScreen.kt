package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.theme.SquircleShape
import com.suvojeet.suvmusic.core.model.AppTheme
import com.suvojeet.suvmusic.core.model.LyricsAnimationType
import com.suvojeet.suvmusic.core.model.LyricsTextPosition
import com.suvojeet.suvmusic.core.model.PlayerStyle
import com.suvojeet.suvmusic.core.model.ThemeMode
import kotlinx.coroutines.launch

/**
 * UI label for [PlayerStyle]. The enum lives in :core:model commonMain
 * without label fields (it's pure-Kotlin data); display strings are a
 * UI concern and live next to the screen that renders them. Mirror in
 * :app at the top of AppearanceSettingsScreen.kt.
 */
val PlayerStyle.label: String
    get() = when (this) {
        PlayerStyle.YT_MUSIC -> "YT Music (New)"
        PlayerStyle.CLASSIC -> "Classic (SuvMusic)"
        PlayerStyle.LIQUID_GLASS -> "Liquid Glass (iOS)"
    }

/**
 * SuvMusic Appearance settings — 4th screen ported to commonMain (after
 * About, SponsorBlock, AI). Largest port so far at ~600 lines.
 *
 * State surface: 11 values + 11 setters. The :app side keeps the Koin/
 * SettingsViewModel/DataStore plumbing and feeds state in; the host
 * (Android nav graph or Desktop window) wraps this with chrome.
 *
 * Differences vs the Android original:
 *   - No Scaffold + TopAppBar — host owns the title bar.
 *   - `dpadFocusable` (Android-only TV helper) replaced with
 *     Modifier.clickable on rows and ListItem default click handling
 *     in the bottom-sheet pickers.
 *   - Same five ModalBottomSheet pickers (theme mode, app theme, player
 *     style, lyrics text position, lyrics animation type).
 *   - "Pure Black" toggle still gates on `isSystemInDarkTheme()` —
 *     CMP-compatible (foundation API).
 *   - "Dynamic Colors" subtitle reads "Use Android 12+ wallpaper colors"
 *     verbatim from the Android original. On Desktop the toggle has no
 *     effect (kept the wording unchanged for now; a future host-aware
 *     subtitle would clean this up).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    themeMode: ThemeMode,
    pureBlackEnabled: Boolean,
    dynamicColorEnabled: Boolean,
    appTheme: AppTheme,
    playerAnimatedBackgroundEnabled: Boolean,
    albumArtDynamicColorsEnabled: Boolean,
    rotatingVinylAnimationEnabled: Boolean,
    playerStyle: PlayerStyle,
    forceMaxRefreshRateEnabled: Boolean,
    lyricsTextPosition: LyricsTextPosition,
    lyricsAnimationType: LyricsAnimationType,
    lyricsBlur: Float,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onPlayerAnimatedBackgroundChange: (Boolean) -> Unit,
    onAlbumArtDynamicColorsChange: (Boolean) -> Unit,
    onRotatingVinylAnimationChange: (Boolean) -> Unit,
    onPlayerStyleChange: (PlayerStyle) -> Unit,
    onForceMaxRefreshRateChange: (Boolean) -> Unit,
    onLyricsTextPositionChange: (LyricsTextPosition) -> Unit,
    onLyricsAnimationTypeChange: (LyricsAnimationType) -> Unit,
    onLyricsBlurChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 20.dp),
    /**
     * Slot for the host to drop a "Branding" / "App Logo" section in. The
     * picker needs platform-specific drawable resources for the previews,
     * so commonMain hands off rendering to the host. Pass `null` to hide
     * the section (e.g. on platforms that don't ship the variant assets).
     */
    brandingSection: (@Composable () -> Unit)? = null,
) {
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        // --- Theme Section ---
        item {
            SectionTitle("Theme Mode")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavRow(
                    icon = Icons.Default.DarkMode,
                    title = "Theme Mode",
                    subtitle = themeMode.label,
                    onClick = { showThemeModeSheet = true },
                )

                val isDark = when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }

                AnimatedVisibility(
                    visible = isDark,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        ThinDivider()
                        SwitchRow(
                            icon = Icons.Default.ColorLens,
                            title = "Pure Black",
                            subtitle = "True black background (#000000)",
                            checked = pureBlackEnabled,
                            onCheckedChange = onPureBlackChange,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Colors Section ---
        item {
            SectionTitle("System & Colors")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.Palette,
                    title = "Dynamic Colors",
                    subtitle = "Use Android 12+ wallpaper colors",
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange,
                )

                AnimatedVisibility(
                    visible = !dynamicColorEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        ThinDivider()
                        NavRow(
                            icon = Icons.Default.ColorLens,
                            title = "App Theme",
                            subtitle = appTheme.label,
                            onClick = { showAppThemeSheet = true },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Branding (App Logo) — host-rendered ---
        brandingSection?.let { section ->
            item {
                section()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // --- Player Section ---
        item {
            SectionTitle("Player")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.Animation,
                    title = "Dynamic Background",
                    subtitle = "Animate player background colors",
                    checked = playerAnimatedBackgroundEnabled,
                    onCheckedChange = onPlayerAnimatedBackgroundChange,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Palette,
                    title = "Album Art Dynamic Colors",
                    subtitle = "Use album art colors for player UI",
                    checked = albumArtDynamicColorsEnabled,
                    onCheckedChange = onAlbumArtDynamicColorsChange,
                )
                ThinDivider()
                SwitchRow(
                    icon = Icons.Default.Album,
                    title = "Rotating Vinyl Animation",
                    subtitle = "Rotate artwork in vinyl mode",
                    checked = rotatingVinylAnimationEnabled,
                    onCheckedChange = onRotatingVinylAnimationChange,
                )
                ThinDivider()
                NavRow(
                    icon = Icons.Default.Palette,
                    title = "Player Style",
                    subtitle = playerStyle.label,
                    onClick = { showPlayerStyleSheet = true },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Performance Section ---
        item {
            SectionTitle("Performance")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    icon = Icons.Default.Animation,
                    title = "Force Max Refresh Rate",
                    subtitle = "Enable 90Hz/120Hz for smoother UI",
                    checked = forceMaxRefreshRateEnabled,
                    onCheckedChange = onForceMaxRefreshRateChange,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Lyrics Section ---
        item {
            SectionTitle("Lyrics")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                NavRow(
                    icon = Icons.Default.FormatAlignLeft,
                    title = "Lyrics Text Position",
                    subtitle = lyricsTextPosition.label,
                    onClick = { showLyricsPositionSheet = true },
                )
                ThinDivider()
                NavRow(
                    icon = Icons.Default.Animation,
                    title = "Lyrics Animation Style",
                    subtitle = lyricsAnimationType.label,
                    onClick = { showLyricsAnimationSheet = true },
                )
                ThinDivider()
                SliderRow(
                    icon = Icons.Default.BlurOn,
                    title = "Lyrics Blur Intensity",
                    value = lyricsBlur,
                    onValueChange = onLyricsBlurChange,
                    valueRange = 0f..12f,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Theme Mode picker
    if (showThemeModeSheet) {
        EnumPickerSheet(
            title = "Theme Mode",
            entries = ThemeMode.entries,
            selected = themeMode,
            label = { it.label },
            onSelect = { mode ->
                onThemeModeChange(mode)
                scope.launch {
                    themeModeSheetState.hide()
                    showThemeModeSheet = false
                }
            },
            onDismiss = { showThemeModeSheet = false },
            sheetState = themeModeSheetState,
        )
    }

    // App Theme picker
    if (showAppThemeSheet) {
        EnumPickerSheet(
            title = "App Theme",
            entries = AppTheme.entries,
            selected = appTheme,
            label = { it.label },
            onSelect = { theme ->
                onAppThemeChange(theme)
                scope.launch {
                    appThemeSheetState.hide()
                    showAppThemeSheet = false
                }
            },
            onDismiss = { showAppThemeSheet = false },
            sheetState = appThemeSheetState,
        )
    }

    // Player Style picker
    if (showPlayerStyleSheet) {
        EnumPickerSheet(
            title = "Player Style",
            entries = PlayerStyle.entries,
            selected = playerStyle,
            label = { it.label },
            onSelect = { style ->
                onPlayerStyleChange(style)
                scope.launch {
                    playerStyleSheetState.hide()
                    showPlayerStyleSheet = false
                }
            },
            onDismiss = { showPlayerStyleSheet = false },
            sheetState = playerStyleSheetState,
        )
    }

    // Lyrics Position picker
    if (showLyricsPositionSheet) {
        EnumPickerSheet(
            title = "Lyrics Text Position",
            entries = LyricsTextPosition.entries,
            selected = lyricsTextPosition,
            label = { it.label },
            onSelect = { position ->
                onLyricsTextPositionChange(position)
                scope.launch {
                    lyricsPositionSheetState.hide()
                    showLyricsPositionSheet = false
                }
            },
            onDismiss = { showLyricsPositionSheet = false },
            sheetState = lyricsPositionSheetState,
        )
    }

    // Lyrics Animation picker
    if (showLyricsAnimationSheet) {
        EnumPickerSheet(
            title = "Lyrics Animation Style",
            entries = LyricsAnimationType.entries,
            selected = lyricsAnimationType,
            label = { it.label },
            onSelect = { type ->
                onLyricsAnimationTypeChange(type)
                scope.launch {
                    lyricsAnimationSheetState.hide()
                    showLyricsAnimationSheet = false
                }
            },
            onDismiss = { showLyricsAnimationSheet = false },
            sheetState = lyricsAnimationSheetState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumPickerSheet(
    title: String,
    entries: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
            )
            entries.forEach { entry ->
                ListItem(
                    headlineContent = { Text(label(entry)) },
                    leadingContent = {
                        RadioButton(
                            selected = selected == entry,
                            onClick = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry) }
                        .padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun ThinDivider() {
    M3HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    )
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
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
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    text = formatOneDecimal(value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        modifier = Modifier.clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/**
 * Multiplatform replacement for `"%.1f".format(value)` — `String.format`
 * isn't available in commonMain. Truncates to one decimal place by
 * integer math; matches Android's behaviour on the value range used
 * here (0.0–12.0 for lyrics blur).
 */
private fun formatOneDecimal(value: Float): String {
    val rounded = (value * 10).toInt()
    val whole = rounded / 10
    val frac = if (rounded < 0) -rounded % 10 else rounded % 10
    val sign = if (rounded < 0 && whole == 0) "-" else ""
    return "$sign$whole.$frac"
}
