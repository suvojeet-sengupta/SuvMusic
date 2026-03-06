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
import com.suvojeet.suvmusic.listentogether.*
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.MeshGradientBackground
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.BorderStroke

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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Listen Together",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Surface(
                    color = when (connectionState) {
                        ConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
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
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
        
        FilledIconButton(
            onClick = onDismiss,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Close, "Close", modifier = Modifier.size(24.dp))
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
        val icon = if (connectionState == ConnectionState.ERROR) Icons.Default.WifiOff else Icons.Default.Wifi
        
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(32.dp),
            color = if (connectionState == ConnectionState.ERROR) MaterialTheme.colorScheme.errorContainer else dominantColors.accent.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (connectionState == ConnectionState.ERROR) MaterialTheme.colorScheme.error else dominantColors.accent
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Connect to Session",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Sync music with friends in real-time. Connect to the session server to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 40.dp)
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server URL") },
            leadingIcon = { Icon(Icons.Default.Link, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = dominantColors.accent,
                focusedLabelColor = dominantColors.accent
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = connectionState != ConnectionState.CONNECTING,
            colors = ButtonDefaults.buttonColors(containerColor = dominantColors.accent)
        ) {
            if (connectionState == ConnectionState.CONNECTING) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Connect to Server", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
            .padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WHO ARE YOU?",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = { Text("Enter your name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .clickable(enabled = username.isNotBlank()) { onCreateRoom() },
                shape = RoundedCornerShape(28.dp),
                color = if (username.isNotBlank()) dominantColors.accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Add, 
                        null, 
                        modifier = Modifier.size(32.dp),
                        tint = if (username.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "HOST ROOM",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = if (username.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(140.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { if (it.length <= 8) roomCode = it.uppercase() },
                        placeholder = { Text("CODE", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black, 
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    IconButton(
                        onClick = { onJoinRoom(roomCode) },
                        enabled = username.isNotBlank() && roomCode.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                if (username.isNotBlank() && roomCode.length >= 4) dominantColors.accent else Color.Gray.copy(alpha = 0.2f), 
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Login, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("JOIN", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSettings() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Connection Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Icon(if (showSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }

        AnimatedVisibility(visible = showSettings) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingsToggleItem(
                        title = "Auto-approve guests",
                        subtitle = "Accept requests automatically",
                        checked = autoApproval,
                        onCheckedChange = onAutoApprovalChange,
                        dominantColors = dominantColors
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsToggleItem(
                        title = "Sync host volume",
                        subtitle = "Match volume with host",
                        checked = syncVolume,
                        onCheckedChange = onSyncVolumeChange,
                        dominantColors = dominantColors
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsToggleItem(
                        title = "Mute host audio",
                        subtitle = "Useful for local syncing",
                        checked = muteHost,
                        onCheckedChange = onMuteHostChange,
                        dominantColors = dominantColors
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = onServerUrlChange,
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onToggleLogs,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Real-time Logs")
                            }
                            
                            FilledTonalButton(
                                onClick = onToggleBlocked,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Blocked (${blockedUsers.size})")
                            }
                        }
                        
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f), 
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
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
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (uiState.role == RoomRole.HOST && pendingRequests.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Pending Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.tertiary
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

        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(36.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SESSION CODE", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 8.sp
                        ),
                        modifier = Modifier.clickable { onCopyCode(room.roomCode) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    val hostUser = room.users.find { it.userId == room.hostId }
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Hosted by ${hostUser?.username ?: "..."}") },
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(label = "UPTIME", value = durationString, modifier = Modifier.weight(1f))
                StatItem(label = "LISTENERS", value = "${room.users.size}", modifier = Modifier.weight(1f))
                StatItem(label = "STATUS", value = if (room.isPlaying) "Playing" else "Paused", modifier = Modifier.weight(1f), highlight = room.isPlaying, highlightColor = Color(0xFF4CAF50))
            }
        }

        item {
            val track = room.currentTrack
            Text(
                "CURRENTLY SYNCED", 
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track?.thumbnail != null) {
                        coil.compose.AsyncImage(
                            model = track.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Waiting for Host...", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold, 
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = track?.artist ?: "No track in sync", 
                            style = MaterialTheme.typography.bodyMedium, 
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        if (track != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = if (room.isPlaying) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                                shape = CircleShape
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(if (room.isPlaying) Color(0xFF4CAF50) else Color.Gray, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (room.isPlaying) "Synced" else "Paused", 
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (room.isPlaying) Color(0xFF4CAF50) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "LISTENERS", 
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
            Spacer(modifier = Modifier.height(40.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (uiState.role != RoomRole.HOST) {
                    Button(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Resync with Host", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                
                Button(
                    onClick = onLeaveRoom,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f), 
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Leave Session", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false, highlightColor: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        color = if (highlight) highlightColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, (if (highlight) highlightColor else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (highlight) highlightColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = if (highlight) highlightColor else MaterialTheme.colorScheme.onSurface)
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
    
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isHost) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(), 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isHost) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(if (user.isConnected) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMe) "${user.username} (You)" else user.username, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (isHost) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "HOST", 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), 
                                color = accentColor, 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Text(
                    text = if (user.isConnected) "Synchronized" else "Disconnected", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            if (!isMe) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            onClick = { 
                                onBlock()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)) },
                            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, dominantColors: DominantColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }, 
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, 
                checkedTrackColor = dominantColors.accent,
                uncheckedBorderColor = Color.Transparent,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun RequestItem(request: JoinRequestPayload, onApprove: (String) -> Unit, onReject: (String) -> Unit, dominantColors: DominantColors) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = dominantColors.accent.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(request.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = dominantColors.accent)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(request.username, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onReject(request.userId) }, 
                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape).size(40.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(
                    onClick = { onApprove(request.userId) }, 
                    modifier = Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape).size(40.dp)
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
        containerColor = Color(0xFF0A0A0F),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.3f)) },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Session Terminal", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
                    Text("Real-time event tracking", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
                
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { onLogActiveChange(!isLogActive) }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(if (isLogActive) Color(0xFF4CAF50) else Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLogActive) "Capturing" else "Paused", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = Color.Black,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(48.dp), tint = Color.DarkGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No events logged yet", color = Color.DarkGray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        reverseLayout = true
                    ) {
                        items(logs.reversed()) { entry ->
                            LogItemRow(entry)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Console")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = dominantColors.accent)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun LogItemRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFEF5350)
        LogLevel.WARNING -> Color(0xFFFFB74D)
        LogLevel.INFO -> Color(0xFF81C784)
        LogLevel.DEBUG -> Color(0xFF64B5F6)
    }
    
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.timestamp.split("T").lastOrNull()?.take(8) ?: entry.timestamp, 
                color = Color.Gray, 
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = entry.level.name, 
                    color = color, 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = entry.message, 
            color = Color.White.copy(alpha = 0.9f), 
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        if (entry.details != null) {
            Text(
                text = "> ${entry.details}", 
                color = Color.Gray, 
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Text(
                text = "Blocked Users", 
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
            )
            Text(
                text = "Users in this list cannot request to join your rooms.", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                if (blockedUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No blocked users", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        items(blockedUsers.toList()) { userId ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("User ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    Text(userId, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                                }
                                Button(
                                    onClick = { onUnblock(userId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Text("Unblock")
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 40.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Close")
            }
        }
    }
}
