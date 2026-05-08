package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LanguageSelectionDialog(
    initialSelection: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    // Languages users can pick. The order intentionally puts the major
    // South Asian languages near the top because the app's primary user
    // base is on the subcontinent — but everything below is also fully
    // supported through YouTube Music localized recommendations.
    val languages = listOf(
        "English",
        "Hindi",
        "Bengali",
        "Punjabi",
        "Marathi",
        "Gujarati",
        "Tamil",
        "Telugu",
        "Kannada",
        "Malayalam",
        "Urdu",
        "Spanish",
        "Portuguese",
        "French",
        "German",
        "Italian",
        "Korean",
        "Japanese",
        "Chinese",
        "Arabic",
        "Russian",
        "Turkish",
        "Indonesian",
    )

    var selectedLanguages by remember { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Music Languages",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Select preferred languages. Used to bias home recommendations and search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (selectedLanguages.isEmpty()) {
                    Text(
                        "No languages selected — all languages will be shown.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.height(360.dp)
                ) {
                    items(languages) { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguages = if (selectedLanguages.contains(language)) {
                                        selectedLanguages - language
                                    } else {
                                        selectedLanguages + language
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedLanguages.contains(language),
                                onCheckedChange = { checked ->
                                    selectedLanguages = if (checked) {
                                        selectedLanguages + language
                                    } else {
                                        selectedLanguages - language
                                    }
                                }
                            )
                            Text(
                                text = language,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedLanguages) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
