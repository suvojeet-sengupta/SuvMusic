package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.listentogether.ConnectionState
import com.suvojeet.suvmusic.listentogether.ListenTogetherClient
import com.suvojeet.suvmusic.listentogether.RoomRole
import com.suvojeet.suvmusic.listentogether.ListenTogetherAutoApprovalKey
import com.suvojeet.suvmusic.listentogether.ListenTogetherSyncVolumeKey
import com.suvojeet.suvmusic.listentogether.ListenTogetherMuteHostKey
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel

@Composable
fun ListenTogetherScreen(
    onDismiss: () -> Unit,
    dominantColors: DominantColors,
    viewModel: ListenTogetherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    
    val savedUsername by viewModel.savedUsername.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    
    // Settings preferences
    val autoApproval by viewModel.autoApproval.collectAsState()
    val syncVolume by viewModel.syncVolume.collectAsState()
    val muteHost by viewModel.muteHost.collectAsState()
    
    // Local state for username input to avoid stutter, sync with saved on init
    var username by remember { mutableStateOf("") }
    
    // Sync when saved username loads
    androidx.compose.runtime.LaunchedEffect(savedUsername) {
        if (username.isEmpty() && savedUsername.isNotEmpty()) {
            username = savedUsername
        }
    }
    
    // Save username when changed
    val saveUsername: (String) -> Unit = { newName ->
        username = newName
        viewModel.updateSavedUsername(newName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.secondary,
                        dominantColors.primary
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Listen Together",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = dominantColors.onBackground
                    )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        "Close",
                        tint = dominantColors.onBackground
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isInRoom) {
                    RoomContent(
                        uiState = uiState,
                        onLeaveRoom = { viewModel.leaveRoom() },
                        onCopyCode = { code ->
                            scope.launch {
                                clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("Room Code", code)))
                            }
                        },
                        onSync = { viewModel.requestSync() },
                        viewModel = viewModel,
                        dominantColors = dominantColors
                    )
                } else {
                    SetupContent(
                        username = username,
                        onUsernameChange = saveUsername,
                        onCreateRoom = { viewModel.createRoom(username) },
                        onJoinRoom = { code -> viewModel.joinRoom(code, username) },
                        connectionState = uiState.connectionState,
                        serverUrl = serverUrl,
                        onServerUrlChange = { viewModel.updateServerUrl(it) },
                        autoApproval = autoApproval,
                        onAutoApprovalChange = { viewModel.updateAutoApproval(it) },
                        syncVolume = syncVolume,
                        onSyncVolumeChange = { viewModel.updateSyncVolume(it) },
                        muteHost = muteHost,
                        onMuteHostChange = { viewModel.updateMuteHost(it) },
                        dominantColors = dominantColors
                    )
                }
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
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    autoApproval: Boolean,
    onAutoApprovalChange: (Boolean) -> Unit,
    syncVolume: Boolean,
    onSyncVolumeChange: (Boolean) -> Unit,
    muteHost: Boolean,
    onMuteHostChange: (Boolean) -> Unit,
    dominantColors: DominantColors
) {
    var roomCode by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    
    // Default server check for credits
    val showCredits = ListenTogetherClient.isMetroServer(serverUrl)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Listen to music with friends in real-time.",
            style = MaterialTheme.typography.bodyLarge,
            color = dominantColors.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Your Name", color = dominantColors.onBackground.copy(alpha = 0.7f)) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = dominantColors.onBackground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = dominantColors.accent,
                unfocusedBorderColor = dominantColors.onBackground.copy(alpha = 0.3f),
                focusedTextColor = dominantColors.onBackground,
                unfocusedTextColor = dominantColors.onBackground
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(color = dominantColors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Create a Room", 
            style = MaterialTheme.typography.titleMedium,
            color = dominantColors.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCreateRoom,
            enabled = username.isNotBlank() && connectionState != ConnectionState.CONNECTING,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = dominantColors.accent,
                contentColor = dominantColors.onBackground,
                disabledContainerColor = dominantColors.accent.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (connectionState == ConnectionState.CONNECTING) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = dominantColors.onBackground)
            } else {
                Icon(Icons.Default.Group, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Host a Session", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "OR", 
            style = MaterialTheme.typography.labelLarge,
            color = dominantColors.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Join a Room", 
            style = MaterialTheme.typography.titleMedium,
            color = dominantColors.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { if (it.length <= 8) roomCode = it.uppercase() },
                label = { Text("Room Code", color = dominantColors.onBackground.copy(alpha = 0.7f)) },
                placeholder = { Text("QSVF5H5P", color = dominantColors.onBackground.copy(alpha = 0.3f)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = dominantColors.accent,
                    unfocusedBorderColor = dominantColors.onBackground.copy(alpha = 0.3f),
                    focusedTextColor = dominantColors.onBackground,
                    unfocusedTextColor = dominantColors.onBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilledTonalButton(
                onClick = { onJoinRoom(roomCode) },
                enabled = username.isNotBlank() && roomCode.length >= 4 && connectionState != ConnectionState.CONNECTING,
                modifier = Modifier.height(64.dp), // Match height of text field approx
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                    contentColor = dominantColors.onBackground,
                    disabledContainerColor = dominantColors.onBackground.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = dominantColors.onBackground)
                } else {
                    Icon(Icons.Default.Login, null)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = dominantColors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // Server Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSettings = !showSettings }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Settings, 
                contentDescription = null, 
                tint = dominantColors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Server Settings",
                style = MaterialTheme.typography.labelLarge,
                color = dominantColors.accent
            )
        }

        AnimatedVisibility(visible = showSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(dominantColors.onBackground.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Server URL",
                    style = MaterialTheme.typography.labelMedium,
                    color = dominantColors.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = dominantColors.accent,
                        unfocusedBorderColor = dominantColors.onBackground.copy(alpha = 0.3f),
                        focusedTextColor = dominantColors.onBackground,
                        unfocusedTextColor = dominantColors.onBackground
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = dominantColors.onBackground.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                
                // Auto-approval toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-approve join requests",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground
                        )
                        Text(
                            text = "Automatically approve join requests when you're the host",
                            style = MaterialTheme.typography.bodySmall,
                            color = dominantColors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = autoApproval,
                        onCheckedChange = onAutoApprovalChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = dominantColors.accent,
                            checkedTrackColor = dominantColors.accent.copy(alpha = 0.5f),
                            uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.6f),
                            uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.2f)
                        )
                    )
                }
                
                // Sync volume toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync host volume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground
                        )
                        Text(
                            text = "Sync your volume with the host's volume as a guest",
                            style = MaterialTheme.typography.bodySmall,
                            color = dominantColors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = syncVolume,
                        onCheckedChange = onSyncVolumeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = dominantColors.accent,
                            checkedTrackColor = dominantColors.accent.copy(alpha = 0.5f),
                            uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.6f),
                            uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.2f)
                        )
                    )
                }
                
                // Mute host toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mute host audio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground
                        )
                        Text(
                            text = "Mute the audio when you're a guest",
                            style = MaterialTheme.typography.bodySmall,
                            color = dominantColors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = muteHost,
                        onCheckedChange = onMuteHostChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = dominantColors.accent,
                            checkedTrackColor = dominantColors.accent.copy(alpha = 0.5f),
                            uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.6f),
                            uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.2f)
                        )
                    )
                }
                
                if (showCredits) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://nyx.meowery.eu/") }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = dominantColors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server provided by metroserver (nyx.meowery.eu)",
                            style = MaterialTheme.typography.labelSmall,
                            color = dominantColors.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomContent(
    uiState: com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherUiState,
    onLeaveRoom: () -> Unit,
    onCopyCode: (String) -> Unit,
    onSync: () -> Unit,
    viewModel: com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel,
    dominantColors: DominantColors
) {
    val room = uiState.roomState ?: return
    val pendingRequests by viewModel.pendingJoinRequests.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Pending Requests (Host Only)
        if (uiState.role == RoomRole.HOST && pendingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Join Requests (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = dominantColors.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            items(pendingRequests) { request ->
                RequestItem(
                    request = request,
                    onApprove = { viewModel.approveJoin(it) },
                    onReject = { viewModel.rejectJoin(it) },
                    dominantColors = dominantColors
                )
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = dominantColors.onBackground.copy(alpha = 0.1f))
            }
        }

        // Room Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = dominantColors.onBackground.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ROOM CODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                onCopyCode(room.roomCode) 
                            }
                            .background(dominantColors.onBackground.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            color = dominantColors.onBackground
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.ContentCopy, 
                            "Copy", 
                            tint = dominantColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Host Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(dominantColors.accent)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = dominantColors.onBackground,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val hostUser = room.users.find { it.userId == room.hostId }
                        Text(
                            text = "Hosted by ${hostUser?.username ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground
                        )
                    }
                }
            }
        }

        // Session Info Card
        item {
            var sessionDuration by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            
            androidx.compose.runtime.LaunchedEffect(Unit) {
                while (true) {
                    sessionDuration = viewModel.getSessionDuration()
                    kotlinx.coroutines.delay(1000)
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
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = dominantColors.onBackground.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Session Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = dominantColors.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = durationString,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = dominantColors.onBackground
                        )
                    }
                    
                    // Role
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ROLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = dominantColors.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Container(
                            color = if (uiState.role == RoomRole.HOST) dominantColors.accent.copy(alpha = 0.2f) else dominantColors.onBackground.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (uiState.role == RoomRole.HOST) "HOST" else "GUEST",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.role == RoomRole.HOST) dominantColors.accent else dominantColors.onBackground,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    // Connection
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = dominantColors.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = dominantColors.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Now Playing Status
        item {
            val track = room.currentTrack
            if (track != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = dominantColors.onBackground.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (track.thumbnail != null) {
                            coil.compose.AsyncImage(
                                model = track.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(dominantColors.accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, null, tint = dominantColors.accent)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (room.isPlaying) {
                                    Icon(
                                        Icons.Filled.PlayArrow, 
                                        null, 
                                        modifier = Modifier.size(16.dp),
                                        tint = dominantColors.accent
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Now Playing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = dominantColors.accent
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Pause, 
                                        null, 
                                        modifier = Modifier.size(16.dp),
                                        tint = dominantColors.onBackground.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Paused",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = dominantColors.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = dominantColors.onBackground
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = dominantColors.onBackground.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = dominantColors.onBackground.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Waiting for music...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dominantColors.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Users Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connected Users",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = dominantColors.onBackground
                )
                Container(
                    color = dominantColors.onBackground.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${room.users.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = dominantColors.onBackground,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // User List
        items(room.users) { user ->
            UserItem(user, dominantColors)
        }

        // Buttons
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.role != RoomRole.HOST) {
                    FilledTonalButton(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                            contentColor = dominantColors.onBackground
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Sync")
                    }
                }
                
                Button(
                    onClick = onLeaveRoom,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Room")
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: com.suvojeet.suvmusic.listentogether.JoinRequestPayload,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    dominantColors: DominantColors
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = dominantColors.onBackground.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = dominantColors.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = request.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = dominantColors.onBackground
                )
            }
            
            Row {
                IconButton(
                    onClick = { onReject(request.userId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(
                    onClick = { onApprove(request.userId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Approve",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun UserItem(user: com.suvojeet.suvmusic.listentogether.UserInfo, dominantColors: DominantColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (user.isHost) dominantColors.accent 
                    else dominantColors.onBackground.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (user.isHost) dominantColors.onBackground 
                       else dominantColors.onBackground
            )
            
            // Online Status Dot
            if (user.isConnected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF4CAF50), CircleShape)
                        .border(2.dp, dominantColors.primary, CircleShape) // Use primary bg for border
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = dominantColors.onBackground
                )
                if (user.isHost) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Container(
                        color = dominantColors.accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "HOST",
                            style = MaterialTheme.typography.labelSmall,
                            color = dominantColors.accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Text(
                text = if (user.isConnected) "Online" else "Disconnected",
                style = MaterialTheme.typography.bodySmall,
                color = if (user.isConnected) dominantColors.onBackground.copy(alpha = 0.6f) 
                       else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun Container(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(color, shape)
            .clip(shape)
    ) {
        content()
    }
}
