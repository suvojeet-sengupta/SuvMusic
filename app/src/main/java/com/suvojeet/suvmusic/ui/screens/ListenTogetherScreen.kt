package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.shareplay.*
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.scale

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
                        .size(6.dp)
                        .background(
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.CONNECTED -> "Online"
                        ConnectionState.CONNECTING -> "Connecting..."
                        ConnectionState.RECONNECTING -> "Reconnecting..."
                        ConnectionState.ERROR -> "Connection Error"
                        else -> "Offline"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = if (connectionState == ConnectionState.ERROR) Icons.Default.WifiOff else Icons.Default.Wifi
        
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (connectionState == ConnectionState.ERROR) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (connectionState == ConnectionState.ERROR) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Connect to Session",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Sync music with friends in real-time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server URL") },
            leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = connectionState != ConnectionState.CONNECTING
        ) {
            if (connectionState == ConnectionState.CONNECTING) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Connect", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IDENTIFICATION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = { Text("Enter your name") },
                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                onClick = { if (username.isNotBlank()) onCreateRoom() },
                enabled = username.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "HOST ROOM",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier
                    .weight(1.2f)
                    .height(110.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BasicTextField(
                        value = roomCode,
                        onValueChange = { if (it.length <= 8) roomCode = it.uppercase() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, 
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (roomCode.isEmpty()) {
                                Text(
                                    "ROOM CODE", 
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    Button(
                        onClick = { onJoinRoom(roomCode) },
                        enabled = username.isNotBlank() && roomCode.length >= 4,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("JOIN", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

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
        
        Spacer(modifier = Modifier.height(16.dp))
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
            modifier = Modifier.clickable { onToggleSettings() }.clip(RoundedCornerShape(12.dp)),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        AnimatedVisibility(visible = showSettings) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleItem(
                        title = "Auto-approve guests",
                        subtitle = "Accept requests automatically",
                        checked = autoApproval,
                        onCheckedChange = onAutoApprovalChange,
                        dominantColors = dominantColors
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsToggleItem(
                        title = "Sync host volume",
                        subtitle = "Match volume with host",
                        checked = syncVolume,
                        onCheckedChange = onSyncVolumeChange,
                        dominantColors = dominantColors
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onToggleLogs,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Logs", style = MaterialTheme.typography.labelLarge)
                            }
                            
                            FilledTonalButton(
                                onClick = onToggleBlocked,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Blocked", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        
                        TextButton(
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth(),
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
    viewModel: ListenTogetherViewModel,
    dominantColors: DominantColors
) {
    val room = uiState.roomState ?: return
    val pendingRequests by viewModel.pendingJoinRequests.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.role == RoomRole.HOST && pendingRequests.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Join Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
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

        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SESSION CODE", 
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 4.sp
                        ),
                        modifier = Modifier.clickable { onCopyCode(room.roomCode) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val hostUser = room.users.find { it.userId == room.hostId }
                    AssistChip(
                        onClick = {},
                        label = { Text("Host: ${hostUser?.username ?: "..."}") },
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                        shape = CircleShape
                    )
                }
            }
        }

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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(label = "UPTIME", value = durationString, modifier = Modifier.weight(1f))
                StatItem(label = "LISTENERS", value = "${room.users.size}", modifier = Modifier.weight(1f))
                StatItem(
                    label = "STATUS", 
                    value = if (room.isPlaying) "Playing" else "Paused", 
                    modifier = Modifier.weight(1f), 
                    highlight = room.isPlaying, 
                    highlightColor = Color(0xFF4CAF50)
                )
            }
        }

        item {
            val track = room.currentTrack
            Text(
                "SYNCED TRACK", 
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track?.thumbnail != null) {
                        coil.compose.AsyncImage(
                            model = track.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "No Synchronized Track", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold, 
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = track?.artist ?: "Host is not playing anything", 
                            style = MaterialTheme.typography.bodySmall, 
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                "LISTENERS", 
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(room.users) { user: UserInfo ->
            UserListItem(
                user = user, 
                isHost = user.userId == room.hostId, 
                isMe = user.userId == uiState.userId,
                accentColor = dominantColors.accent,
                onBlock = { onBlockUser(user.userId) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.role != RoomRole.HOST) {
                    Button(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Resync", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
                
                TextButton(
                    onClick = onLeaveRoom,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Session", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false, highlightColor: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        color = if (highlight) highlightColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = if (highlight) highlightColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
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
    accentColor: Color,
    onBlock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (isHost) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.username.take(1).uppercase(), 
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isHost) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .background(if (user.isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMe) "${user.username} (You)" else user.username, 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isMe || isHost) FontWeight.Bold else FontWeight.Medium
                )
                if (isHost) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), 
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "HOST", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                            color = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), 
                            fontSize = 8.sp
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = if (user.isConnected) "Synchronized" else "Connection lost", 
                style = MaterialTheme.typography.labelMedium, 
                color = if (user.isConnected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!isMe) {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
    )
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, dominantColors: DominantColors) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.85f)
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun RequestItem(request: JoinRequestPayload, onApprove: (String) -> Unit, onReject: (String) -> Unit, dominantColors: DominantColors) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(request.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(request.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onReject(request.userId) }, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(
                    onClick = { onApprove(request.userId) }, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = Color(0xFF4CAF50))
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
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Session Terminal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Real-time event tracking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                FilterChip(
                    selected = isLogActive,
                    onClick = { onLogActiveChange(!isLogActive) },
                    label = { Text(if (isLogActive) "Capturing" else "Paused") },
                    leadingIcon = { 
                        Box(modifier = Modifier.size(6.dp).background(if (isLogActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline, CircleShape)) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No events logged", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
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
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Logs")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
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
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.timestamp.split("T").lastOrNull()?.take(8) ?: entry.timestamp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = color.copy(alpha = 0.1f), 
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = entry.level.name, 
                    color = color, 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = entry.message, 
            color = MaterialTheme.colorScheme.onSurface, 
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Text(
                text = "Blocked Users", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "These users cannot join your rooms.", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (blockedUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No blocked users", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(blockedUsers.toList()) { userId ->
                            ListItem(
                                headlineContent = { Text(userId, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) },
                                trailingContent = {
                                    TextButton(onClick = { onUnblock(userId) }) {
                                        Text("Unblock")
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        }
    }
}
