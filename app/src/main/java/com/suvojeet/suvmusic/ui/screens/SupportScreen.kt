package com.suvojeet.suvmusic.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
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
 * Support Screen with help options, bug reporting, and contact info.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support", fontWeight = FontWeight.Bold) },
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
        containerColor = surfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Header illustration
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.2f),
                                primaryColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                Text(
                    text = "🎧",
                    fontSize = 48.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "How can we help?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "We're here to make your music experience better",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // === HELP SECTION ===
            SupportSectionTitle("Get Help", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
                    SupportItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "FAQ",
                        subtitle = "Frequently asked questions",
                        accentColor = primaryColor,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic/wiki/FAQ"))
                            context.startActivity(intent)
                        }
                    )
                    SupportItem(
                        icon = Icons.AutoMirrored.Filled.Message,
                        title = "Join Telegram",
                        subtitle = "Get help from the community",
                        accentColor = primaryColor,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/suvmusic_app"))
                            context.startActivity(intent)
                        },
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === FEEDBACK SECTION ===
            SupportSectionTitle("Feedback", primaryColor)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerColor)
            ) {
                Column {
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
                        }
                    )
                    SupportItem(
                        icon = Icons.Default.Star,
                        title = "Rate the App",
                        subtitle = "Show your support with 5 stars",
                        accentColor = Color(0xFF4CAF50),
                        onClick = {
                            // Open GitHub repo for now (can be changed to Play Store later)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/SuvMusic"))
                            context.startActivity(intent)
                        },
                        showDivider = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === CONTACT SECTION ===
            SupportSectionTitle("Contact Developer", primaryColor)
            
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
                        subtitle = "suvojeet.developer@gmail.com",
                        accentColor = primaryColor,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:suvojeet.developer@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "SuvMusic Support")
                            }
                            context.startActivity(intent)
                        }
                    )
                    SupportItem(
                        icon = com.suvojeet.suvmusic.ui.utils.SocialIcons.Telegram,
                        title = "Telegram",
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
                    text = " for music lovers",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
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
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
