package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerCacheScreen(
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel,
    downloadRepository: DownloadRepository
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var storageInfo by remember { mutableStateOf<DownloadRepository.StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageInfo = downloadRepository.getStorageInfo()
            isLoading = false
        }
    }
    
    fun refreshStorageInfo() {
        scope.launch {
            withContext(Dispatchers.IO) {
                storageInfo = downloadRepository.getStorageInfo()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "Player Cache",
                onBack = onBackClick,
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
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        M3ELoadingIndicator()
                    }
                }
            } else {
                storageInfo?.let { info ->
                    item {
                        M3ECacheOverviewCard(info, uiState.playerCacheLimit)
                    }

                    item { M3ESettingsGroupHeader("CACHE SIZE") }
                    item {
                        M3ESwitchItem(
                            icon = Icons.Default.AllInclusive,
                            title = "Unlimited Cache",
                            subtitle = "Allow cache to grow without limit",
                            checked = uiState.playerCacheLimit == -1L,
                            onCheckedChange = { checked ->
                                settingsViewModel.setPlayerCacheLimit(if (checked) -1L else 500L * 1024 * 1024)
                            }
                        )
                    }

                    if (uiState.playerCacheLimit != -1L) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    "Maximum Cache Size",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                )
                                val options = listOf(
                                    250L * 1024 * 1024 to "250 MB",
                                    500L * 1024 * 1024 to "500 MB",
                                    1024L * 1024 * 1024 to "1 GB",
                                    2L * 1024 * 1024 * 1024 to "2 GB",
                                    5L * 1024 * 1024 * 1024 to "5 GB"
                                )
                                M3EButtonGroup(
                                    options = options,
                                    selected = options.find { it.first == uiState.playerCacheLimit } ?: options[1],
                                    onSelect = { settingsViewModel.setPlayerCacheLimit(it.first) },
                                    label = { it.second }
                                )
                            }
                        }
                    }

                    item { M3ESettingsGroupHeader("CACHE STATISTICS") }
                    item {
                        M3ESettingsItem(
                            icon = Icons.Default.Info,
                            title = "Cache Usage",
                            subtitle = "${info.formatSize(info.progressiveCacheBytes)} used",
                            onClick = null
                        )
                    }

                    item { M3ESettingsGroupHeader("ACTIONS") }
                    item {
                        M3ESettingsItem(
                            icon = Icons.Default.DeleteSweep,
                            title = "Clear Player Cache",
                            subtitle = "Remove all cached streams",
                            iconTint = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            onClick = { if (info.progressiveCacheBytes > 0) showClearCacheDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Player Cache?", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = { Text("This will remove all cached songs. Downloaded songs will NOT be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        isClearing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.clearProgressiveCache()
                                refreshStorageInfo()
                            }
                            isClearing = false
                        }
                    }
                ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3ECacheOverviewCard(info: DownloadRepository.StorageInfo, limitBytes: Long) {
    val usedBytes = info.progressiveCacheBytes
    val isUnlimited = limitBytes == -1L
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Cached, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.height(16.dp))
            Text(if (isUnlimited) "Unlimited Cache" else "Cache Usage", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
                text = info.formatSize(usedBytes) + if (isUnlimited) "" else " / ${info.formatSize(limitBytes)}",
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            if (!isUnlimited && limitBytes > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = (usedBytes.toFloat() / limitBytes).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}
