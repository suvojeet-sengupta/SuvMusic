package com.suvojeet.suvmusic.ui.screens.settings

import android.graphics.Bitmap
import android.os.Build
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.core.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscordSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showTokenDialog by remember { mutableStateOf(false) }
    var showWebLogin by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "Discord RPC",
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            item { M3ESettingsGroupHeader("CONNECTION") }
            
            item {
                if (uiState.discordToken.isNotBlank()) {
                    M3ESettingsItem(
                        icon = Icons.Default.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Connected",
                        subtitle = "Discord status integration active",
                        trailingContent = {
                            TextButton(
                                onClick = { 
                                    viewModel.setDiscordToken("") 
                                    viewModel.setDiscordRpcEnabled(false)
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Logout", style = MaterialTheme.typography.labelLargeEmphasized)
                            }
                        }
                    )
                } else {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Connect Discord to display your music status.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { showWebLogin = true },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Log In", style = MaterialTheme.typography.labelLargeEmphasized)
                                }
                                OutlinedButton(
                                    onClick = { showTokenDialog = true },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Use Token")
                                }
                            }
                        }
                    }
                }
            }

            item { M3ESettingsGroupHeader("DISPLAY OPTIONS") }
            item {
                val isEnabled = uiState.discordToken.isNotBlank()
                M3ESwitchItem(
                    icon = Icons.Default.Settings,
                    title = "Enable Rich Presence",
                    subtitle = if (!isEnabled) "Log in to enable" else "Show status on Discord",
                    checked = uiState.discordRpcEnabled,
                    onCheckedChange = { if (isEnabled) viewModel.setDiscordRpcEnabled(it) }
                )
            }
            item {
                val isEnabled = uiState.discordToken.isNotBlank() && uiState.discordRpcEnabled
                M3ESwitchItem(
                    icon = Icons.Default.Code,
                    title = "Swap Details & State",
                    subtitle = "Show song title prominently",
                    checked = uiState.discordUseDetails,
                    onCheckedChange = { if (isEnabled) viewModel.setDiscordUseDetails(it) }
                )
            }

            item { M3ESettingsGroupHeader("PREVIEW") }
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    DiscordPreviewCardM3E(
                        useDetails = uiState.discordUseDetails,
                        enabled = uiState.discordRpcEnabled && uiState.discordToken.isNotBlank()
                    )
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
            title = { Text("Enter Discord Token", style = MaterialTheme.typography.titleLargeEmphasized) },
            text = {
                Column {
                    Text("Enter your user token manually. It's stored locally.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Token") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
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
                    Text("Save", style = MaterialTheme.typography.labelLargeEmphasized)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Web Login
    if (showWebLogin) {
        Dialog(
            onDismissRequest = { showWebLogin = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discord Login", style = MaterialTheme.typography.titleLargeEmphasized)
                        IconButton(onClick = { showWebLogin = false }) {
                            Icon(Icons.Default.Close, "Close") 
                        }
                    }
                    
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                
                                val jsSnippet = "javascript:(function()%7Bvar%20i%3Ddocument.createElement('iframe')%3Bdocument.body.appendChild(i)%3Balert(i.contentWindow.localStorage.token.slice(1,-1))%7D)()"
                                val motorola = "motorola"
                                val samsungUserAgent = "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

                                if (Build.MANUFACTURER.equals(motorola, ignoreCase = true)) {
                                    settings.userAgentString = samsungUserAgent
                                } else {
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                }
                                
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                
                                webChromeClient = object : WebChromeClient() {
                                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                        if (!message.isNullOrBlank() && message != "null" && message != "undefined") {
                                            viewModel.setDiscordToken(message)
                                            showWebLogin = false
                                        }
                                        result?.confirm()
                                        return true
                                    }
                                }
                                
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (url.endsWith("/app")) {
                                            view?.stopLoading()
                                            view?.loadUrl(jsSnippet)
                                            return true
                                        }
                                        return false
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscordPreviewCardM3E(useDetails: Boolean, enabled: Boolean) {
    val backgroundColor = Color(0xFF5865F2) // Discord Blurple
    
    ElevatedCard(
        modifier = Modifier.width(320.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PLAYING A GAME", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                    Text("SuvMusic", style = MaterialTheme.typography.titleMediumEmphasized, color = Color.White)
                    
                    if (enabled) {
                        Text(if (useDetails) "Song Title" else "Artist Name", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(if (useDetails) "Artist Name" else "Song Title", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Text("03:45 left", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    } else {
                         Text("Not Playing", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
