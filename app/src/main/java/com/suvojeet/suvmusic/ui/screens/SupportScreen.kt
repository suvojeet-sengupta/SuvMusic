package com.suvojeet.suvmusic.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    val surfaceColor = colorScheme.surface
    val surfaceContainerColor = colorScheme.surfaceContainer
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    var showTelegramOptions by remember { mutableStateOf(false) }

    if (showTelegramOptions) {
        AlertDialog(
            onDismissRequest = { showTelegramOptions = false },
            title = { Text("Join Telegram", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/TechToli"))
                                context.startActivity(intent)
                                showTelegramOptions = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                            contentDescription = null,
                            tint = Color(0xFF0088CC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Telegram Channel", fontWeight = FontWeight.Bold)
                            Text("For Updates (@TechToli)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Tech_Toli"))
                                context.startActivity(intent)
                                showTelegramOptions = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                            contentDescription = null,
                            tint = Color(0xFF0088CC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Telegram Group", fontWeight = FontWeight.Bold)
                            Text("For Support (@Tech_Toli)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTelegramOptions = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support & Feedback", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent // We use the gradient background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor,
                            surfaceContainerColor.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Header illustration
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(30.dp))
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
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Support SuvMusic",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Professional English Text
            Text(
                text = "SuvMusic is an ad-free, open-source project created with passion for the music community. Maintaining the app and adding new features takes significant time and resources.\n\nIf you enjoy SuvMusic, please consider supporting its growth. Every contribution, whether it's through a donation or a star on GitHub, helps keep this project alive and thriving! 🚀",
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp
                ),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // === DONATION / SUPPORT SECTION ===
            SupportSectionTitle("Contribute & Support", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SupportItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.BuyMeACoffee,
                        title = "Buy Me a Coffee",
                        subtitle = "Support directly via BuyMeACoffee",
                        accentColor = Color(0xFFFFDD00),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/suvojeet_sengupta"))
                            context.startActivity(intent)
                        }
                    )
                    
                    SupportItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.UPI,
                        title = "Pay via UPI",
                        subtitle = "suvojitsengupta21-3@okicici",
                        accentColor = Color(0xFF097969), // UPI Green
                        onClick = {
                            val upiId = "suvojitsengupta21-3@okicici"
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UPI ID", upiId)
                            clipboardManager.setPrimaryClip(clip)
                            
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Toast.makeText(context, "UPI ID copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            
                            // Try to open UPI app
                            try {
                                val uri = Uri.parse("upi://pay?pa=$upiId&pn=Suvojeet%20Sengupta")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // If no UPI app, we already copied it
                            }
                        }
                    )

                    SupportItem(
                        icon = Icons.Default.Star,
                        title = "Star on GitHub",
                        subtitle = "Show love on our repository",
                        accentColor = Color(0xFF4CAF50),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic"))
                            context.startActivity(intent)
                        }
                    )
                    
                    SupportItem(
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
                        },
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // === HELP & FEEDBACK SECTION ===
            SupportSectionTitle("Help & Feedback", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
                    SupportItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                        title = "Join Telegram",
                        subtitle = "Channel & Support Group",
                        accentColor = Color(0xFF0088CC),
                        onClick = {
                            showTelegramOptions = true
                        }
                    )
                    SupportItem(
                        icon = Icons.Default.BugReport,
                        title = "Report a Bug",
                        subtitle = "Help us fix issues",
                        accentColor = Color(0xFFE53935),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=bug_report.md"))
                            context.startActivity(intent)
                        }
                    )
                    SupportItem(
                        icon = Icons.Default.Lightbulb,
                        title = "Request a Feature",
                        subtitle = "Share your ideas with us",
                        accentColor = Color(0xFFFFC107),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic/issues/new?template=feature_request.md"))
                            context.startActivity(intent)
                        },
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === CONTACT SECTION ===
            SupportSectionTitle("Contact", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
                    SupportItem(
                        icon = Icons.Default.Email,
                        title = "Email",
                        subtitle = "Suvojitsengupta21@gmail.com",
                        accentColor = primaryColor,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:Suvojitsengupta21@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "SuvMusic Support")
                            }
                            context.startActivity(intent)
                        }
                    )
                    SupportItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                        title = "Telegram DM",
                        subtitle = "@suvojeet_sengupta",
                        accentColor = Color(0xFF0088CC),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/suvojeet_sengupta"))
                            context.startActivity(intent)
                        },
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer
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
private fun SupportSectionTitle(title: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SupportItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}