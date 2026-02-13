package com.suvojeet.suvmusic.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.suvojeet.suvmusic.data.SessionManager
import kotlinx.coroutines.launch

/**
 * WebView-based YouTube Music login screen.
 * Captures cookies for authenticated API requests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginScreen(
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Sign-in")
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Security Notice Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Login is handled directly by Google. This app only uses the session to play music.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            // Security improvements
                            allowFileAccess = false
                            allowContentAccess = false
                            
                            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        
                        val webView = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(webView, true)
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                currentUrl = url ?: ""
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                // Check if user is logged in (redirected to music.youtube.com)
                                url?.let { currentUrl ->
                                    if (currentUrl.contains("music.youtube.com") && 
                                        !currentUrl.contains("accounts.google.com")) {
                                        
                                        // Get cookies
                                        val cookies = CookieManager.getInstance().getCookie(currentUrl)
                                        if (cookies != null && 
                                            (cookies.contains("SAPISID") || cookies.contains("__Secure-3PAPISID"))) {
                                            
                                            scope.launch {
                                                sessionManager.saveCookies(cookies)

                                                // Extract authuser index
                                                val uri = android.net.Uri.parse(currentUrl)
                                                val authUserParam = uri.getQueryParameter("authuser")
                                                if (authUserParam != null) {
                                                    val index = authUserParam.toIntOrNull() ?: 0
                                                    sessionManager.setAuthUserIndex(index)
                                                } else {
                                                    sessionManager.setAuthUserIndex(0)
                                                }
                                                
                                                // Try to extract avatar URL
                                                view?.evaluateJavascript(
                                                    "(function() { " +
                                                    "var selectors = ['img.yt-spec-avatar-shape__avatar', 'button#avatar-btn img', 'img[src*=\"googleusercontent.com\"]'];" +
                                                    "for (var i = 0; i < selectors.length; i++) {" +
                                                    "  var img = document.querySelector(selectors[i]);" +
                                                    "  if (img && img.src && img.src.indexOf('http') === 0) return img.src;" +
                                                    "}" +
                                                    "return '';" +
                                                    "})()"
                                                ) { result ->
                                                    val avatarUrl = result.trim('"')
                                                    if (avatarUrl.isNotEmpty() && avatarUrl != "null") {
                                                        scope.launch {
                                                            sessionManager.saveUserAvatar(avatarUrl)
                                                        }
                                                    }
                                                    onLoginSuccess()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Handle intent:// URLs (like Play Store redirects)
                                if (url.startsWith("intent://")) {
                                    // Parse the intent to check what it's for
                                    try {
                                        val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                                        val packageName = intent?.`package`
                                        
                                        // If it's trying to open YouTube Music app, just ignore it
                                        // This prevents the "install YT Music" prompt during login
                                        if (packageName == "com.google.android.apps.youtube.music") {
                                            // Simply ignore this redirect - stay on current page
                                            return true
                                        }
                                        
                                        // For other intents, try to handle them
                                        if (intent != null) {
                                            val packageManager = view?.context?.packageManager
                                            val info = packageManager?.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                                            if (info != null) {
                                                view?.context?.startActivity(intent)
                                            }
                                            // If can't handle, just ignore rather than showing error
                                        }
                                    } catch (e: Exception) {
                                        // Silently ignore - don't crash the WebView
                                    }
                                    return true
                                }
                                
                                // Handle market:// URLs (Play Store links) - ignore them during login
                                if (url.startsWith("market://")) {
                                    // Don't redirect to Play Store during login
                                    return true
                                }
                                
                                // Allow normal http/https URLs to load in WebView
                                return false
                            }
                        }
                        
                        loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Clean up if needed
        }
    }
}
