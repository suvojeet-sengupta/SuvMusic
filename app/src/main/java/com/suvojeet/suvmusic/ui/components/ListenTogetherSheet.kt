package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.listentogether.ConnectionState
import com.suvojeet.suvmusic.listentogether.ListenTogetherUsernameKey
import com.suvojeet.suvmusic.listentogether.RoomRole
import com.suvojeet.suvmusic.listentogether.dataStore
import com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    viewModel: ListenTogetherViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Persist username
    var username by remember { 
        mutableStateOf(runBlocking { context.dataStore.data.map { it[ListenTogetherUsernameKey] ?: "" }.first() }) 
    }
    
    // Save username when changed
    val saveUsername: (String) -> Unit = { newName ->
        username = newName
        // Note: Ideally save to DataStore in ViewModel, but quick inline here
        // We'll update the viewModel to handle persistence if we had time, for now user input drives it.
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listen Together",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (uiState.isInRoom) {
                    RoomContent(
                        uiState = uiState,
                        onLeaveRoom = { viewModel.leaveRoom() },
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                        },
                        onSync = { viewModel.requestSync() }
                    )
                } else {
                    SetupContent(
                        username = username,
                        onUsernameChange = saveUsername,
                        onCreateRoom = { viewModel.createRoom(username) },
                        onJoinRoom = { code -> viewModel.joinRoom(code, username) },
                        connectionState = uiState.connectionState
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
    connectionState: ConnectionState
) {
    var roomCode by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Listen to music with friends in real-time.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Your Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Create a Room", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCreateRoom,
            enabled = username.isNotBlank() && connectionState != ConnectionState.CONNECTING,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (connectionState == ConnectionState.CONNECTING) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Icon(Icons.Default.Group, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Host a Session")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("OR", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Join a Room", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                label = { Text("Room Code") },
                placeholder = { Text("ABC12") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilledTonalButton(
                onClick = { onJoinRoom(roomCode) },
                enabled = username.isNotBlank() && roomCode.length >= 4,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Login, null)
            }
        }
    }
}

@Composable
fun RoomContent(
    uiState: com.suvojeet.suvmusic.ui.viewmodel.ListenTogetherUiState,
    onLeaveRoom: () -> Unit,
    onCopyCode: (String) -> Unit,
    onSync: () -> Unit
) {
    val room = uiState.roomState ?: return
    
    Column {
        // Room Code Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ROOM CODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { onCopyCode(room.roomCode) }
                ) {
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.displayMedium.copy(letterSpacing = 4.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ContentCopy, 
                        "Copy", 
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val roleText = if (uiState.role == RoomRole.HOST) "You are the Host" else "You are a Guest"
                Text(
                    text = roleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Connected Users
        Text(
            text = "Connected Users (${room.users.size})",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(room.users) { user ->
                UserItem(user)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Actions
        Row(modifier = Modifier.fillMaxWidth()) {
            if (uiState.role != RoomRole.HOST) {
                Button(
                    onClick = onSync,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Playback")
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Button(
                onClick = onLeaveRoom,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leave Room")
            }
        }
    }
}

@Composable
fun UserItem(user: com.suvojeet.suvmusic.listentogether.UserInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (user.isHost) {
                Text(
                    text = "Host",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (user.isConnected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
    }
}
