package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.*
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
fun LyricsProvidersScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "Lyrics Providers",
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
            item {
                Text(
                    text = "The preferred provider will be tried first. Only enabled providers can be selected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            item { M3ESettingsGroupHeader("PROVIDERS") }
            
            item {
                LyricsProviderItemM3E(
                    title = "BetterLyrics (Apple Music)",
                    subtitle = "Time-synced lyrics database",
                    enabled = uiState.betterLyricsEnabled,
                    onEnabledChange = { viewModel.setBetterLyricsEnabled(it) },
                    isPreferred = uiState.preferredLyricsProvider == "BetterLyrics",
                    onSelectPreferred = { viewModel.setPreferredLyricsProvider("BetterLyrics") }
                )
            }
            
            item {
                LyricsProviderItemM3E(
                    title = "SimpMusic",
                    subtitle = "Community sourced lyrics",
                    enabled = uiState.simpMusicEnabled,
                    onEnabledChange = { viewModel.setSimpMusicEnabled(it) },
                    isPreferred = uiState.preferredLyricsProvider == "SimpMusic",
                    onSelectPreferred = { viewModel.setPreferredLyricsProvider("SimpMusic") }
                )
            }
            
            item {
                LyricsProviderItemM3E(
                    title = "Kugou",
                    subtitle = "Massive lyrics library",
                    enabled = uiState.kuGouEnabled,
                    onEnabledChange = { viewModel.setKuGouEnabled(it) },
                    isPreferred = uiState.preferredLyricsProvider == "Kugou",
                    onSelectPreferred = { viewModel.setPreferredLyricsProvider("Kugou") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricsProviderItemM3E(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isPreferred: Boolean,
    onSelectPreferred: () -> Unit
) {
    Column {
        M3ESwitchItem(
            icon = Icons.Default.Lyrics,
            title = title,
            subtitle = subtitle,
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
        
        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ListItem(
                headlineContent = { 
                    Text(
                        text = "Preferred",
                        style = if (isPreferred) MaterialTheme.typography.bodyMediumEmphasized else MaterialTheme.typography.bodyMedium,
                        color = if (isPreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                leadingContent = {
                    RadioButton(
                        selected = isPreferred,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 16.dp)
                    .clickable { onSelectPreferred() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}
