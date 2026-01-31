package com.suvojeet.suvmusic.ui.screens.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }
    var showWebLogin by remember { mutableStateOf(false) }

    // Background Gradient matching SettingsScreen
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Last.fm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Connection Status
                item {
                    LastFmSectionTitle("Account")
                    LastFmCard {
                        if (uiState.lastFmUsername != null) {
                            ListItem(
                                headlineContent = { Text("Connected as ${uiState.lastFmUsername}") },
                                supportingContent = { Text("Scrobbling is active") },
                                trailingContent = {
                                    Button(
                                        onClick = { viewModel.disconnectLastFm() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                    ) {
                                        Text("Logout")
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Connect to Last.fm to scrobble your music.", style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = { showLoginDialog = true }) {
                                    Text("Log In")
                                }
                            }
                        }
                    }
                }

                if (uiState.lastFmUsername != null) {
                    item {
                        LastFmSectionTitle("Scrobbling")
                        LastFmCard {
                            LastFmSwitchItem(
                                icon = Icons.Default.MusicNote,
                                title = "Enable Scrobbling",
                                checked = uiState.lastFmScrobblingEnabled,
                                onCheckedChange = { viewModel.setLastFmScrobblingEnabled(it) }
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            LastFmSwitchItem(
                                icon = Icons.Default.Favorite, // Or another icon like Star or Recommend
                                title = "Enable Recommendations",
                                subtitle = "Show recommended artists and songs on Home",
                                checked = uiState.lastFmRecommendationsEnabled,
                                onCheckedChange = { viewModel.setLastFmRecommendationsEnabled(it) }
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            
                            LastFmSwitchItem(
                                icon = Icons.Default.PlayArrow,
                                title = "Show 'Now Playing'",
                                subtitle = "Update your status while listening",
                                checked = uiState.lastFmUseNowPlaying,
                                onCheckedChange = { viewModel.setLastFmUseNowPlaying(it) }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            LastFmSwitchItem(
                                icon = Icons.Default.Favorite,
                                title = "Send Likes",
                                subtitle = "Mark songs as loved on Last.fm",
                                checked = uiState.lastFmSendLikes,
                                onCheckedChange = { viewModel.setLastFmSendLikes(it) }
                            )
                        }
                    }

                    item {
                        LastFmSectionTitle("Rules")
                        LastFmCard {
                           Column(modifier = Modifier.padding(16.dp)) {
                               // Minimum Duration
                               Text("Minimum Track Duration: ${uiState.scrobbleMinDuration}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                               Slider(
                                   value = uiState.scrobbleMinDuration.toFloat(),
                                   onValueChange = { viewModel.setScrobbleMinDuration(it.toInt()) },
                                   valueRange = 30f..120f,
                                   steps = 9 // 30, 40, ... 120
                               )
                               Text("Tracks shorter than this won't be scrobbled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                               
                               Spacer(Modifier.height(16.dp))
                               
                               // Scrobble Percentage
                               Text("Scrobble Point: ${(uiState.scrobbleDelayPercent * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                               Slider(
                                   value = uiState.scrobbleDelayPercent,
                                   onValueChange = { viewModel.setScrobbleDelayPercent(it) },
                                   valueRange = 0.5f..1.0f,
                                   steps = 4 // 50, 60, 70, 80, 90, 100
                               )
                               Text("Percentage of track played to trigger scrobble.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                           }
                        }
                    }
                }
            }
        }
    }

    // Login Dialog
    if (showLoginDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Log in to Last.fm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, "Toggle password visibility")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    TextButton(
                        onClick = { 
                            showLoginDialog = false
                            showWebLogin = true 
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Login via Browser instead")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        error = null
                        viewModel.loginLastFmMobile(
                            username, password,
                            onSuccess = { loggedInUsername ->
                                isLoading = false
                                showLoginDialog = false
                                onLoginSuccess(loggedInUsername)
                            },
                            onError = { msg ->
                                isLoading = false
                                error = msg
                            }
                        )
                    },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Log In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Web Login View
    if (showWebLogin) {
        val authUrl = remember { viewModel.getLastFmAuthUrl() }
        
        AlertDialog(
            onDismissRequest = { showWebLogin = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            content = {
               Surface(
                   shape = RoundedCornerShape(16.dp),
                   color = MaterialTheme.colorScheme.surface
               ) {
                   Column {
                       Row(
                           modifier = Modifier.fillMaxWidth().padding(8.dp),
                           horizontalArrangement = Arrangement.SpaceBetween,
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           Text("Last.fm Login", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                           IconButton(onClick = { showWebLogin = false }) {
                               Icon(Icons.Default.VisibilityOff, "Close") 
                           }
                       }
                       
                       AndroidView(
                           factory = { ctx ->
                               WebView(ctx).apply {
                                   settings.javaScriptEnabled = true
                                   webViewClient = object : WebViewClient() {
                                       override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                           if (url != null && url.startsWith("suvmusic://lastfm-auth")) {
                                               val token = android.net.Uri.parse(url).getQueryParameter("token")
                                               if (token != null) {
                                                   viewModel.processLastFmToken(
                                                       token,
                                                       onSuccess = { loggedInUsername ->
                                                           showWebLogin = false
                                                           onLoginSuccess(loggedInUsername)
                                                       },
                                                       onError = { /* Handle error toast */ }
                                                   )
                                               }
                                               return true
                                           }
                                           return false
                                       }
                                   }
                                   loadUrl(authUrl)
                               }
                           },
                           modifier = Modifier.weight(1f).fillMaxWidth()
                       )
                   }
               }
            }
        )
    }
}

// --- Local Components (Copied/Adapted from SettingsScreen style) ---

@Composable
private fun LastFmSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
private fun LastFmCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun LastFmSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
