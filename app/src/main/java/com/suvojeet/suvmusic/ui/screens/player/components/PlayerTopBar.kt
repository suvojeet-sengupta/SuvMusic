package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.DominantColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerTopBar(
    onDismiss: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreClick: () -> Unit,
    dominantColors: DominantColors,
    audioArEnabled: Boolean = false,
    onRecenter: () -> Unit = {},
    title: String = "Now Playing",
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = dominantColors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.KeyboardArrowDown, "Dismiss", modifier = Modifier.size(32.dp), tint = dominantColors.onBackground)
            }
        },
        actions = {
            if (audioArEnabled) {
                IconButton(onClick = onRecenter) {
                    Icon(Icons.Filled.MyLocation, "Recenter AR", tint = dominantColors.onBackground)
                }
            }
            IconButton(onClick = onQueueClick) {
                Icon(Icons.Filled.QueueMusic, "Queue", tint = dominantColors.onBackground)
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Filled.MoreVert, "More options", tint = dominantColors.onBackground)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
    )
}
