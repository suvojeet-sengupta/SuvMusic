package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.ImportResult
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.viewmodel.ImportState

// Library UI Colors (Apple Music Dark Theme)
private val DarkBackground = Color(0xFF1C1C1E)
private val DarkSurface = Color(0xFF2C2C2E)
private val AccentRed = Color(0xFFFA2D48)
private val AccentGradient = listOf(Color(0xFFFA2D48), Color(0xFFFF6B6B))
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.5f)

/**
 * Bottom sheet to add a song to an existing playlist or create a new one.
 * Styled to match Apple Music dark theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    song: Song,
    isVisible: Boolean,
    playlists: List<PlaylistDisplayItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: (playlistId: String) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = DarkBackground,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
                
                // Song info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                // Create new playlist button with gradient
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onCreateNewPlaylist() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(AccentGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "New Playlist",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Create a new playlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Playlists list
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AccentRed)
                    }
                } else if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists found. Create one!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "YOUR PLAYLISTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddToPlaylist(playlist.getPlaylistId()) }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                ) {
                                    if (!playlist.thumbnailUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = playlist.thumbnailUrl,
                                            contentDescription = playlist.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(24.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.songCount} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen dialog to create a new playlist on YouTube Music.
 * Supports light/dark mode with MaterialTheme colors.
 */
/**
 * Full-screen dialog to create a new playlist on YouTube Music.
 * Supports light/dark mode with MaterialTheme colors and premium styling.
 */
@Composable
fun CreatePlaylistDialog(
    isVisible: Boolean,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, isPrivate: Boolean, syncWithYt: Boolean) -> Unit,
    isLoggedIn: Boolean = true
) {
    if (isVisible) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var syncWithYt by remember { mutableStateOf(isLoggedIn) }
        var isPrivate by remember { mutableStateOf(true) }
        
        // Theme-aware colors
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val backgroundColor = MaterialTheme.colorScheme.surface
        val primaryColor = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface
        val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
        
        Dialog(
            onDismissRequest = { if (!isCreating) onDismiss() },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background) // Main background
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.IconButton(
                            onClick = { if (!isCreating) onDismiss() }
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = onSurface)
                        }
                        
                        Text(
                            text = "New Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        
                        // Create Action
                        TextButton(
                            onClick = { 
                                if (title.isNotBlank()) {
                                    onCreate(title, description, isPrivate, syncWithYt)
                                }
                            },
                            enabled = title.isNotBlank() && !isCreating,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = primaryColor,
                                disabledContentColor = secondaryText.copy(alpha = 0.4f)
                            )
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = primaryColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Artwork Placeholder
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = secondaryText.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Inputs
                        androidx.compose.material3.OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            enabled = !isCreating,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        androidx.compose.material3.OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            minLines = 2,
                            maxLines = 4,
                            enabled = !isCreating,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Options Section
                        Text(
                            text = "OPTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Sync Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isCreating && isLoggedIn) { syncWithYt = !syncWithYt }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sync with YouTube Music", style = MaterialTheme.typography.bodyMedium, color = onSurface)
                                        if (!isLoggedIn) {
                                            Text("Login required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    androidx.compose.material3.Switch(
                                        checked = syncWithYt,
                                        onCheckedChange = { syncWithYt = it },
                                        enabled = !isCreating && isLoggedIn
                                    )
                                }
                                
                                if (syncWithYt) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    // Privacy Toggle (Only for YT playlists)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isCreating) { isPrivate = !isPrivate }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Private Playlist", style = MaterialTheme.typography.bodyMedium, color = onSurface)
                                            Text("Only you can view this", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                                        }
                                        androidx.compose.material3.Switch(
                                            checked = isPrivate,
                                            onCheckedChange = { isPrivate = it },
                                            enabled = !isCreating
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to rename a playlist.
 * Styled to match Apple Music dark theme.
 */
@Composable
fun RenamePlaylistDialog(
    isVisible: Boolean,
    currentName: String,
    isRenaming: Boolean,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    if (isVisible) {
        var title by remember(currentName) { mutableStateOf(currentName) }
        
        Dialog(
            onDismissRequest = { if (!isRenaming) onDismiss() }
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rename Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Title Input
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        enabled = !isRenaming,
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isRenaming
                        ) {
                            Text(
                                "Cancel",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Button(
                            onClick = { 
                                if (title.isNotBlank() && title != currentName) {
                                    onRename(title)
                                } else {
                                    onDismiss()
                                }
                            },
                            enabled = title.isNotBlank() && !isRenaming,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentRed,
                                disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isRenaming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                if (isRenaming) "Renaming..." else "Rename",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Dialog to delete a playlist.
 * Styled to match Apple Music dark theme.
 */
@Composable
fun DeletePlaylistDialog(
    isVisible: Boolean,
    playlistTitle: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { if (!isDeleting) onDismiss() }
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentRed,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Delete Playlist?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Are you sure you want to delete \"$playlistTitle\"? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isDeleting
                        ) {
                            Text(
                                "Cancel", 
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Button(
                            onClick = onDelete,
                            enabled = !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentRed,
                                disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Delete", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
