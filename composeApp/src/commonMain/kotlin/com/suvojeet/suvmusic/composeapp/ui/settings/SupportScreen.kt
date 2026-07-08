package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.composeapp.theme.SquircleShape

/**
 * Support & feedback screen — ported from
 * `app/.../ui/screens/SupportScreen.kt` to commonMain.
 *
 * Differences vs the Android original:
 *   - Stateless: takes `onOpenUri`, `onCopyText`, `onShareText` callbacks
 *     instead of using LocalContext + Intent + ClipboardManager. Hosts wire
 *     these to platform-native flows.
 *   - UPI clipboard copy: `onCopyText(upiId, "UPI ID")` from this screen.
 *     Hosts decide whether to also fire a platform UPI deeplink. The
 *     desktop host can simply ignore the upi://pay URI.
 *   - SocialIcons.Telegram/UPI/Donate (Android-only vector drawables) get
 *     swapped for Material analogues (Send/AccountBalance/AttachMoney) —
 *     same approach AboutDeveloperSection took.
 *   - Build.VERSION.SDK_INT branch removed: Android 13+ shows its own
 *     copy-confirmation toast. The host can still surface a snackbar via
 *     onCopyText if it wants to.
 *   - No Scaffold/TopAppBar/SnackbarHost — host owns chrome.
 */
