package com.suvojeet.suvmusic.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.util.dpadFocusable
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider

/**
 * Support Screen with donation options, help, and contact info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
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
                                imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                                contentDescription = null,
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/TechToli"))
                                    context.startActivity(intent)
                                    showTelegramOptions = false
                                },
                                shape = SquircleShape
                            )
                            .clip(SquircleShape),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    ListItem(
                        headlineContent = { Text("Telegram Group", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("For Support (@Tech_Toli)") },
                        leadingContent = {
                            Icon(
                                imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                                contentDescription = null,
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadFocusable(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Tech_Toli"))
                                    context.startActivity(intent)
                                    showTelegramOptions = false
                                },
                                shape = SquircleShape
                            )
                            .clip(SquircleShape),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTelegramOptions = false }) {
                    Text("Close")
                }
            },
            shape = SquircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Support & Feedback", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(
                                onClick = onBack,
                                shape = CircleShape,
                            )
                            .padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Header illustration
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(SquircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.3f),
                                    primaryColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Support SuvMusic",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "SuvMusic is an ad-free, open-source project created with passion. Your support helps keep this project alive and thriving! 🚀",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // === DONATION / SUPPORT SECTION ===
            item {
                SettingsSectionTitle("Contribute & Support")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SupportListItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Donate,
                        title = "Donate via Coindrop",
                        subtitle = "Support the project directly",
                        accentColor = Color(0xFF007BFF),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://coindrop.to/suvojeet_sengupta"))
                            context.startActivity(intent)
                        }
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.UPI,
                        title = "Pay via UPI",
                        subtitle = "suvojitsengupta21-3@okicici",
                        accentColor = Color(0xFF097969),
                        onClick = {
                            val upiId = "suvojitsengupta21-3@okicici"
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UPI ID", upiId)
                            clipboardManager.setPrimaryClip(clip)

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                com.suvojeet.suvmusic.util.SnackbarUtil.showMessage("UPI ID copied to clipboard")
                            }
                            
                            try {
                                val uri = Uri.parse("upi://pay?pa=$upiId&pn=Suvojeet%20Sengupta")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    )

                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = Icons.Default.Star,
                        title = "Star on GitHub",
                        subtitle = "Show love on our repository",
                        accentColor = Color(0xFF4CAF50),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic"))
                            context.startActivity(intent)
                        }
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = Icons.Default.Share,
                        title = "Share SuvMusic",
                        subtitle = "Spread the word with friends",
                        accentColor = Color(0xFF2196F3),
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out SuvMusic - The best open source music player! \n\nDownload: https://github.com/suvojeet-sengupta/SuvMusic/releases")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // === HELP & FEEDBACK SECTION ===
            item {
                SettingsSectionTitle("Help & Feedback")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SupportListItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                        title = "Join Telegram",
                        subtitle = "Channel & Support Group",
                        accentColor = Color(0xFF0088CC),
                        onClick = { showTelegramOptions = true }
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = Icons.Default.BugReport,
                        title = "Report a Bug",
                        subtitle = "Help us fix issues",
                        accentColor = Color(0xFFE53935),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=bug_report.md"))
                            context.startActivity(intent)
                        }
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = Icons.Default.Lightbulb,
                        title = "Request a Feature",
                        subtitle = "Share your ideas with us",
                        accentColor = Color(0xFFFFC107),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=feature_request.md"))
                            context.startActivity(intent)
                        }
                    )

                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "How SuvMusic handles your data",
                        accentColor = Color(0xFF9C27B0),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://suvojeet-sengupta.github.io/SuvMusic-Website/suvmusic-privacy.html"))
                            context.startActivity(intent)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // === CONTACT SECTION ===
            item {
                SettingsSectionTitle("Contact")
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SupportListItem(
                        icon = Icons.Default.Email,
                        title = "Email",
                        subtitle = "suvojeet@suvojeetsengupta.in",
                        accentColor = primaryColor,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:suvojeet@suvojeetsengupta.in")
                                putExtra(Intent.EXTRA_SUBJECT, "SuvMusic Support")
                            }
                            context.startActivity(intent)
                        }
                    )
                    
                    M3HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SupportListItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                        title = "Telegram DM",
                        subtitle = "@suvojeet_sengupta",
                        accentColor = Color(0xFF0088CC),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/suvojeet_sengupta"))
                            context.startActivity(intent)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Footer
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Made with ",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = " in India",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SupportListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
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
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = Modifier
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .clip(SquircleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
