package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ai.AIEqualizerService
import com.suvojeet.suvmusic.ai.AIProvider
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext

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
    val isABCompareActive by aiService.isABCompareActive.collectAsState()
    val promptHistory by aiService.promptHistory.collectAsState()
    var prompt by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Loading animation state
    var loadingPhase by remember { mutableStateOf(0) }
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            while (isProcessing) {
                loadingPhase = (loadingPhase + 1) % 3
                delay(800)
            }
        }
    }

    // Auto-scroll logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // Load prompt history on enter
    LaunchedEffect(Unit) {
        aiService.loadPromptHistory()
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
                    if (aiService.lastResult.value != null) {
                        IconButton(onClick = { aiService.toggleABCompare() }) {
                            Icon(
                                Icons.Default.Compare,
                                contentDescription = "A/B Compare",
                                tint = if (isABCompareActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "Prompt History")
                    }
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
                        Row(
                            modifier = Modifier.size(48.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (0 until 3).forEach { index ->
                                val dotSize = animateFloatAsState(
                                    targetValue = if (loadingPhase == index) 8f else 4f,
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(dotSize.value.dp)
                                        .padding(horizontal = 2.dp)
                                        .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
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

            val lastResult by aiService.lastResult.collectAsState()
            if (lastResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // A/B Compare Banner
                if (isABCompareActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Compare,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "A/B Compare Active",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Showing ORIGINAL settings",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(onClick = { aiService.toggleABCompare() }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Disable Compare",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                AIResultCard(lastResult!!)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Revert button
                    Button(
                        onClick = { aiService.revertAIChanges() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Revert")
                    }
                    
                    // Save button
                    Button(
                        onClick = { 
                            scope.launch {
                                aiService.saveCurrentAISettings(prompt.ifBlank { "Manual AI Application" })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }
        }
    }

    // Prompt History Modal
    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Prompt History", fontWeight = FontWeight.Bold)
                    if (promptHistory.entries.isNotEmpty()) {
                        TextButton(onClick = { 
                            scope.launch { aiService.clearPromptHistory() }
                        }) {
                            Text("Clear All")
                        }
                    }
                }
            },
            text = {
                if (promptHistory.entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No prompt history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(promptHistory.entries) { index, entry ->
                            PromptHistoryItem(
                                entry = entry,
                                onClick = {
                                    prompt = entry.prompt
                                    showHistory = false
                                },
                                onDelete = {
                                    // Could implement individual delete here
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AIResultCard(state: com.suvojeet.suvmusic.ai.AudioEffectState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Optimization Applied", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Explicitly handle unboxing to avoid NullPointerException on primitive conversion
            val bassVal = (state.safeBassBoost * 100).toInt()
            val echoVal = (state.safeVirtualizer * 100).toInt()
            val eqBandsString = try {
                state.safeEqBands.joinToString(", ") { it.toInt().toString() }
            } catch (e: Exception) {
                "0, 0, 0, 0, 0, 0, 0, 0, 0, 0"
            }

            Text("Bass: $bassVal% | Echo: $echoVal%", style = MaterialTheme.typography.bodySmall)
            Text("EQ: $eqBandsString", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PromptHistoryItem(
    entry: com.suvojeet.suvmusic.ai.PromptHistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val timeString = remember(entry.timestamp) { dateFormat.format(Date(entry.timestamp)) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = entry.prompt,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.songTitle != null) {
                    Text(
                        text = "🎵 ${entry.songTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
