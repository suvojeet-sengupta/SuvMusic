package com.suvojeet.suvmusic.ui.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AboutDescriptionSection() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    AboutSectionTitle("About")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SuvMusic is a client for YouTube Music with a clean, ad-free interface and fine-grained playback control.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "It focuses on a responsive listening experience, giving you quick access to a large catalogue while keeping the app simple to use.",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}
