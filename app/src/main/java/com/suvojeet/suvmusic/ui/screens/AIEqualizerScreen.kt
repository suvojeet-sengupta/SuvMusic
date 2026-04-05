package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ai.AIEqualizerService
import com.suvojeet.suvmusic.ai.AIProvider
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEqualizerScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    aiService: AIEqualizerService,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by aiService.logs.collectAsState()
    val isProcessing by aiService.isProcessing.collectAsState()
    var prompt by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("AI Equalizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Describe how you want your music to sound:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., 'More bass and clear vocals for jazz'") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val provider = when (uiState.selectedAiProvider) {
                                        "OPENAI" -> AIProvider.OPENAI
                                        "ANTHROPIC" -> AIProvider.ANTHROPIC
                                        else -> AIProvider.GEMINI
                                    }
                                    val apiKey = when (provider) {
                                        AIProvider.OPENAI -> uiState.openaiApiKey
                                        AIProvider.ANTHROPIC -> uiState.anthropicApiKey
                                        AIProvider.GEMINI -> uiState.geminiApiKey
                                    }
                                    val model = when (provider) {
                                        AIProvider.OPENAI -> uiState.openaiModel
                                        AIProvider.ANTHROPIC -> uiState.anthropicModel
                                        AIProvider.GEMINI -> uiState.geminiModel
                                    }
                                    aiService.processPrompt(prompt, provider, apiKey, model)
                                }
                            },
                            enabled = prompt.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "AI Internal Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = "> $log",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(bottom = 4.dp),
                            color = if (log.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Button(
                onClick = { aiService.clearLogs() },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors()
            ) {
                Text("Clear Logs")
            }
        }
    }
}
