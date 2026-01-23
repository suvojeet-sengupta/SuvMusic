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
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCacheScreen(
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel,
    downloadRepository: DownloadRepository
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var storageInfo by remember { mutableStateOf<DownloadRepository.StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    
    // Load storage info
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageInfo = downloadRepository.getStorageInfo()
            isLoading = false
        }
    }
    
    // Refresh info when cache is cleared
    fun refreshStorageInfo() {
        scope.launch {
            withContext(Dispatchers.IO) {
                storageInfo = downloadRepository.getStorageInfo()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Cache", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp),
        ) {
            // Usage Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val usedBytes = storageInfo?.cacheBytes ?: 0L
                    val limitBytes = uiState.playerCacheLimit
                    val isUnlimited = limitBytes == -1L
                    
                    Text(
                        text = if (isUnlimited) "Unlimited Cache" else "Cache Usage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = formatSize(usedBytes) + if (isUnlimited) "" else " / " + formatSize(limitBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    if (!isUnlimited && limitBytes > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val progress = (usedBytes.toFloat() / limitBytes).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            // Info Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Player cache stores songs you stream so they don't need to be re-downloaded. This allows for instant playback and offline access to recently played songs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Limit Settings
            // Unlimited Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (uiState.playerCacheLimit == -1L) {
                            settingsViewModel.setPlayerCacheLimit(500L * 1024 * 1024) // Revert to default
                        } else {
                            settingsViewModel.setPlayerCacheLimit(-1L)
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unlimited Cache",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Allow cache to grow without limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = uiState.playerCacheLimit == -1L,
                    onCheckedChange = { checked ->
                        if (checked) {
                            settingsViewModel.setPlayerCacheLimit(-1L)
                        } else {
                            settingsViewModel.setPlayerCacheLimit(500L * 1024 * 1024)
                        }
                    }
                )
            }
            
            // Fixed Limit Options (only if not unlimited)
            if (uiState.playerCacheLimit != -1L) {
                val currentLimit = uiState.playerCacheLimit
                val options = listOf(
                    250L * 1024 * 1024 to "250 MB",
                    500L * 1024 * 1024 to "500 MB",
                    1024L * 1024 * 1024 to "1 GB",
                    2L * 1024 * 1024 * 1024 to "2 GB",
                    5L * 1024 * 1024 * 1024 to "5 GB",
                    10L * 1024 * 1024 * 1024 to "10 GB"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Maximum Cache Size",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                options.forEach { (size, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsViewModel.setPlayerCacheLimit(size) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLimit == size,
                            onClick = { settingsViewModel.setPlayerCacheLimit(size) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) 
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                   Text(
                       text = "Note: Changes to cache limit may require an app restart to take full effect.",
                       style = MaterialTheme.typography.bodySmall,
                       color = MaterialTheme.colorScheme.onTertiaryContainer,
                       modifier = Modifier.padding(12.dp)
                   ) 
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Actions
            OutlinedButton(
                onClick = { showClearCacheDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClearing && (storageInfo?.cacheBytes ?: 0L) > 0,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Player Cache")
            }
            
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
    
    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Player Cache?") },
            text = { 
                Text("This will remove all cached songs. Downloaded songs will NOT be affected.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        isClearing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadRepository.clearCache()
                                storageInfo = downloadRepository.getStorageInfo()
                            }
                            isClearing = false
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
