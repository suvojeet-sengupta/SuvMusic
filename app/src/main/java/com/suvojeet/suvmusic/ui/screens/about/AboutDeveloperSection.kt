package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.ui.theme.SquircleShape
import com.suvojeet.suvmusic.ui.utils.SocialIcons

@Composable
internal fun AboutDeveloperSection(onOpenUri: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    AboutSectionTitle("Developer")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(SquircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, colorScheme.tertiary)
                        )
                    )
                    .padding(3.dp)
                    .background(colorScheme.surface, SquircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://avatars.githubusercontent.com/u/107928380?v=4",
                    contentDescription = "Suvojeet Sengupta",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(SquircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.logo)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Suvojeet Sengupta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = colorScheme.onSurface
            )

            Text(
                text = "Vibe Coder",
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Thinking matters. I'm a self-taught Kotlin learner who believes architecture and creativity are my true + points. Delivering what AI can't—human touch in code.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialIconBadge(
                    icon = SocialIcons.GitHub,
                    onClick = { onOpenUri("https://github.com/suvojeet-sengupta") }
                )
                SocialIconBadge(
                    icon = Icons.Default.Email,
                    onClick = { onOpenUri("mailto:suvojeet@suvojeetsengupta.in") }
                )
                SocialIconBadge(
                    icon = Icons.Default.Language,
                    onClick = { onOpenUri("https://suvojeet-sengupta.github.io/SuvMusic-Website/") }
                )
                SocialIconBadge(
                    icon = SocialIcons.Instagram,
                    onClick = { onOpenUri("https://www.instagram.com/suvojeet__sengupta?igsh=MWhyMXE4YzhxaDVvNg==") }
                )
                SocialIconBadge(
                    icon = SocialIcons.Telegram,
                    onClick = { onOpenUri("https://t.me/suvojeet_sengupta") }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}
