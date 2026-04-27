package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.suvojeet.suvmusic.core.model.ChatProxyModels
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.components.BetaBadge
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Assistant Settings", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        BetaBadge()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Select AI Provider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        AIProviderItem("Chat Proxy", "Free, no API key required", uiState.selectedAiProvider == "CHAT_PROXY") {
                            viewModel.setSelectedAiProvider("CHAT_PROXY")
                        }
                        M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        AIProviderItem("Gemini", "Google's powerful model", uiState.selectedAiProvider == "GEMINI") {
                            viewModel.setSelectedAiProvider("GEMINI")
                        }
                        M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        AIProviderItem("OpenAI", "GPT-4o and more", uiState.selectedAiProvider == "OPENAI") {
                            viewModel.setSelectedAiProvider("OPENAI")
                        }
                        M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        AIProviderItem("Anthropic", "Claude 3.5 Sonnet", uiState.selectedAiProvider == "ANTHROPIC") {
                            viewModel.setSelectedAiProvider("ANTHROPIC")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                when (uiState.selectedAiProvider) {
                    "CHAT_PROXY" -> ChatProxyConfigSection(
                        model = uiState.chatProxyModel,
                        onModelChange = { viewModel.setChatProxyModel(it) }
                    )
                    "GEMINI" -> APIConfigSection(
                        title = "Gemini Configuration",
                        apiKey = uiState.geminiApiKey,
                        onApiKeyChange = { viewModel.setGeminiApiKey(it) },
                        model = uiState.geminiModel,
                        onModelChange = { viewModel.setGeminiModel(it) },
                        placeholder = "Enter Gemini API Key"
                    )
                    "OPENAI" -> APIConfigSection(
                        title = "OpenAI Configuration",
                        apiKey = uiState.openaiApiKey,
                        onApiKeyChange = { viewModel.setOpenAiApiKey(it) },
                        model = uiState.openaiModel,
                        onModelChange = { viewModel.setOpenAiModel(it) },
                        placeholder = "Enter OpenAI API Key"
                    )
                    "ANTHROPIC" -> APIConfigSection(
                        title = "Anthropic Configuration",
                        apiKey = uiState.anthropicApiKey,
                        onApiKeyChange = { viewModel.setAnthropicApiKey(it) },
                        model = uiState.anthropicModel,
                        onModelChange = { viewModel.setAnthropicModel(it) },
                        placeholder = "Enter Anthropic API Key"
                    )
                }
            }
        }
    }
}

@Composable
fun AIProviderItem(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(description, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { RadioButton(selected = selected, onClick = onClick) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun APIConfigSection(
    title: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    placeholder: String
) {
    // Use local states for smooth typing
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var localModel by remember(model) { mutableStateOf(model) }

    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = localApiKey,
            onValueChange = {
                localApiKey = it
                onApiKeyChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = localModel,
            onValueChange = {
                localModel = it
                onModelChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model Name") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Go to the provider's dashboard to get your API key.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatProxyConfigSection(
    model: String,
    onModelChange: (String) -> Unit
) {
    val models = remember { ChatProxyModels.withRandomOption() }
    var expanded by remember { mutableStateOf(false) }

    // Ensure localModel is valid (in case old saved value is no longer in list)
    var localModel by remember(model) {
        mutableStateOf(if (model in models || model.isEmpty()) model else models[0])
    }

    Column {
        Text("Chat Proxy Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = ChatProxyModels.displayName(localModel),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { modelKey ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (modelKey == ChatProxyModels.RANDOM) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(ChatProxyModels.displayName(modelKey))
                            }
                        },
                        onClick = {
                            localModel = modelKey
                            onModelChange(modelKey)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (localModel == ChatProxyModels.RANDOM)
                "Randomly picks a model each request. Auto-fallback if one fails."
            else
                "Uses Chat Proxy API. Auto-fallback to other models if this one fails.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show available models count
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "${ChatProxyModels.ALL.size} models available",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "If selected model fails, the next request will auto-fallback to another model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
