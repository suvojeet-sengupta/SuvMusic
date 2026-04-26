package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.viewmodel.ImportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistScreen(
    isVisible: Boolean,
    importState: ImportState,
    onDismiss: () -> Unit,
    onImport: (url: String) -> Unit,
    onImportM3U: (android.net.Uri) -> Unit,
    onImportSUV: (android.net.Uri) -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    val canDismiss = importState !is ImportState.Loading && importState !is ImportState.Processing

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                if (canDismiss) {
                    TopAppBar(
                        title = {
                             Text("Import Playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                onReset()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = importState,
                    transitionSpec = {
                        fadeIn(tween(300)) + scaleIn(initialScale = 0.95f) togetherWith
                                fadeOut(tween(300)) + scaleOut(targetScale = 1.05f)
                    },
                    label = "ImportState"
                ) { state ->
                    when (state) {
                        is ImportState.Idle -> InputView(onImport, onImportM3U, onImportSUV)
                        is ImportState.Loading -> LoadingView(onCancel)
                        is ImportState.Processing -> ProcessingView(state, onCancel)
                        is ImportState.Success -> SuccessView(
                            state = state,
                            onDone = {
                                onReset()
                                onDismiss()
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "I just imported ${state.successCount} songs to SuvMusic! 🎵")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Import Stats")
                                context.startActivity(shareIntent)
                            }
                        )
                        is ImportState.Error -> ErrorView(state.message, onReset)
                    }
                }
            }
        }
    }
}

@Composable
private fun InputView(
    onImport: (String) -> Unit, 
    onImportM3U: (android.net.Uri) -> Unit,
    onImportSUV: (android.net.Uri) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    
    val m3uPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportM3U(uri)
        }
    }
    
    val suvPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportSUV(uri)
        }
    }

    // Auto-paste if valid link in clipboard (only once on appearing)
    LaunchedEffect(Unit) {
        val clipText = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
        if (clipText != null && isValidUrl(clipText) && url.isEmpty()) {
            url = clipText
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spotify Icon Placeholder
            Surface(
                modifier = Modifier.size(56.dp),
                shape = SquircleShape,
                color = Color(0xFF1DB954).copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote, 
                        contentDescription = "Spotify",
                        tint = Color(0xFF1DB954), 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // YT Music Icon Placeholder
            Surface(
                modifier = Modifier.size(56.dp),
                shape = SquircleShape,
                color = Color(0xFFFF0000).copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow, 
                        contentDescription = "YT Music",
                        tint = Color(0xFFFF0000), 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // File Icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = SquircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile, 
                        contentDescription = "Files",
                        tint = MaterialTheme.colorScheme.secondary, 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Transfer Your Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = "SuvMusic supports importing from Spotify, YouTube Music, and .m3u or .suv files.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Playlist Link") },
            placeholder = { Text("Spotify or YouTube Music URL") },
            singleLine = true,
            shape = SquircleShape,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = { url = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                } else {
                    IconButton(onClick = {
                        scope.launch {
                            val clipText = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
                            if (clipText != null) url = clipText
                        }
                    }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onImport(url) },
            enabled = isValidUrl(url),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = SquircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Continue with Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { m3uPicker.launch("*/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = SquircleShape
            ) {
                Text("Import .m3u", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            OutlinedButton(
                onClick = { suvPicker.launch("*/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = SquircleShape
            ) {
                Text("Import .suv", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun isValidUrl(url: String): Boolean {
    val cleanUrl = url.trim()
    return cleanUrl.contains("spotify.com/") || 
           cleanUrl.contains("spotify.link") ||
           cleanUrl.contains("youtube.com/") ||
           cleanUrl.contains("youtu.be/")
}

@Composable
private fun ProcessingView(state: ImportState.Processing, onCancel: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Song Artwork
        Card(
            shape = SquircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.size(180.dp)
        ) {
            if (state.thumbnail != null) {
                AsyncImage(
                    model = state.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Song Details
        AnimatedContent(targetState = state.currentSong, label = "SongTitle") { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(targetState = state.currentArtist, label = "ArtistName") { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Status & Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.status,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.status == "Not Found") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "${state.currentIndex} / ${state.totalSongs}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val animatedProgress by animateFloatAsState(
            targetValue = if (state.totalSongs > 0) state.currentIndex.toFloat() / state.totalSongs else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "import_progress"
        )
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = SquircleShape
        ) {
            Text("Cancel Import", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SuccessView(
    state: ImportState.Success, 
    onDone: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF00E676), // Success green
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "All Done!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Successfully imported ${state.successCount} of ${state.totalCount} songs.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (state.failedSongs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = SquircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Failed to match ${state.failedSongs.size} songs:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    state.failedSongs.take(5).forEach { (title, artist) ->
                        Text(
                            text = "• $title - $artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (state.failedSongs.size > 5) {
                        Text(
                            text = "and ${state.failedSongs.size - 5} more...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = SquircleShape
        ) {
             Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
             Spacer(modifier = Modifier.width(8.dp))
             Text("Share Statistics", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = SquircleShape
        ) {
            Text("Go to Library", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LoadingView(onCancel: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        LoadingIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fetching Playlist Information...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = SquircleShape
        ) {
            Text("Cancel", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Import Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = SquircleShape
        ) {
            Text("Try Again", fontWeight = FontWeight.ExtraBold)
        }
    }
}
