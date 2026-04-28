package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp),
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    var showTelegramOptions by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(SquircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                primaryColor.copy(alpha = 0.05f),
                            ),
                        ),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(64.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Support SuvMusic",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SuvMusic is an ad-free, open-source project created with passion. Your support helps keep this project alive and thriving!",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
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
                    accentColor = Color(0xFF007BFF),
                    onClick = { onOpenUri("https://coindrop.to/suvojeet_sengupta") },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Pay via UPI",
                    subtitle = "suvojitsengupta21-3@okicici",
                    accentColor = Color(0xFF097969),
                    onClick = {
                        onCopyText("suvojitsengupta21-3@okicici", "UPI ID")
                        onOpenUri("upi://pay?pa=suvojitsengupta21-3@okicici&pn=Suvojeet%20Sengupta")
                    },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Star,
                    title = "Star on GitHub",
                    subtitle = "Show love on our repository",
                    accentColor = Color(0xFF4CAF50),
                    onClick = { onOpenUri("https://github.com/suvojeet-sengupta/SuvMusic") },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Share,
                    title = "Share SuvMusic",
                    subtitle = "Spread the word with friends",
                    accentColor = Color(0xFF2196F3),
                    onClick = {
                        onShareText(
                            "Check out SuvMusic - The best open source music player! \n\n" +
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
                    icon = Icons.Default.Send,
                    title = "Join Telegram",
                    subtitle = "Channel & Support Group",
                    accentColor = Color(0xFF0088CC),
                    onClick = { showTelegramOptions = true },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.BugReport,
                    title = "Report a Bug",
                    subtitle = "Help us fix issues",
                    accentColor = Color(0xFFE53935),
                    onClick = {
                        onOpenUri("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=bug_report.md")
                    },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Lightbulb,
                    title = "Request a Feature",
                    subtitle = "Share your ideas with us",
                    accentColor = Color(0xFFFFC107),
                    onClick = {
                        onOpenUri("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=feature_request.md")
                    },
                )
                ThinDivider()
                SupportListItem(
                    icon = Icons.Default.Security,
                    title = "Privacy Policy",
                    subtitle = "How SuvMusic handles your data",
                    accentColor = Color(0xFF9C27B0),
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
                    accentColor = Color(0xFF0088CC),
                    onClick = { onOpenUri("https://t.me/suvojeet_sengupta") },
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Made with ",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF4081),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = " in India",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
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
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
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
