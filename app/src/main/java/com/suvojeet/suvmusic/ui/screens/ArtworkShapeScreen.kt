package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.ui.screens.player.components.ArtworkShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Artwork Shape selection screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkShapeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val artworkShapeString by sessionManager.artworkShapeFlow.collectAsState(initial = "ROUNDED_SQUARE")
    
    val currentArtworkShape = try {
        ArtworkShape.valueOf(artworkShapeString)
    } catch (e: Exception) {
        ArtworkShape.ROUNDED_SQUARE
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Artwork Shape") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose how album artwork appears on the player screen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Display artwork shapes in a grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArtworkShape.entries.take(2).forEach { shape ->
                    ArtworkShapeCard(
                        shape = shape,
                        isSelected = shape == currentArtworkShape,
                        primaryColor = primaryColor,
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                sessionManager.setArtworkShape(shape.name)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArtworkShape.entries.drop(2).forEach { shape ->
                    ArtworkShapeCard(
                        shape = shape,
                        isSelected = shape == currentArtworkShape,
                        primaryColor = primaryColor,
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                sessionManager.setArtworkShape(shape.name)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text
            Text(
                text = "You can also change this from the player by long-pressing on the artwork.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun ArtworkShapeCard(
    shape: ArtworkShape,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "border"
    )
    
    val (shapeName, icon) = when (shape) {
        ArtworkShape.ROUNDED_SQUARE -> "Rounded" to Icons.Rounded.RoundedCorner
        ArtworkShape.CIRCLE -> "Circle" to Icons.Default.Circle
        ArtworkShape.VINYL -> "Vinyl" to Icons.Default.Album
        ArtworkShape.SQUARE -> "Square" to Icons.Default.Square
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = shapeName,
                tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = shapeName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}
