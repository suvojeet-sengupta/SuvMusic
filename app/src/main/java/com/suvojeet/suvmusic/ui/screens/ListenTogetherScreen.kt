package com.suvojeet.suvmusic.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.shareplay.*
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherScreen(
    onDismiss: () -> Unit,
    dominantColors: DominantColors,
    viewModel: ListenTogetherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isLogActive by viewModel.isLogActive.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val bufferingUsers by viewModel.bufferingUsers.collectAsState()
    
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val savedUsername by viewModel.savedUsername.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    
    val autoApproval by viewModel.autoApproval.collectAsState()
    val syncVolume by viewModel.syncVolume.collectAsState()
    val muteHost by viewModel.muteHost.collectAsState()
    
    var username by remember { mutableStateOf("") }
    
    LaunchedEffect(savedUsername) {
        if (username.isEmpty() && savedUsername.isNotEmpty()) {
            username = savedUsername
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MeshGradientBackground(
                dominantColors = dominantColors
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                ListenTogetherHeader(onDismiss = onDismiss, connectionState = connectionState)

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = when {
                            uiState.isInRoom -> "room"
                            connectionState == ConnectionState.CONNECTED -> "setup"
                            else -> "connect"
                        },
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(stiffness = 1500f)) + 
                             slideInVertically { it / 4 })
                                .togetherWith(fadeOut(animationSpec = spring()) + 
                                             slideOutVertically { -it / 4 })
                        },
                        label = "ScreenTransition"
                    ) { target ->
                        when (target) {
                            "room" -> RoomContent(
                                uiState = uiState,
                                onLeaveRoom = { viewModel.leaveRoom() },
                                onCopyCode = { code ->
                                    scope.launch {
                                        clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("Room Code", code)))
                                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSync = { viewModel.requestSync() },
                                onBlockUser = { viewModel.blockUser(it) },
                                bufferingUsers = bufferingUsers,
                                viewModel = viewModel,
                                dominantColors = dominantColors
                            )
                            "setup" -> SetupContent(
                                username = username,
                                onUsernameChange = { 
                                    username = it
                                    viewModel.updateSavedUsername(it)
                                },
                                onCreateRoom = { viewModel.createRoom(username) },
                                onJoinRoom = { code -> viewModel.joinRoom(code, username) },
                                connectionState = connectionState,
                                onDisconnect = { viewModel.disconnectFromServer() },
                                serverUrl = serverUrl,
                                onServerUrlChange = { viewModel.updateServerUrl(it) },
                                autoApproval = autoApproval,
                                onAutoApprovalChange = { viewModel.updateAutoApproval(it) },
                                syncVolume = syncVolume,
                                onSyncVolumeChange = { viewModel.updateSyncVolume(it) },
                                muteHost = muteHost,
                                onMuteHostChange = { viewModel.updateMuteHost(it) },
                                isLogActive = isLogActive,
                                onLogActiveChange = { viewModel.setLogActive(it) },
                                logs = logs,
                                onClearLogs = { viewModel.clearLogs() },
                                blockedUsers = blockedUsers,
                                onUnblockUser = { viewModel.unblockUser(it) },
                                dominantColors = dominantColors
                            )
                            "connect" -> ConnectToServerContent(
                                connectionState = connectionState,
                                onConnect = { viewModel.connectToServer() },
                                serverUrl = serverUrl,
                                onServerUrlChange = { viewModel.updateServerUrl(it) },
                                dominantColors = dominantColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListenTogetherHeader(onDismiss: () -> Unit, connectionState: ConnectionState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Listen Together",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.CONNECTED -> "Online"
                        ConnectionState.CONNECTING -> "Connecting..."
                        ConnectionState.RECONNECTING -> "Reconnecting..."
                        ConnectionState.ERROR -> "Connection Error"
                        else -> "Offline"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Box(
            modifier = Modifier
                .dpadFocusable(
                    onClick = onDismiss,
                    shape = CircleShape,
                )
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, "Close", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ConnectToServerContent(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    dominantColors: DominantColors
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.size(100.dp),
            shape = SquircleShape,
            color = if (connectionState == ConnectionState.ERROR) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (connectionState == ConnectionState.ERROR) Icons.Default.WifiOff else Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (connectionState == ConnectionState.ERROR) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Listen Together",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )
        
        Text(
            text = "Experience music with friends in perfect sync, no matter where they are.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 40.dp),
            lineHeight = 24.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "SERVER CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    placeholder = { Text("https://your-server.com") },
                    leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .dpadFocusable(onClick = onConnect, shape = SquircleShape),
                    shape = SquircleShape,
                    enabled = connectionState != ConnectionState.CONNECTING && serverUrl.isNotBlank(),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (connectionState == ConnectionState.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), 
                            color = MaterialTheme.colorScheme.onPrimary, 
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            "Connect to Server", 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                if (connectionState == ConnectionState.ERROR) {
                    Text(
                        text = "Could not connect to the server. Please check the URL and your connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // How it works section
        Text(
            text = "HOW IT WORKS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureItem(
                icon = Icons.Default.Sync,
                title = "Live Sync",
                desc = "Sub-millisecond latency for perfect timing.",
                modifier = Modifier.weight(1f)
            )
            FeatureItem(
                icon = Icons.Default.Group,
                title = "Groups",
                desc = "Host rooms or join friends instantly.",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        NyxCredit(onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nyx.meowery.eu/"))
            context.startActivity(intent)
        })
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = SquircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            desc, 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun SetupContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    autoApproval: Boolean,
    onAutoApprovalChange: (Boolean) -> Unit,
    syncVolume: Boolean,
    onSyncVolumeChange: (Boolean) -> Unit,
    muteHost: Boolean,
    onMuteHostChange: (Boolean) -> Unit,
    isLogActive: Boolean,
    onLogActiveChange: (Boolean) -> Unit,
    logs: List<LogEntry>,
    onClearLogs: () -> Unit,
    blockedUsers: Set<String>,
    onUnblockUser: (String) -> Unit,
    dominantColors: DominantColors
) {
    val context = LocalContext.current
    var roomCode by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showBlockedUsers by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Identity Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SquircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WHO ARE YOU?",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = { Text("Enter your name") },
                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleShape,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }

        // Action Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Host Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .dpadFocusable(onClick = { if (username.isNotBlank()) onCreateRoom() }, shape = SquircleShape),
                enabled = username.isNotBlank(),
                shape = SquircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            "Host",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            "Start a room",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Join Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp),
                shape = SquircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicTextField(
                        value = roomCode,
                        onValueChange = { if (it.length <= 8) roomCode = it.uppercase() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black, 
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (roomCode.isEmpty()) {
                                Text(
                                    "CODE", 
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    Button(
                        onClick = { onJoinRoom(roomCode) },
                        enabled = username.isNotBlank() && roomCode.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .dpadFocusable(onClick = { onJoinRoom(roomCode) }, shape = SquircleShape),
                        shape = SquircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("JOIN ROOM", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Settings and more
        SettingsSection(
            showSettings = showSettings,
            onToggleSettings = { showSettings = !showSettings },
            showLogs = showLogs,
            onToggleLogs = { showLogs = !showLogs },
            showBlocked = showBlockedUsers,
            onToggleBlocked = { showBlockedUsers = !showBlockedUsers },
            autoApproval = autoApproval,
            onAutoApprovalChange = onAutoApprovalChange,
            syncVolume = syncVolume,
            onSyncVolumeChange = onSyncVolumeChange,
            muteHost = muteHost,
            onMuteHostChange = onMuteHostChange,
            serverUrl = serverUrl,
            onServerUrlChange = onServerUrlChange,
            onDisconnect = onDisconnect,
            isLogActive = isLogActive,
            onLogActiveChange = onLogActiveChange,
            logs = logs,
            onClearLogs = onClearLogs,
            blockedUsers = blockedUsers,
            onUnblockUser = onUnblockUser,
            dominantColors = dominantColors
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NyxCredit(onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nyx.meowery.eu/"))
            context.startActivity(intent)
        })
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSection(
    showSettings: Boolean,
    onToggleSettings: () -> Unit,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
    showBlocked: Boolean,
    onToggleBlocked: () -> Unit,
    autoApproval: Boolean,
    onAutoApprovalChange: (Boolean) -> Unit,
    syncVolume: Boolean,
    onSyncVolumeChange: (Boolean) -> Unit,
    muteHost: Boolean,
    onMuteHostChange: (Boolean) -> Unit,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    onDisconnect: () -> Unit,
    isLogActive: Boolean,
    onLogActiveChange: (Boolean) -> Unit,
    logs: List<LogEntry>,
    onClearLogs: () -> Unit,
    blockedUsers: Set<String>,
    onUnblockUser: (String) -> Unit,
    dominantColors: DominantColors
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("Connection Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            trailingContent = { Icon(if (showSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) },
            modifier = Modifier.dpadFocusable(onClick = onToggleSettings, shape = SquircleShape).clip(SquircleShape),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        AnimatedVisibility(
            visible = showSettings,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = SquircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleItem(
                        title = "Auto-approve guests",
                        subtitle = "Accept requests automatically",
                        checked = autoApproval,
                        onCheckedChange = onAutoApprovalChange,
                        dominantColors = dominantColors
                    )
                    M3HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    SettingsToggleItem(
                        title = "Sync host volume",
                        subtitle = "Match volume with host",
                        checked = syncVolume,
                        onCheckedChange = onSyncVolumeChange,
                        dominantColors = dominantColors
                    )
                    M3HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    SettingsToggleItem(
                        title = "Mute host audio",
                        subtitle = "Useful for local syncing",
                        checked = muteHost,
                        onCheckedChange = onMuteHostChange,
                        dominantColors = dominantColors
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = onServerUrlChange,
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = SquircleShape,
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onToggleLogs,
                                modifier = Modifier.weight(1f).height(48.dp).dpadFocusable(onClick = onToggleLogs, shape = SquircleShape),
                                shape = SquircleShape
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Logs", style = MaterialTheme.typography.labelLarge)
                            }
                            
                            FilledTonalButton(
                                onClick = onToggleBlocked,
                                modifier = Modifier.weight(1f).height(48.dp).dpadFocusable(onClick = onToggleBlocked, shape = SquircleShape),
                                shape = SquircleShape
                            ) {
                                Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Blocked", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        
                        TextButton(
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth().dpadFocusable(onClick = onDisconnect, shape = SquircleShape),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect from Server")
                        }
                    }
                }
            }
        }

        if (showLogs) {
            LogViewSheet(
                logs = logs,
                onDismiss = onToggleLogs,
                isLogActive = isLogActive,
                onLogActiveChange = onLogActiveChange,
                onClear = onClearLogs,
                dominantColors = dominantColors
            )
        }

        if (showBlocked) {
            BlockedUsersSheet(
                blockedUsers = blockedUsers,
                onDismiss = onToggleBlocked,
                onUnblock = onUnblockUser
            )
        }
    }
}

@Composable
fun RoomContent(
    uiState: ListenTogetherUiState,
    onLeaveRoom: () -> Unit,
    onCopyCode: (String) -> Unit,
    onSync: () -> Unit,
    onBlockUser: (String) -> Unit,
    bufferingUsers: List<String>,
    viewModel: ListenTogetherViewModel,
    dominantColors: DominantColors
) {
    val room = uiState.roomState ?: return
    val context = LocalContext.current
    val pendingRequests by viewModel.pendingJoinRequests.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pending Requests Banner
        if (uiState.role == RoomRole.HOST && pendingRequests.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = SquircleShape,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.GroupAdd, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.onTertiary, 
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Join Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        pendingRequests.forEach { request ->
                            RequestItem(
                                request = request,
                                onApprove = { viewModel.approveJoin(request.userId) },
                                onReject = { viewModel.rejectJoin(request.userId) },
                                dominantColors = dominantColors
                            )
                        }
                    }
                }
            }
        }

        // Room Code & Host Info
        item {
            Surface(
                shape = SquircleShape,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "ROOM CODE", 
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        onClick = { onCopyCode(room.roomCode) },
                        shape = SquircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.scale(1.1f).dpadFocusable(onClick = { onCopyCode(room.roomCode) }, shape = SquircleShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black, 
                                    letterSpacing = 6.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                Icons.Default.ContentCopy, 
                                null, 
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val hostUser = room.users.find { it.userId == room.hostId }
                    AssistChip(
                        onClick = {},
                        label = { Text("Hosted by ${hostUser?.username ?: "..."}") },
                        leadingIcon = { 
                            Icon(Icons.Default.Stars, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFD700)) 
                        },
                        shape = CircleShape,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }

        // Stats Row
        item {
            var sessionDuration by remember { mutableLongStateOf(0L) }
            LaunchedEffect(Unit) {
                while (true) {
                    sessionDuration = viewModel.getSessionDuration()
                    delay(1000)
                }
            }
            
            val durationString = remember(sessionDuration) {
                val seconds = sessionDuration / 1000
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                val s = seconds % 60
                if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
                else String.format("%02d:%02d", m, s)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(label = "ELAPSED", value = durationString, modifier = Modifier.weight(1f))
                StatItem(label = "LISTENING", value = "${room.users.size}", modifier = Modifier.weight(1f))
                StatItem(
                    label = "SYNC", 
                    value = if (room.isPlaying) "Live" else "Paused", 
                    modifier = Modifier.weight(1f), 
                    highlight = room.isPlaying, 
                    highlightColor = Color(0xFF4CAF50)
                )
            }
        }

        // Current Track
        item {
            val track = room.currentTrack
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "NOW SYNCING", 
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Surface(
                    shape = SquircleShape,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (track?.thumbnail != null) {
                            coil.compose.AsyncImage(
                                model = track.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(SquircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = SquircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track?.title ?: "Nothing Synchronized", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Black, 
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = track?.artist ?: "Host is not playing anything", 
                                style = MaterialTheme.typography.bodyMedium, 
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (room.isPlaying) {
                            Icon(
                                imageVector = Icons.Default.Equalizer, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Listeners List
        item {
            Text(
                "LISTENERS", 
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(room.users) { user: UserInfo ->
            val isBuffering = bufferingUsers.contains(user.userId)
            UserListItem(
                user = user, 
                isHost = user.userId == room.hostId, 
                isMe = user.userId == uiState.userId,
                isBuffering = isBuffering,
                accentColor = dominantColors.accent,
                onBlock = { onBlockUser(user.userId) }
            )
        }

        // Footer Actions
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Join my SuvMusic session! Code: ${room.roomCode}")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Session Code"))
                        },
                        modifier = Modifier.weight(1f).height(56.dp).dpadFocusable(onClick = { /* share logic */ }, shape = SquircleShape),
                        shape = SquircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Share", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    if (uiState.role != RoomRole.HOST) {
                        Button(
                            onClick = onSync,
                            modifier = Modifier.weight(1f).height(56.dp).dpadFocusable(onClick = onSync, shape = SquircleShape),
                            shape = SquircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Resync", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                
                TextButton(
                    onClick = onLeaveRoom,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(56.dp).dpadFocusable(onClick = onLeaveRoom, shape = SquircleShape),
                    shape = SquircleShape
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Leave Session", 
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                NyxCredit(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nyx.meowery.eu/"))
                    context.startActivity(intent)
                })
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false, highlightColor: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        color = if (highlight) highlightColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        shape = SquircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), 
                color = if (highlight) highlightColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) highlightColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun UserListItem(
    user: UserInfo, 
    isHost: Boolean, 
    isMe: Boolean,
    isBuffering: Boolean = false,
    accentColor: Color,
    onBlock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = SquircleShape,
                    color = if (isHost) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = if (isHost) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.username.take(1).uppercase(), 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isHost) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Status Indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(2.dp)
                        .background(
                            color = when {
                                isBuffering -> Color(0xFFFF9800)
                                user.isConnected -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.error
                            }, 
                            shape = CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMe) "${user.username} (You)" else user.username, 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isHost) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Verified, 
                            null, 
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = when {
                        isBuffering -> "Buffering track..."
                        user.isConnected -> "Synchronized"
                        else -> "Waiting for sync..."
                    }, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = when {
                        isBuffering -> Color(0xFFFF9800)
                        user.isConnected -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (!isMe) {
                Box {
                    IconButton(
                        onClick = { showMenu = true }, 
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.MoreHoriz, null, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu, 
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            onClick = { 
                                onBlock()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error, 
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, dominantColors: DominantColors) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.8f)
            )
        },
        modifier = Modifier.dpadFocusable(onClick = { onCheckedChange(!checked) }, shape = SquircleShape).clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun RequestItem(request: JoinRequestPayload, onApprove: (String) -> Unit, onReject: (String) -> Unit, dominantColors: DominantColors) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = SquircleShape,
        modifier = Modifier.padding(vertical = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(modifier = Modifier.size(36.dp), shape = SquircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(request.username.take(1).uppercase(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(request.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onReject(request.userId) }, 
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(
                    onClick = { onApprove(request.userId) }, 
                    modifier = Modifier.size(40.dp).background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewSheet(
    logs: List<LogEntry>,
    onDismiss: () -> Unit,
    isLogActive: Boolean,
    onLogActiveChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    dominantColors: DominantColors
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Session Terminal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Real-time event tracking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                
                FilterChip(
                    selected = isLogActive,
                    onClick = { onLogActiveChange(!isLogActive) },
                    label = { Text(if (isLogActive) "Capturing" else "Paused", fontWeight = FontWeight.Bold) },
                    leadingIcon = { 
                        Box(modifier = Modifier.size(8.dp).background(if (isLogActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline, CircleShape)) 
                    },
                    shape = SquircleShape
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = SquircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No events logged", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        reverseLayout = true
                    ) {
                        items(logs.reversed()) { entry ->
                            LogItemRow(entry)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(56.dp).dpadFocusable(onClick = onClear, shape = SquircleShape),
                    shape = SquircleShape
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Clear Logs", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp).dpadFocusable(onClick = onDismiss, shape = SquircleShape),
                    shape = SquircleShape
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.WARNING -> Color(0xFFFFB74D)
        LogLevel.INFO -> Color(0xFF4CAF50)
        LogLevel.DEBUG -> MaterialTheme.colorScheme.primary
    }
    
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.timestamp.split("T").lastOrNull()?.take(8) ?: entry.timestamp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = color.copy(alpha = 0.15f), 
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = entry.level.name, 
                    color = color, 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.message, 
            color = MaterialTheme.colorScheme.onSurface, 
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersSheet(
    blockedUsers: Set<String>,
    onDismiss: () -> Unit,
    onUnblock: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Text(
                text = "Blocked Users", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "These users cannot join your rooms.", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                fontWeight = FontWeight.Medium
            )
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = SquircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                if (blockedUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No blocked users", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(blockedUsers.toList()) { userId ->
                            ListItem(
                                headlineContent = { Text(userId, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), fontWeight = FontWeight.Bold) },
                                trailingContent = {
                                    TextButton(onClick = { onUnblock(userId) }, modifier = Modifier.dpadFocusable(onClick = { onUnblock(userId) }, shape = SquircleShape)) {
                                        Text("Unblock", fontWeight = FontWeight.Bold)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 32.dp).dpadFocusable(onClick = onDismiss, shape = SquircleShape),
                shape = SquircleShape
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NyxCredit(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Integration by ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Nyx",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun M3HorizontalDivider(modifier: Modifier = Modifier, color: Color) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        color = color
    )
}
