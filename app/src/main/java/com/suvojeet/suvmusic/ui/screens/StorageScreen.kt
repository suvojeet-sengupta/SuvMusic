package com.suvojeet.suvmusic.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun StorageScreen(
    downloadRepository: DownloadRepository,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onPlayerCacheClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var storageInfo by remember { mutableStateOf<DownloadRepository.StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showClearThumbnailsDialog by remember { mutableStateOf(false) }
    var showClearImageCacheDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settingsViewModel.setDownloadLocation(it.toString())
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageInfo = downloadRepository.getStorageInfo()
            isLoading = false
        }
    }
    
    fun refreshInfo() {
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
                title = "Storage",
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
                        M3EStorageOverviewCard(info)
                    }

                    item { M3ESettingsGroupHeader("DOWNLOAD LOCATION") }
                    item {
                        M3ENavigationItem(
                            icon = Icons.Default.Folder,
                            title = "Download Location",
                            subtitle = if (uiState.downloadLocation == null) "Default (Music/SuvMusic)" else "Custom folder",
                            onClick = { folderPickerLauncher.launch(null) }
                        )
                    }

                    item { M3ESettingsGroupHeader("CACHE") }
                    item {
                        M3ENavigationItem(
                            icon = Icons.Default.Cached,
                            title = "Player Cache",
                            subtitle = info.formatSize(info.progressiveCacheBytes),
                            onClick = onPlayerCacheClick
                        )
                    }
                    item {
                        M3ENavigationItem(
                            icon = Icons.Default.Image,
                            title = "Image Cache",
                            subtitle = info.formatSize(info.imageCacheBytes),
                            onClick = { showClearImageCacheDialog = true }
                        )
                    }
                    item {
                        M3ENavigationItem(
                            icon = Icons.Default.PhotoLibrary,
                            title = "Thumbnails",
                            subtitle = info.formatSize(info.thumbnailsBytes),
                            onClick = { showClearThumbnailsDialog = true }
                        )
                    }

                    item { M3ESettingsGroupHeader("CLEANUP ACTIONS") }
                    item {
                        M3ESettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete All Downloads",
                            subtitle = "${info.downloadedSongsCount} songs offline",
                            iconTint = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            onClick = { if (info.downloadedSongsCount > 0) showDeleteAllDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Downloads?", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = { Text("This will permanently delete all downloaded songs.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        isClearing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.deleteAllDownloads()
                                refreshInfo()
                            }
                            isClearing = false
                        }
                    }
                ) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") } }
        )
    }

    // Clear Image Cache Dialog
    if (showClearImageCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearImageCacheDialog = false },
            title = { Text("Clear Image Cache?", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = { Text("This will remove all temporary images cached by the app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearImageCacheDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.clearImageCache()
                                refreshInfo()
                            }
                        }
                    }
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearImageCacheDialog = false }) { Text("Cancel") } }
        )
    }

    // Clear Thumbnails Dialog
    if (showClearThumbnailsDialog) {
        AlertDialog(
            onDismissRequest = { showClearThumbnailsDialog = false },
            title = { Text("Clear Thumbnails?", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = { Text("This will remove all downloaded album artworks.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearThumbnailsDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.clearThumbnails()
                                refreshInfo()
                            }
                        }
                    }
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearThumbnailsDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EStorageOverviewCard(info: DownloadRepository.StorageInfo) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SdStorage, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(16.dp))
            Text("Total Used Space", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(info.formatSize(info.totalBytes), style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