@Composable
fun SupportScreen(
    onOpenUri: (String) -> Unit,
    onCopyText: (text: String, label: String) -> Unit,
    onShareText: (text: String) -> Unit,
    onSubmitFeedback: (
        rating: Int,
        category: String,
        message: String,
        userName: String?,
        userEmail: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp),
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    var showTelegramOptions by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    if (showTelegramOptions) {
        AlertDialog(
            onDismissRequest = { showTelegramOptions = false },
            title = { Text("Join Telegram", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Telegram Channel", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("For Updates (@TechToli)") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleShape)
                            .clickable {
                                onOpenUri("https://t.me/TechToli")
                                showTelegramOptions = false
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    ListItem(
                        headlineContent = { Text("Telegram Group", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("For Support (@Tech_Toli)") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleShape)
                            .clickable {
                                onOpenUri("https://t.me/Tech_Toli")
                                showTelegramOptions = false
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTelegramOptions = false }) { Text("Close") }
            },
            shape = SquircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(SquircleShape)
                    .background(colorScheme.surfaceContainerHighest),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Support SuvMusic",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SuvMusic is an ad-free, open-source music player. If it's useful to you, there are a few ways to help it keep going.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            SectionTitle("Contribute & Support")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SupportListItem(
                    icon = Icons.Default.AttachMoney,
                    title = "Donate via Coindrop",
                    subtitle = "Support the project directly",
                    accentColor = primaryColor,
                    onClick = { onOpenUri("https://coindrop.to/suvojeet_sengupta") },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Pay via UPI",
                    subtitle = "suvojitsengupta21-3@okicici",
                    accentColor = primaryColor,
                    onClick = {
                        onCopyText("suvojitsengupta21-3@okicici", "UPI ID")
                        onOpenUri("upi://pay?pa=suvojitsengupta21-3@okicici&pn=Suvojeet%20Sengupta")
                    },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Star,
                    title = "Star on GitHub",
                    subtitle = "Star the repository",
                    accentColor = primaryColor,
                    onClick = { onOpenUri("https://github.com/suvojeet-sengupta/SuvMusic") },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Share,
                    title = "Share SuvMusic",
                    subtitle = "Tell others about the app",
                    accentColor = primaryColor,
                    onClick = {
                        onShareText(
                            "SuvMusic - an ad-free, open-source music player.\n\n" +
                                "Download: https://github.com/suvojeet-sengupta/SuvMusic/releases",
                        )
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Help & Feedback")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SupportListItem(
                    icon = Icons.Default.Feedback,
                    title = "Send Feedback",
                    subtitle = "Share your thoughts directly with us",
                    accentColor = primaryColor,
                    onClick = { showFeedbackDialog = true },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Send,
                    title = "Join Telegram",
                    subtitle = "Channel and support group",
                    accentColor = primaryColor,
                    onClick = { showTelegramOptions = true },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.BugReport,
                    title = "Report a Bug",
                    subtitle = "Open an issue on GitHub",
                    accentColor = primaryColor,
                    onClick = {
                        onOpenUri("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=bug_report.md")
                    },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Lightbulb,
                    title = "Request a Feature",
                    subtitle = "Suggest an improvement",
                    accentColor = primaryColor,
                    onClick = {
                        onOpenUri("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=feature_request.md")
                    },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Security,
                    title = "Privacy Policy",
                    subtitle = "How SuvMusic handles your data",
                    accentColor = primaryColor,
                    onClick = {
                        onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/suvmusic-privacy.html")
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Contact")
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SupportListItem(
                    icon = Icons.Default.Email,
                    title = "Email",
                    subtitle = "suvojeet@suvojeetsengupta.in",
                    accentColor = primaryColor,
                    onClick = { onOpenUri("mailto:suvojeet@suvojeetsengupta.in?subject=SuvMusic%20Support") },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Send,
                    title = "Telegram DM",
                    subtitle = "@suvojeet_sengupta",
                    accentColor = primaryColor,
                    onClick = { onOpenUri("https://t.me/suvojeet_sengupta") },
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                text = "Made in India",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSubmit = onSubmitFeedback,
            onOpenUri = onOpenUri
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(start = 28.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun ThinDivider() {
    M3HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    )
}

@Composable
private fun SupportListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = Modifier
            .clip(SquircleShape)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        rating: Int,
        category: String,
        message: String,
        userName: String?,
        userEmail: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onOpenUri: (String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var category by remember { mutableStateOf("general") }
    var message by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "general" to "General",
        "bug" to "Bug",
        "feature" to "Feature",
        "ui_ux" to "UI/UX",
        "performance" to "Performance",
        "improvement" to "Improvement",
        "other" to "Other"
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting && successMessage == null) onDismiss() },
        title = {
            Text(
                text = if (successMessage != null) "Thank You!" else "Send Feedback",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "Your feedback helps improve SuvMusic. Select a rating and leave a message below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Stars rating selector
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$i Stars",
                                tint = if (i <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable(enabled = !isSubmitting) { rating = i }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category selection
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (catId, catLabel) ->
                            val selected = category == catId
                            FilterChip(
                                selected = selected,
                                onClick = { if (!isSubmitting) category = catId },
                                label = { Text(catLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message field
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message (Required)") },
                        placeholder = { Text("Describe your feedback, request, or issue...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        enabled = !isSubmitting,
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional User Info (Name & Email)
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSubmitting
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Email (Optional)") },
                        placeholder = { Text("If you want us to reply...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSubmitting
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Error UI with direct Telegram / Email fallbacks!
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = SquircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Direct support links:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = { onOpenUri("mailto:suvojeet@suvojeetsengupta.in?subject=SuvMusic%20Feedback") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Email Support", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                    TextButton(
                                        onClick = { onOpenUri("https://t.me/Tech_Toli") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Telegram Group", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (successMessage != null) {
                TextButton(onClick = onDismiss) { Text("OK") }
            } else {
                Button(
                    onClick = {
                        if (message.isBlank()) {
                            errorMessage = "Please enter a message"
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        onSubmit(
                            rating,
                            category,
                            message,
                            userName.ifBlank { null },
                            userEmail.ifBlank { null },
                            /* onSuccess = */ {
                                isSubmitting = false
                                successMessage = "Your feedback has been submitted successfully!"
                            },
                            /* onError = */ { error ->
                                isSubmitting = false
                                errorMessage = "Feedback server down. Please try again or use direct support links below."
                            }
                        )
                    },
                    enabled = !isSubmitting && message.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit")
                    }
                }
            }
        },
        dismissButton = {
            if (successMessage == null) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        },
        shape = SquircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
