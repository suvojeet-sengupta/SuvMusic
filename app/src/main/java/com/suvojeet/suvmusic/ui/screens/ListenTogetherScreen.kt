package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.suvojeet.suvmusic.listentogether.ConnectionState
import com.suvojeet.suvmusic.listentogether.RoomRole
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast

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
                // Header
                ListenTogetherHeader(onDismiss = onDismiss, connectionState = connectionState)

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = when {
                            uiState.isInRoom -> "room"
                            connectionState == ConnectionState.CONNECTED -> "setup"
                            else -> "connect"
                        },
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + 
                             slideInVertically { it / 2 })
                                .togetherWith(fadeOut(animationSpec = spring()) + 
                                             slideOutVertically { -it / 2 })
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
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Listen Together",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800)
                                else -> Color.Gray
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Default.Close, "Close")
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
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (connectionState == ConnectionState.ERROR) Icons.Default.WifiOff else Icons.Default.Wifi,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (connectionState == ConnectionState.ERROR) MaterialTheme.colorScheme.error else dominantColors.accent
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Connect to Session Server",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "You need to be connected to the server to create or join rooms.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server URL") },
            leadingIcon = { Icon(Icons.Default.Link, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = dominantColors.accent,
                focusedLabelColor = dominantColors.accent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = connectionState != ConnectionState.CONNECTING,
            colors = ButtonDefaults.buttonColors(containerColor = dominantColors.accent)
        ) {
            if (connectionState == ConnectionState.CONNECTING) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Connect Now", fontWeight = FontWeight.Bold)
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
    logs: List<com.suvojeet.suvmusic.listentogether.LogEntry>,
    onClearLogs: () -> Unit,
    dominantColors: DominantColors
) {
    var roomCode by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Identity Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Your Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Section
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCreateRoom,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = dominantColors.accent)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Host", fontWeight = FontWeight.ExtraBold)
            }

            Surface(
                modifier = Modifier.weight(1.2f).height(64.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { if (it.length <= 8) roomCode = it.uppercase() },
                        placeholder = { Text("CODE", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = { onJoinRoom(roomCode) },
                        enabled = username.isNotBlank() && roomCode.length >= 4,
                        modifier = Modifier.size(40.dp).background(dominantColors.accent, CircleShape)
                    ) {
                        Icon(Icons.Default.Login, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Settings Section
        SettingsSection(
            showSettings = showSettings,
            onToggleSettings = { showSettings = !showSettings },
            showLogs = showLogs,
            onToggleLogs = { showLogs = !showLogs },
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
            dominantColors = dominantColors
        )
    }
}

@Composable
fun SettingsSection(
    showSettings: Boolean,
    onToggleSettings: () -> Unit,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
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
    logs: List<com.suvojeet.suvmusic.listentogether.LogEntry>,
    onClearLogs: () -> Unit,
    dominantColors: DominantColors
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Connection Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onToggleSettings) {
                Icon(if (showSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
        }

        AnimatedVisibility(visible = showSettings) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleItem(
                        title = "Auto-approve guests",
                        subtitle = "Accept requests automatically",
                        checked = autoApproval,
                        onCheckedChange = onAutoApprovalChange,
                        dominantColors = dominantColors
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    SettingsToggleItem(
                        title = "Sync host volume",
                        subtitle = "Match volume with host",
                        checked = syncVolume,
                        onCheckedChange = onSyncVolumeChange,
                        dominantColors = dominantColors
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onToggleLogs,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Logs")
                        }
                        
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Disconnect")
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
    }
}

@Composable
fun RoomContent(
    uiState: com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherUiState,
    onLeaveRoom: () -> Unit,
    onCopyCode: (String) -> Unit,
    onSync: () -> Unit,
    viewModel: ListenTogetherViewModel,
    dominantColors: DominantColors
) {
    val room = uiState.roomState ?: return
    val pendingRequests by viewModel.pendingJoinRequests.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Pending Requests (Host Only)
        if (uiState.role == RoomRole.HOST && pendingRequests.isNotEmpty()) {
            item {
                Surface(
                    color = dominantColors.accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Pending Requests (${pendingRequests.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = dominantColors.accent,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        
                        pendingRequests.forEach { request ->
                            RequestItem(
                                request = request,
                                onApprove = { viewModel.approveJoin(it) },
                                onReject = { viewModel.rejectJoin(it) },
                                dominantColors = dominantColors
                            )
                        }
                    }
                }
            }
        }

        // Room Info Section
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SESSION CODE", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 4.sp),
                        modifier = Modifier.clickable { onCopyCode(room.roomCode) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val hostUser = room.users.find { it.userId == room.hostId }
                    AssistChip(
                        onClick = {},
                        label = { Text("Hosted by ${hostUser?.username ?: "..."}") },
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(label = "UPTIME", value = durationString, modifier = Modifier.weight(1f))
                StatItem(label = "USERS", value = "${room.users.size}", modifier = Modifier.weight(1f))
                StatItem(label = "ROLE", value = if (uiState.role == RoomRole.HOST) "Host" else "Guest", modifier = Modifier.weight(1f), highlight = uiState.role == RoomRole.HOST, highlightColor = dominantColors.accent)
            }
        }

        // Now Playing Section
        item {
            val track = room.currentTrack
            Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track?.thumbnail != null) {
                        coil.compose.AsyncImage(
                            model = track.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track?.title ?: "Waiting...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(track?.artist ?: "Host is choosing a song", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        if (track != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(if (room.isPlaying) Color(0xFF4CAF50) else Color.Gray, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (room.isPlaying) "Syncing" else "Paused", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Listeners List
        item {
            Text("LISTENERS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp))
        }

        items(room.users) { user ->
            UserListItem(user, isHost = user.userId == room.hostId, accentColor = dominantColors.accent)
        }

        // Action Buttons
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.role != RoomRole.HOST) {
                    FilledTonalButton(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resync with Host")
                    }
                }
                
                Button(
                    onClick = onLeaveRoom,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Session")
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false, highlightColor: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        color = if (highlight) highlightColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (highlight) highlightColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (highlight) highlightColor else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun UserListItem(user: com.suvojeet.suvmusic.listentogether.UserInfo, isHost: Boolean, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(if (isHost) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(user.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (isHost) accentColor else MaterialTheme.colorScheme.onSurface)
            Box(modifier = Modifier.size(10.dp).align(Alignment.BottomEnd).background(if (user.isConnected) Color(0xFF4CAF50) else Color.Gray, CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (isHost) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("HOST", style = MaterialTheme.typography.labelSmall, color = accentColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(if (user.isConnected) "Online" else "Disconnected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, dominantColors: DominantColors) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = dominantColors.accent)
        )
    }
}

@Composable
fun RequestItem(request: com.suvojeet.suvmusic.listentogether.JoinRequestPayload, onApprove: (String) -> Unit, onReject: (String) -> Unit, dominantColors: DominantColors) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(request.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onReject(request.userId) }, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape).size(36.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
            IconButton(onClick = { onApprove(request.userId) }, modifier = Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape).size(36.dp)) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = Color(0xFF4CAF50))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewSheet(
    logs: List<com.suvojeet.suvmusic.listentogether.LogEntry>,
    onDismiss: () -> Unit,
    isLogActive: Boolean,
    onLogActiveChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    dominantColors: DominantColors
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Connection Logs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Capture", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isLogActive, onCheckedChange = onLogActiveChange)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No logs yet", color = Color.DarkGray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        items(logs.reversed()) { entry ->
                            LogItemRow(entry)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Clear Logs")
            }
        }
    }
}

@Composable
fun LogItemRow(entry: com.suvojeet.suvmusic.listentogether.LogEntry) {
    val color = when (entry.level) {
        com.suvojeet.suvmusic.listentogether.LogLevel.ERROR -> Color(0xFFEF5350)
        com.suvojeet.suvmusic.listentogether.LogLevel.WARNING -> Color(0xFFFFB74D)
        com.suvojeet.suvmusic.listentogether.LogLevel.INFO -> Color(0xFF81C784)
        com.suvojeet.suvmusic.listentogether.LogLevel.DEBUG -> Color(0xFF64B5F6)
    }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row {
            Text("[${entry.timestamp}] ", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(entry.level.name, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Text(entry.message, color = Color.White, style = MaterialTheme.typography.bodySmall)
        if (entry.details != null) {
            Text(entry.details, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}
