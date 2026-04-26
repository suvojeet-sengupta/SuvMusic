package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.suvojeet.suvmusic.ai.AIEqualizerService
import com.suvojeet.suvmusic.ai.AIProvider
import com.suvojeet.suvmusic.ui.components.BetaBadge
import com.suvojeet.suvmusic.ui.screens.ai_equalizer.AIEqLogsPanel
import com.suvojeet.suvmusic.ui.screens.ai_equalizer.AIEqPromptHistoryDialog
import com.suvojeet.suvmusic.ui.screens.ai_equalizer.AIEqPromptInput
import com.suvojeet.suvmusic.ui.screens.ai_equalizer.AIEqResultActions
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEqualizerScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    aiService: AIEqualizerService,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by aiService.logs.collectAsState()
    val isProcessing by aiService.isProcessing.collectAsState()
    val isABCompareActive by aiService.isABCompareActive.collectAsState()
    val isAutoModeEnabled by aiService.isAutoModeEnabled.collectAsState()
    val promptHistory by aiService.promptHistory.collectAsState()
    val autoStatus by aiService.autoStatus.collectAsState()
    val lastResult by aiService.lastResult.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { aiService.loadPromptHistory() }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Neural Equalizer",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BetaBadge()
                        }
                        Text(
                            text = if (isAutoModeEnabled) "Auto-Tuning Active" else "Manual Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAutoModeEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (lastResult != null) {
                        IconButton(onClick = { aiService.toggleABCompare() }) {
                            Icon(
                                Icons.Default.Compare,
                                contentDescription = "A/B Compare",
                                tint = if (isABCompareActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
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
            AutoModeToggleCard(
                isEnabled = isAutoModeEnabled,
                onToggle = { aiService.setAutoModeEnabled(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Sound Architecture Prompt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AIEqPromptInput(
                prompt = prompt,
                onPromptChange = { prompt = it },
                isProcessing = isProcessing,
                isAutoModeEnabled = isAutoModeEnabled,
                onSend = {
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
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Neural Processing History",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            AIEqLogsPanel(
                logs = logs,
                autoStatus = autoStatus,
                listState = listState,
                onClearLogs = { aiService.clearLogs() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )

            if (lastResult != null) {
                AIResultCard(lastResult!!)
                Spacer(modifier = Modifier.height(16.dp))
                AIEqResultActions(
                    onRevert = { aiService.revertAIChanges() },
                    onSave = {
                        scope.launch {
                            aiService.saveCurrentAISettings(prompt.ifBlank { "Neural Optimized" })
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showHistory) {
        AIEqPromptHistoryDialog(
            promptHistory = promptHistory,
            onDismiss = { showHistory = false },
            onClearAll = { scope.launch { aiService.clearPromptHistory() } },
            onEntryClick = { selectedPrompt ->
                prompt = selectedPrompt
                showHistory = false
            }
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
        border = BorderStroke(
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
                            if (isEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current Architecture",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF69F0AE),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                state.safeEqBands.forEach { band ->
                    val heightFactor = (band + 12f) / 24f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFactor.coerceIn(0.1f, 1f))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterTag("Bass", "${(state.safeBassBoost * 100).toInt()}%")
                ParameterTag("Echo", "${(state.safeVirtualizer * 100).toInt()}%")
                ParameterTag("Spatial", if (state.isSpatialEnabled) "ON" else "OFF")
                ParameterTag("Limiter", "${state.safeLimiterMakeupGain.toInt()}dB")
            }
        }
    }
}

@Composable
fun ParameterTag(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
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
        shape = RoundedCornerShape(12.dp),
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
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
