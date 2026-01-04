package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Settings screen with audio quality, theme, and account settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onLoginClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQualitySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Account Section
        SectionTitle("Account")
        
        if (uiState.isLoggedIn) {
            ListItem(
                headlineContent = { Text("Signed in") },
                supportingContent = { Text("YouTube Music connected") },
                leadingContent = {
                    if (uiState.userAvatarUrl != null) {
                        AsyncImage(
                            model = uiState.userAvatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
            
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign out",
                subtitle = "Disconnect from YouTube Music",
                onClick = { viewModel.logout() }
            )
        } else {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Login,
                title = "Sign in to YouTube Music",
                subtitle = "Access your playlists and recommendations",
                onClick = onLoginClick
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Playback Section
        SectionTitle("Playback")
        
        SettingsItem(
            icon = Icons.Default.HighQuality,
            title = "Audio Quality",
            subtitle = uiState.audioQuality.label,
            onClick = { showQualitySheet = true }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Appearance Section
        SectionTitle("Appearance")
        
        ListItem(
            headlineContent = { Text("Dynamic Theme") },
            supportingContent = { Text("Use system colors (Android 12+)") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null
                )
            },
            trailingContent = {
                Switch(
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // About Section
        SectionTitle("About")
        
        SettingsItem(
            icon = Icons.Default.Info,
            title = "SuvMusic",
            subtitle = "Version 1.0.0",
            onClick = { }
        )
        
        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // Audio Quality Bottom Sheet
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Audio Quality",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setAudioQuality(quality)
                                scope.launch {
                                    sheetState.hide()
                                    showQualitySheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.audioQuality == quality,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = quality.label)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}
