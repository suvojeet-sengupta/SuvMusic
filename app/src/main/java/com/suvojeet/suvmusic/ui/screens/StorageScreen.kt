package com.suvojeet.suvmusic.ui.screens

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
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Storage management screen showing storage usage breakdown and cleanup options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    downloadRepository: DownloadRepository,
    onBackClick: () -> Unit,
    onPlayerCacheClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var storageInfo by remember { mutableStateOf<DownloadRepository.StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    
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
        topBar = {
            TopAppBar(
                title = { Text("Storage Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                storageInfo?.let { info ->
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Overview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Total Used Space",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = info.formatSize(info.totalBytes),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Storage Breakdown Section
                    Text(
                        text = "DETAILS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Downloads Item
                    StorageItem(
                        icon = Icons.Default.AudioFile,
                        title = "Downloads",
                        subtitle = "${info.downloadedSongsCount} songs offline",
                        size = info.formatSize(info.downloadedSongsBytes),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = null // No specific detail screen for downloads here, currently managed in Library
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Player Cache Item (Clickable to go to detail screen)
                    StorageItem(
                        icon = Icons.Default.Cached,
                        title = "Player Cache",
                        subtitle = "Streamed songs",
                        size = info.formatSize(info.cacheBytes),
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = onPlayerCacheClick,
                        showChevron = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Thumbnails Item
                    StorageItem(
                        icon = Icons.Default.Image,
                        title = "Thumbnails",
                        subtitle = "Album artwork images",
                        size = info.formatSize(info.thumbnailsBytes),
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = null
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Actions Section
                    Text(
                        text = "ACTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Delete All Downloads Button
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        enabled = !isClearing && info.downloadedSongsCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All Downloads")
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    
    // Delete All Downloads Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Downloads?") },
            text = { 
                Text("This will permanently delete all downloaded songs. This action cannot be undone.") 
            },
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
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    size: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = size,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            if (showChevron) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
