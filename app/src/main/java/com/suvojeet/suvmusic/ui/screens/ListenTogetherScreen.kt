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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Listen Together",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = dominantColors.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = if (uiState.isInRoom) "Connected Session" else "Sync with friends",
                        style = MaterialTheme.typography.bodySmall,
                        color = dominantColors.onBackground.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        "Close",
                        modifier = Modifier.size(20.dp),
                        tint = dominantColors.onBackground
                    )
                }
            }
            
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
    
    val showCredits = ListenTogetherClient.isMetroServer(serverUrl)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Hero Illustration placeholder or icon
        Box(
            modifier = Modifier
                .padding(vertical = 32.dp)
                .size(80.dp)
                .background(dominantColors.accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = dominantColors.accent,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = "Experience music in perfect sync.",
            style = MaterialTheme.typography.titleMedium,
            color = dominantColors.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Create a room and share your code to start listening together.",
            style = MaterialTheme.typography.bodyMedium,
            color = dominantColors.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Your Display Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = dominantColors.accent,
                unfocusedBorderColor = dominantColors.onBackground.copy(alpha = 0.2f),
                focusedTextColor = dominantColors.onBackground,
                unfocusedTextColor = dominantColors.onBackground,
                focusedLabelColor = dominantColors.accent,
                unfocusedLabelColor = dominantColors.onBackground.copy(alpha = 0.5f),
                cursorColor = dominantColors.accent,
                focusedLeadingIconColor = dominantColors.onBackground.copy(alpha = 0.7f),
                unfocusedLeadingIconColor = dominantColors.onBackground.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Host Card
        Card(
            onClick = onCreateRoom,
            enabled = username.isNotBlank() && connectionState != ConnectionState.CONNECTING && connectionState != ConnectionState.RECONNECTING,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = dominantColors.accent,
                contentColor = dominantColors.onBackground
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = dominantColors.onBackground, strokeWidth = 3.dp)
                } else {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Host New Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = dominantColors.onBackground.copy(alpha = 0.1f))
            Text(
                "OR JOIN", 
                style = MaterialTheme.typography.labelSmall,
                color = dominantColors.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 16.dp),
                letterSpacing = 1.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = dominantColors.onBackground.copy(alpha = 0.1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { if (it.length <= 8) roomCode = it.uppercase() },
                placeholder = { Text("Enter Room Code", color = dominantColors.onBackground.copy(alpha = 0.3f)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = dominantColors.accent,
                    unfocusedBorderColor = dominantColors.onBackground.copy(alpha = 0.2f),
                    focusedTextColor = dominantColors.onBackground,
                    unfocusedTextColor = dominantColors.onBackground,
                    cursorColor = dominantColors.accent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilledTonalButton(
                onClick = { onJoinRoom(roomCode) },
                enabled = username.isNotBlank() && roomCode.length >= 4 && connectionState != ConnectionState.CONNECTING && connectionState != ConnectionState.RECONNECTING,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                    contentColor = dominantColors.onBackground,
                    disabledContainerColor = dominantColors.onBackground.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = dominantColors.onBackground)
                } else {
                    Icon(Icons.Default.Login, null)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Server Settings Header
        Surface(
            onClick = { showSettings = !showSettings },
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings, 
                    contentDescription = null, 
                    tint = dominantColors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Advanced Settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = dominantColors.accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = showSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(dominantColors.onBackground.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "SERVER CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = dominantColors.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = dominantColors.accent,
                        unfocusedBorderColor = dominantColors.onBackground.copy(alpha = 0.2f),
                        focusedTextColor = dominantColors.onBackground,
                        unfocusedTextColor = dominantColors.onBackground,
                        focusedLeadingIconColor = dominantColors.onBackground.copy(alpha = 0.7f),
                        unfocusedLeadingIconColor = dominantColors.onBackground.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = dominantColors.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsToggle(
                    title = "Auto-approve guests",
                    subtitle = "Automatically accept join requests",
                    checked = autoApproval,
                    onCheckedChange = onAutoApprovalChange,
                    dominantColors = dominantColors
                )
                
                SettingsToggle(
                    title = "Sync host volume",
                    subtitle = "Match volume with the host",
                    checked = syncVolume,
                    onCheckedChange = onSyncVolumeChange,
                    dominantColors = dominantColors
                )
                
                SettingsToggle(
                    title = "Mute host audio",
                    subtitle = "Useful when listening in the same room",
                    checked = muteHost,
                    onCheckedChange = onMuteHostChange,
                    dominantColors = dominantColors
                )
                
                if (showCredits) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        onClick = { uriHandler.openUri("https://nyx.meowery.eu/") },
                        color = dominantColors.accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = dominantColors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Server by metroserver (nyx.meowery.eu)",
                                style = MaterialTheme.typography.labelMedium,
                                color = dominantColors.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = dominantColors.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = dominantColors.onBackground,
                checkedTrackColor = dominantColors.accent,
                uncheckedThumbColor = dominantColors.onBackground.copy(alpha = 0.6f),
                uncheckedTrackColor = dominantColors.onBackground.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
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
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Pending Requests (Host Only)
        if (uiState.role == RoomRole.HOST && pendingRequests.isNotEmpty()) {
            item {
                Surface(
                    color = dominantColors.accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pending Requests (${pendingRequests.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = dominantColors.accent,
                            modifier = Modifier.padding(bottom = 12.dp)
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

        // Room Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = dominantColors.onBackground.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SESSION CODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = dominantColors.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        onClick = { onCopyCode(room.roomCode) },
                        color = dominantColors.onBackground.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp
                                ),
                                color = dominantColors.onBackground
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.Default.ContentCopy, 
                                null, 
                                tint = dominantColors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Host Badge
                    val hostUser = room.users.find { it.userId == room.hostId }
                    Surface(
                        color = dominantColors.accent.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = dominantColors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hosted by ${hostUser?.username ?: "Unknown"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = dominantColors.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Stats Row
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "UPTIME",
                    value = durationString,
                    modifier = Modifier.weight(1f),
                    dominantColors = dominantColors
                )
                StatCard(
                    label = "ROLE",
                    value = if (uiState.role == RoomRole.HOST) "Host" else "Guest",
                    modifier = Modifier.weight(1f),
                    dominantColors = dominantColors,
                    highlight = uiState.role == RoomRole.HOST
                )
                StatCard(
                    label = "USERS",
                    value = "${room.users.size}",
                    modifier = Modifier.weight(1f),
                    dominantColors = dominantColors
                )
            }
        }

        // Now Playing Section
        item {
            Text(
                "NOW PLAYING",
                style = MaterialTheme.typography.labelSmall,
                color = dominantColors.onBackground.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            
            val track = room.currentTrack
            Card(
                colors = CardDefaults.cardColors(containerColor = dominantColors.onBackground.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track?.thumbnail != null) {
                        coil.compose.AsyncImage(
                            model = track.thumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(dominantColors.onBackground.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = dominantColors.onBackground.copy(alpha = 0.5f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        if (track != null) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = dominantColors.onBackground
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = dominantColors.onBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (room.isPlaying) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (room.isPlaying) "Live Syncing" else "Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (room.isPlaying) Color(0xFF4CAF50) else dominantColors.onBackground.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Waiting for Host...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = dominantColors.onBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Users List
        item {
            Text(
                "CONNECTED LISTENERS",
                style = MaterialTheme.typography.labelSmall,
                color = dominantColors.onBackground.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
        }

        items(room.users) { user ->
            UserItem(user, dominantColors)
        }

        // Action Buttons
        item {
            Spacer(modifier = Modifier.height(40.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (uiState.role != RoomRole.HOST) {
                    Button(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                            contentColor = dominantColors.onBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Resync with Host", fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = onLeaveRoom,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Leave Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    dominantColors: DominantColors,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = if (highlight) dominantColors.accent.copy(alpha = 0.15f) else dominantColors.onBackground.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = if (highlight) dominantColors.accent else dominantColors.onBackground,
                fontWeight = FontWeight.Black
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(dominantColors.onBackground.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = request.username.take(1).uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = dominantColors.onBackground
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = request.username,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { onReject(request.userId) },
                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(
                onClick = { onApprove(request.userId) },
                modifier = Modifier.size(32.dp).background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Check,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun UserItem(user: com.suvojeet.suvmusic.listentogether.UserInfo, dominantColors: DominantColors) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.isHost) dominantColors.accent.copy(alpha = 0.2f)
                        else dominantColors.onBackground.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = if (user.isHost) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f)
                )
                
                // Connection Indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .background(if (user.isConnected) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                        .border(2.dp, dominantColors.primary, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = dominantColors.onBackground
                    )
                    if (user.isHost) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = dominantColors.accent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "HOST",
                                style = MaterialTheme.typography.labelSmall,
                                color = dominantColors.accent,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = if (user.isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isConnected) dominantColors.onBackground.copy(alpha = 0.5f) 
                           else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
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
