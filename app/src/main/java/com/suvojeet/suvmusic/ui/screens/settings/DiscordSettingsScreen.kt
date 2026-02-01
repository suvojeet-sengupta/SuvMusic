package com.suvojeet.suvmusic.ui.screens.settings

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTokenDialog by remember { mutableStateOf(false) }
    var showWebLogin by remember { mutableStateOf(false) }

    // Background Gradient
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
                title = { Text("Discord RPC") },
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
                    DiscordSectionTitle("Account")
                    DiscordCard {
                        if (uiState.discordToken.isNotBlank()) {
                            ListItem(
                                headlineContent = { Text("Connected") },
                                supportingContent = { Text("Token is set") },
                                leadingContent = {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Button(
                                        onClick = { 
                                            viewModel.setDiscordToken("") 
                                            viewModel.setDiscordRpcEnabled(false)
                                        },
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
                                Text("Connect Discord to display your music status.", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { showWebLogin = true }) {
                                        Text("Log In via Discord")
                                    }
                                    OutlinedButton(onClick = { showTokenDialog = true }) {
                                        Text("Use Token")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    DiscordSectionTitle("Rich Presence")
                    DiscordCard {
                        val isEnabled = uiState.discordToken.isNotBlank()
                        
                        DiscordSwitchItem(
                            icon = Icons.Default.Settings,
                            title = "Enable Rich Presence",
                            subtitle = if (!isEnabled) "Log in to enable" else "Show status on Discord",
                            checked = uiState.discordRpcEnabled,
                            enabled = isEnabled,
                            onCheckedChange = { viewModel.setDiscordRpcEnabled(it) }
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        DiscordSwitchItem(
                            icon = Icons.Default.Code,
                            title = "Swap Details & State",
                            subtitle = "Show song title prominently instead of artist",
                            checked = uiState.discordUseDetails,
                            enabled = isEnabled && uiState.discordRpcEnabled,
                            onCheckedChange = { viewModel.setDiscordUseDetails(it) }
                        )
                    }
                }

                // Preview Card
                item {
                    DiscordSectionTitle("Preview")
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         DiscordPreviewCard(
                             useDetails = uiState.discordUseDetails,
                             enabled = uiState.discordRpcEnabled && uiState.discordToken.isNotBlank()
                         )
                    }
                }
            }
        }
    }

    // Token Dialog
    if (showTokenDialog) {
        var token by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Enter Discord Token") },
            text = {
                Column {
                    Text("Enter your user token manually. This is stored locally on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Token") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, "Toggle visibility")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDiscordToken(token)
                        showTokenDialog = false
                    },
                    enabled = token.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Web Login View
    if (showWebLogin) {
        Dialog(
            onDismissRequest = { showWebLogin = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discord Login", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = { showWebLogin = false }) {
                            Icon(Icons.Default.Close, "Close") 
                        }
                    }
                    
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (url != null && url.startsWith("suvmusic://discord-auth")) {
                                            val token = android.net.Uri.parse(url).getQueryParameter("token")
                                            if (token != null) {
                                                viewModel.setDiscordToken(token)
                                                showWebLogin = false
                                            }
                                            return true
                                        }
                                        return false
                                    }
                                    
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // Inject JS to extract token
                                        val js = """
                                            (function() {
                                                function checkToken() {
                                                    try {
                                                        var token = (webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m).find(m=>m?.exports?.default?.getToken!==undefined).exports.default.getToken();
                                                        if(token) {
                                                            window.location.href = "suvmusic://discord-auth?token=" + token;
                                                        }
                                                    } catch(e) {}
                                                    
                                                     try {
                                                         var token = localStorage.getItem("token");
                                                          if(token) {
                                                             token = token.replace(/"/g, "");
                                                             window.location.href = "suvmusic://discord-auth?token=" + token;
                                                         }
                                                    } catch(e) {}
                                                }
                                                setInterval(checkToken, 2000);
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(js, null)
                                    }
                                }
                                loadUrl("https://discord.com/login")
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- Local Components ---

@Composable
private fun DiscordSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
private fun DiscordCard(content: @Composable () -> Unit) {
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
private fun DiscordSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(title, fontWeight = FontWeight.Medium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) 
        },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1, color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) } },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun DiscordPreviewCard(useDetails: Boolean, enabled: Boolean) {
    val backgroundColor = Color(0xFF5865F2) // Discord Blurple
    val cardColor = Color(0xFF23272A) // Discord Dark
    
    Card(
        modifier = Modifier.width(320.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("PLAYING A GAME", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Large Image (Album Art)
                Box(modifier = Modifier.size(60.dp)) {
                     Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.DarkGray
                    ) {
                        // Placeholder for Album Art
                        Icon(Icons.Default.Code, null, tint = Color.LightGray, modifier = Modifier.padding(12.dp))
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column {
                    Text("SuvMusic", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    
                    if (enabled) {
                        val line1 = if (useDetails) "Song Title" else "Artist Name"
                        val line2 = if (useDetails) "Artist Name" else "Song Title"
                        
                        Text(line1, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(line2, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        Text("02:30 left", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    } else {
                         Text("Not Playing", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
