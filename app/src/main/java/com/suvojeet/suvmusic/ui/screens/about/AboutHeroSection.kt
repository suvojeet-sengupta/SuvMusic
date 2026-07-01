package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.BuildConfig
import com.suvojeet.suvmusic.ui.components.AppLogo
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@Composable
internal fun AboutHeroSection() {
    val colorScheme = MaterialTheme.colorScheme
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))

        AppLogo(
            modifier = Modifier
                .size(100.dp)
                .clip(SquircleShape),
            contentDescription = "SuvMusic Logo",
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SuvMusic",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = SquircleShape,
            color = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelLarge,
                color = onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "A modern player for YouTube Music.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
