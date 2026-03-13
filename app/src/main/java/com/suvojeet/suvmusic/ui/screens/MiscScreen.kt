package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiscScreen(
    onBack: () -> Unit,
    onLyricsProvidersClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "Misc",
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
            item { M3ESettingsGroupHeader("LYRICS") }
            item {
                M3ENavigationItem(
                    icon = Icons.Default.Lyrics,
                    title = "Lyrics Providers",
                    subtitle = "Manage lyric sources",
                    onClick = onLyricsProvidersClick
                )
            }

            item { M3ESettingsGroupHeader("BEHAVIOR") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.Notes,
                    title = "Stop music on task clear",
                    checked = uiState.stopMusicOnTaskClear,
                    onCheckedChange = { viewModel.setStopMusicOnTaskClear(it) }
                )
            }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.VolumeOff,
                    title = "Pause on media muted",
                    checked = uiState.pauseMusicOnMediaMuted,
                    onCheckedChange = { viewModel.setPauseMusicOnMediaMuted(it) }
                )
            }

            item { M3ESettingsGroupHeader("NOTIFICATIONS") }
            // Placeholder for notification settings if any

            item { M3ESettingsGroupHeader("DEVELOPER") }
            item {
                M3ESwitchItem(
                    icon = Icons.Default.Smartphone,
                    title = "Keep screen on",
                    subtitle = "When player is expanded",
                    checked = uiState.keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )
            }
        }
    }
}
