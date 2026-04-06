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
    val isAutoModeEnabled by aiService.isAutoModeEnabled.collectAsState()
    val promptHistory by aiService.promptHistory.collectAsState()
    val validationWarnings by aiService.validationWarnings.collectAsState()
    
    var prompt by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Cleanup on navigation away
    DisposableEffect(Unit) {
        onDispose {
            aiService.cleanup()
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
                title = { 
                    Column {
                        Text("Neural Equalizer", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(
                            text = if (isAutoModeEnabled) "Auto-Tuning Active" else "Manual Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAutoModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            // Auto Mode Toggle Card
            AutoModeToggleCard(
                isEnabled = isAutoModeEnabled,
                onToggle = { aiService.setAutoModeEnabled(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Prompt Input Section
            Text(
                "Sound Architecture Prompt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., 'Make it feel like a live stadium concert'") },
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                trailingIcon = {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val provider = when (uiState.selectedAiProvider) {
                                        "OPENAI" -> AIProvider.OPENAI
                                        "ANTHROPIC" -> AIProvider.ANTHROPIC
                                        "CHAT_PROXY" -> AIProvider.CHAT_PROXY
                                        else -> AIProvider.GEMINI
                                    }
                                    val apiKey = when (provider) {
                                        AIProvider.OPENAI -> uiState.openaiApiKey
                                        AIProvider.ANTHROPIC -> uiState.anthropicApiKey
                                        else -> "" 
                                    }
                                    val model = when (provider) {
                                        AIProvider.OPENAI -> uiState.openaiModel
                                        AIProvider.ANTHROPIC -> uiState.anthropicModel
                                        AIProvider.CHAT_PROXY -> uiState.chatProxyModel
                                        AIProvider.GEMINI -> uiState.geminiModel
                                    }
                                    aiService.processPrompt(prompt, provider, apiKey, model)
                                }
                            },
                            enabled = prompt.isNotBlank() && !isAutoModeEnabled
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Process")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Neural Terminal
            Text(
                "Neural Processing Link",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                color = Color.Black,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A1A1A))
            ) {
                Box {
                    // CRT Scanline Effect (Visual only)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scanlineCount = size.height.toInt() / 4
                        for (i in 0 until scanlineCount) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.03f),
                                start = androidx.compose.ui.geometry.Offset(0f, i * 4f),
                                end = androidx.compose.ui.geometry.Offset(size.width, i * 4f),
                                strokeWidth = 1f
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(logs) { log ->
                            val color = when {
                                log.startsWith("Error") || log.contains("FAILED") -> Color(0xFFFF5252)
                                log.startsWith("SUCCESS") -> Color(0xFF69F0AE)
                                log.startsWith("AUTO") -> Color(0xFF40C4FF)
                                log.startsWith("Validation") -> Color(0xFFFFD740)
                                else -> Color(0xFFE0E0E0)
                            }
                            
                            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    text = "[SYS] ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                )
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    ),
                                    color = color
                                )
                            }
                        }
                    }
                    
                    // Clear logs overlay
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = { aiService.clearLogs() },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            val lastResult by aiService.lastResult.collectAsState()
            if (lastResult != null) {
                // Optimized Result Card
                AIResultCard(lastResult!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { aiService.revertAIChanges() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Revert")
                    }
                    
                    Button(
                        onClick = { 
                            scope.launch {
                                aiService.saveCurrentAISettings(prompt.ifBlank { "Neural Optimized" })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Prompt History Modal (unchanged but mentioned for context)
    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Neural History", fontWeight = FontWeight.Bold) },
            text = {
                if (promptHistory.entries.isEmpty()) {
                    Text("Empty repository")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(promptHistory.entries) { _, entry ->
                            PromptHistoryItem(entry = entry, onClick = { prompt = entry.prompt; showHistory = false }, onDelete = {})
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } }
        )
    }
}

@Composable
fun AutoModeToggleCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Auto Neural Link", fontWeight = FontWeight.Bold)
                    Text(
                        "Automatically tune each song",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun AIResultCard(state: com.suvojeet.suvmusic.ai.AudioEffectState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Architecture", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Micro-Visualizer for EQ
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                state.safeEqBands.forEach { band ->
                    val heightFactor = (band + 12f) / 24f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFactor.coerceIn(0.1f, 1f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ParameterTag("Bass", "${(state.safeBassBoost * 100).toInt()}%")
                ParameterTag("Echo", "${(state.safeVirtualizer * 100).toInt()}%")
                ParameterTag("Spatial", if(state.isSpatialEnabled) "ON" else "OFF")
                ParameterTag("Limiter", "${state.safeLimiterMakeupGain.toInt()}dB")
            }
        }
    }
}

@Composable
fun ParameterTag(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
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
