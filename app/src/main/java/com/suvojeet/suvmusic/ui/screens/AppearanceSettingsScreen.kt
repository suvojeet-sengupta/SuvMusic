package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.util.dpadFocusable
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android host for the shared AppearanceSettingsScreen.
 *
 * This is the only thing left in :app for this screen — the entire body
 * (settings rows, bottom-sheet pickers, ~600 lines) lives in
 * :composeApp/commonMain so Desktop and Android render identical UI from
 * one source. :app keeps the title bar (with hardware-back-aware
 * dpadFocusable nav icon) and unpacks SettingsViewModel state into the
 * stateless commonMain composable.
 *
 * NavGraph still calls `AppearanceSettingsScreen(viewModel, onBack)` — no
 * route changes needed. Migration pattern: any screen already ported to
 * composeApp/commonMain can shed its :app body the same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { paddingValues ->
        com.suvojeet.suvmusic.composeapp.ui.settings.AppearanceSettingsScreen(
            themeMode = uiState.themeMode,
            pureBlackEnabled = uiState.pureBlackEnabled,
            dynamicColorEnabled = uiState.dynamicColorEnabled,
            appTheme = uiState.appTheme,
            playerAnimatedBackgroundEnabled = uiState.playerAnimatedBackgroundEnabled,
            albumArtDynamicColorsEnabled = uiState.albumArtDynamicColorsEnabled,
            rotatingVinylAnimationEnabled = uiState.rotatingVinylAnimationEnabled,
            playerStyle = uiState.playerStyle,
            forceMaxRefreshRateEnabled = uiState.forceMaxRefreshRateEnabled,
            lyricsTextPosition = uiState.lyricsTextPosition,
            lyricsAnimationType = uiState.lyricsAnimationType,
            lyricsBlur = uiState.lyricsBlur,
            onThemeModeChange = viewModel::setThemeMode,
            onPureBlackChange = viewModel::setPureBlackEnabled,
            onDynamicColorChange = viewModel::setDynamicColor,
            onAppThemeChange = viewModel::setAppTheme,
            onPlayerAnimatedBackgroundChange = viewModel::setPlayerAnimatedBackgroundEnabled,
            onAlbumArtDynamicColorsChange = viewModel::setAlbumArtDynamicColorsEnabled,
            onRotatingVinylAnimationChange = viewModel::setRotatingVinylAnimationEnabled,
            onPlayerStyleChange = viewModel::setPlayerStyle,
            onForceMaxRefreshRateChange = viewModel::setForceMaxRefreshRate,
            onLyricsTextPositionChange = viewModel::setLyricsTextPosition,
            onLyricsAnimationTypeChange = viewModel::setLyricsAnimationType,
            onLyricsBlurChange = viewModel::setLyricsBlur,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp,
            ),
        )
    }
}
