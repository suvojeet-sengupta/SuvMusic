package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.data.YouTubeSyncSelection
import com.suvojeet.suvmusic.ui.viewmodel.YouTubeSyncViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeSyncScreen(
    onBack: () -> Unit,
    viewModel: YouTubeSyncViewModel = koinViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val excluded by viewModel.excludedPlaylists.collectAsStateWithLifecycle()
    val pullOnly by viewModel.pullOnly.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Music sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item(key = "pull_only") {
                SyncRow(
                    title = "YouTube Music to app only",
                    subtitle = "Nothing you do in the app is sent to YouTube Music, including likes, playlist edits and watch history. New playlists stay on this device.",
                    checked = pullOnly,
                    onCheckedChange = viewModel::setPullOnly
                )
                HorizontalDivider()
                Text(
                    text = "Playlists to sync",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Turned-off playlists are hidden from your library and stop updating. They stay on YouTube Music.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
            item(key = YouTubeSyncSelection.LIKED_ID) {
                SyncRow(
                    title = "Liked songs",
                    checked = YouTubeSyncSelection.isSynced(YouTubeSyncSelection.LIKED_ID, excluded),
                    onCheckedChange = { viewModel.setPlaylistSynced(YouTubeSyncSelection.LIKED_ID, it) }
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                SyncRow(
                    title = playlist.name,
                    subtitle = playlist.uploaderName.takeIf { it.isNotBlank() },
                    imageUrl = playlist.thumbnailUrl,
                    checked = YouTubeSyncSelection.isSynced(playlist.id, excluded),
                    onCheckedChange = { viewModel.setPlaylistSynced(playlist.id, it) }
                )
            }
            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    imageUrl: String? = null
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = subtitle?.let { text -> { Text(text) } },
        leadingContent = imageUrl?.let { url ->
            {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}
