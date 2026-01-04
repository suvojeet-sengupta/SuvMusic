package com.suvojeet.suvmusic.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
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
            title = { Text("Sign in to YouTube Music") },
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
                            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(this@apply, true)
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
                                                
                                                // Try to extract avatar URL
                                                view?.evaluateJavascript(
                                                    "(function() { " +
                                                    "var img = document.querySelector('img.yt-spec-avatar-shape__avatar'); " +
                                                    "return img ? img.src : ''; " +
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
