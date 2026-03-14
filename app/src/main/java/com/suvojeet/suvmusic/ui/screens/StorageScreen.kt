package com.suvojeet.suvmusic.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Storage management screen with Material 3 Expressive design.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    
    var storageInfo by remember { mutableStateOf<DownloadRepository.StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showClearThumbnailsDialog by remember { mutableStateOf(false) }
    var showClearImageCacheDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    
    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settingsViewModel.setDownloadLocation(it.toString())
        }
    }

    // Load storage info
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageInfo = downloadRepository.getStorageInfo()
            isLoading = false
        }
    }
    
    // Refresh info helper
    fun refreshInfo() {
        scope.launch {
            withContext(Dispatchers.IO) {
                storageInfo = downloadRepository.getStorageInfo()
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Storage Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(
                                onClick = onBackClick,
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
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    }
                }
            } else {
                storageInfo?.let { info ->
                    // Overview Section
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = SquircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(SquircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SdStorage,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Total Used Space",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = info.formatSize(info.totalBytes),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-1).sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Details Section
                    item {
                        SettingsSectionTitle("Details")
                        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            // Downloads Item
                            StorageListItem(
                                icon = Icons.Default.AudioFile,
                                title = "Downloads",
                                subtitle = "${info.downloadedSongsCount} songs offline",
                                size = info.formatSize(info.downloadedSongsBytes),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = null
                            )
                            
                            M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            // Download Location Item
                            StorageListItem(
                                icon = Icons.Default.Folder,
                                title = "Download Location",
                                subtitle = if (uiState.downloadLocation == null) "Default (Music/SuvMusic)" else "Custom folder set",
                                size = if (uiState.downloadLocation == null) "" else "Custom",
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = { folderPickerLauncher.launch(null) },
                                showChevron = true
                            )

                            M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            // Player Cache Item
                            StorageListItem(
                                icon = Icons.Default.Cached,
                                title = "Player Cache",
                                subtitle = "Streamed songs",
                                size = info.formatSize(info.progressiveCacheBytes),
                                color = MaterialTheme.colorScheme.tertiary,
                                onClick = onPlayerCacheClick,
                                showChevron = true
                            )
                            
                            M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            // Image Cache Item
                            StorageListItem(
                                icon = Icons.Default.Image,
                                title = "Image Cache",
                                subtitle = "Temporary app images",
                                size = info.formatSize(info.imageCacheBytes),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { showClearImageCacheDialog = true }
                            )
                            
                            M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            
                            // Thumbnails Item
                            StorageListItem(
                                icon = Icons.Default.Image,
                                title = "Thumbnails",
                                subtitle = "Album artwork images",
                                size = info.formatSize(info.thumbnailsBytes),
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = { showClearThumbnailsDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    // Actions Section
                    item {
                        SettingsSectionTitle("Actions")
                        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        "Delete All Downloads", 
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(SquircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .dpadFocusable(
                                        onClick = { showDeleteAllDialog = true },
                                        enabled = !isClearing && info.downloadedSongsCount > 0,
                                        shape = SquircleShape
                                    )
                                    .clip(SquircleShape),
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                    disabledHeadlineContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Downloads?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all downloaded songs. This action cannot be undone.") },
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
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            },
            shape = SquircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (showClearImageCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearImageCacheDialog = false },
            title = { Text("Clear Image Cache?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all temporary images cached by the app. They will be re-downloaded when needed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearImageCacheDialog = false
                        isClearing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.clearImageCache()
                                refreshInfo()
                            }
                            isClearing = false
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearImageCacheDialog = false }) { Text("Cancel") }
            },
            shape = SquircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (showClearThumbnailsDialog) {
        AlertDialog(
            onDismissRequest = { showClearThumbnailsDialog = false },
            title = { Text("Clear Thumbnails?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all downloaded album artworks. They will be re-downloaded when needed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearThumbnailsDialog = false
                        isClearing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.clearThumbnails()
                                refreshInfo()
                            }
                            isClearing = false
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearThumbnailsDialog = false }) { Text("Cancel") }
            },
            shape = SquircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
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
private fun StorageListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    size: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = size,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (showChevron) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        },
        modifier = Modifier
            .then(
                if (onClick != null) {
                    Modifier.dpadFocusable(onClick = onClick, shape = SquircleShape)
                } else Modifier
            )
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
