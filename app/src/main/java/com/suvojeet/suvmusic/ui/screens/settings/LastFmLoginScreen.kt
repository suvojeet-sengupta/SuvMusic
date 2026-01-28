package com.suvojeet.suvmusic.ui.screens.settings

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // We need to get the auth URL directly from repository via ViewModel or just hardcode/construct it here?
    // Ideally ViewModel should provide it. 
    // Since we didn't add getAuthUrl to ViewModel in the plan, I can create a new function or helper.
    // Wait, the plan said "retrieve the getAuthUrl from repository/viewmodel". 
    // I will add a helper in the Composable to call the repo if needed, but better to use ViewModel.
    // Actually, I can just inject the repo or let the VM provide a method. 
    // Let's modify VM to expose `getAuthUrl` or just use the one we know.
    // Ideally, I should have added `getAuthUrl()` to ViewModel.
    // For now, I will use a direct call if I can, OR update ViewModel quickly.
    // Let's assume onLoginSuccess is handled by the caller or we handle logic here.
    
    // Actually, the plan was:
    // Update SettingsViewModel to process token.
    // Loading URL: LastFmRepository has getAuthUrl public method.
    // I can just access it if I had the instance.
    // I will modify SettingsViewModel to expose it, or duplicate logic (bad), or since I'm in execution, 
    // I already updated ViewModel. I can add `getAuthUrl` to VM now quickly as I write this file (simulated).
    // NO, I must strictly follow tool usage. 
    // I will assume I can access it via a new call to VM in the code below or add it now.
    
    // Better: Add `getAuthUrl` to SettingsViewModel first.
    
    // Wait, I can't look back at VM code easily while writing this.
    // I'll pause this write, update VM, then write this.
    // No, I can write this file assuming VM has `getLastFmAuthUrl()` 
    // and then go update VM. That's safer.
    
    var isLoading by remember { mutableStateOf(true) }
    
    // We need a way to get the URL. I'll add `getLastFmAuthUrl` to ViewModel.
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Last.fm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                
                                // Intercept callback
                                if (url != null && url.startsWith("suvmusic://lastfm-auth")) {
                                    val uri = android.net.Uri.parse(url)
                                    val token = uri.getQueryParameter("token")
                                    if (token != null) {
                                        viewModel.processLastFmToken(
                                            token = token,
                                            onSuccess = { username ->
                                                onLoginSuccess(username)
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                                                onBack() // Go back on error
                                            }
                                        )
                                    } else {
                                        onBack()
                                    }
                                    // Stop loading this page
                                    view?.stopLoading()
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        // Use a way to get URL. For now, accessing repo indirectly via VM wrapper I'll add
                        // Or I can construct it here temporarily if I haven't added it to VM yet.
                        // Ideally, `viewModel.getLastFmAuthUrl()`
                        // I will add the method to VM after this file creation.
                         loadUrl(viewModel.getLastFmAuthUrl())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
