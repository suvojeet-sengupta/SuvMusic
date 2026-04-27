package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.ui.components.BetaBadge
import com.suvojeet.suvmusic.core.model.ChatProxyModels

/**
 * SuvMusic AI Assistant settings screen — third commonMain port (after
 * AboutScreen and SponsorBlockSettingsScreen).
 *
 * State shape mirrors the SponsorBlock port: stateless composable taking
 * provider keys + per-provider strings + callbacks. The :app side keeps
 * SettingsViewModel / Koin / DataStore plumbing and feeds state in.
 *
 * Differences vs the Android original:
 *   - No Scaffold + TopAppBar: host owns chrome.
 *   - No `dpadFocusable` (Android-only TV helper) — `Modifier.clickable`.
 *   - Provider keys are passed as String constants the host already uses
 *     ("CHAT_PROXY", "GEMINI", "OPENAI", "ANTHROPIC"). We don't introduce
 *     a sealed type because the persistence format (DataStore on Android,
 *     whatever the Desktop side picks later) is canonically the string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    selectedProvider: String,
    chatProxyModel: String,
    geminiApiKey: String,
    geminiModel: String,
    openAiApiKey: String,
    openAiModel: String,
    anthropicApiKey: String,
    anthropicModel: String,
    onProviderSelect: (String) -> Unit,
    onChatProxyModelChange: (String) -> Unit,
    onGeminiApiKeyChange: (String) -> Unit,
    onGeminiModelChange: (String) -> Unit,
    onOpenAiApiKeyChange: (String) -> Unit,
    onOpenAiModelChange: (String) -> Unit,
    onAnthropicApiKeyChange: (String) -> Unit,
    onAnthropicModelChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                BetaBadge()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                "Select AI Provider",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column {
                    ProviderRow(
                        name = "Chat Proxy",
                        description = "Free, no API key required",
                        selected = selectedProvider == "CHAT_PROXY",
                        onSelect = { onProviderSelect("CHAT_PROXY") },
                    )
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProviderRow(
                        name = "Gemini",
                        description = "Google's powerful model",
                        selected = selectedProvider == "GEMINI",
                        onSelect = { onProviderSelect("GEMINI") },
                    )
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProviderRow(
                        name = "OpenAI",
                        description = "GPT-4o and more",
                        selected = selectedProvider == "OPENAI",
                        onSelect = { onProviderSelect("OPENAI") },
                    )
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProviderRow(
                        name = "Anthropic",
                        description = "Claude 3.5 Sonnet",
                        selected = selectedProvider == "ANTHROPIC",
                        onSelect = { onProviderSelect("ANTHROPIC") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            when (selectedProvider) {
                "CHAT_PROXY" -> ChatProxyConfigSection(
                    model = chatProxyModel,
                    onModelChange = onChatProxyModelChange,
                )
                "GEMINI" -> ApiConfigSection(
                    title = "Gemini Configuration",
                    apiKey = geminiApiKey,
                    onApiKeyChange = onGeminiApiKeyChange,
                    model = geminiModel,
                    onModelChange = onGeminiModelChange,
                    placeholder = "Enter Gemini API Key",
                )
                "OPENAI" -> ApiConfigSection(
                    title = "OpenAI Configuration",
                    apiKey = openAiApiKey,
                    onApiKeyChange = onOpenAiApiKeyChange,
                    model = openAiModel,
                    onModelChange = onOpenAiModelChange,
                    placeholder = "Enter OpenAI API Key",
                )
                "ANTHROPIC" -> ApiConfigSection(
                    title = "Anthropic Configuration",
                    apiKey = anthropicApiKey,
                    onApiKeyChange = onAnthropicApiKeyChange,
                    model = anthropicModel,
                    onModelChange = onAnthropicModelChange,
                    placeholder = "Enter Anthropic API Key",
                )
            }
        }
    }
}

@Composable
private fun ProviderRow(
    name: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(name, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Text(description, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = { RadioButton(selected = selected, onClick = onSelect) },
        modifier = Modifier.clickable(onClick = onSelect),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ApiConfigSection(
    title: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    placeholder: String,
) {
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var localModel by remember(model) { mutableStateOf(model) }

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Go to the provider's dashboard to get your API key.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatProxyConfigSection(
    model: String,
    onModelChange: (String) -> Unit,
) {
    val models = remember { ChatProxyModels.withRandomOption() }
    var expanded by remember { mutableStateOf(false) }

    var localModel by remember(model) {
        mutableStateOf(if (model in models || model.isEmpty()) model else models[0])
    }

    Column {
        Text(
            text = "Chat Proxy Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Model",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
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
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                models.forEach { modelKey ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (modelKey == ChatProxyModels.RANDOM) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(ChatProxyModels.displayName(modelKey))
                            }
                        },
                        onClick = {
                            localModel = modelKey
                            onModelChange(modelKey)
                            expanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (localModel == ChatProxyModels.RANDOM) {
                "Randomly picks a model each request. Auto-fallback if one fails."
            } else {
                "Uses Chat Proxy API. Auto-fallback to other models if this one fails."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${ChatProxyModels.ALL.size} models available",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "If selected model fails, the next request will auto-fallback to another model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
