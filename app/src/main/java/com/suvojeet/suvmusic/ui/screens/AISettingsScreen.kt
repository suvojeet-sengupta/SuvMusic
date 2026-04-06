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
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant Settings", fontWeight = FontWeight.Bold) },
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
    var localModel by remember(model) { mutableStateOf(model) }

    Column {
        Text("Chat Proxy Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = localModel,
            onValueChange = {
                localModel = it
                onModelChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model Name") },
            placeholder = { Text("e.g., gpt-5") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Uses Chat Proxy API (codexapi.workers.dev). No API key required.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
