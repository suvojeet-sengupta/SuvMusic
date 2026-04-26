package com.suvojeet.suvmusic.composeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Lightweight About screen — first real shared UI in commonMain.
 *
 * Intentionally NOT a port of app/.../AboutScreen.kt with its 7 sub-section
 * composables and Android-only resource references. This is a clean
 * reimplementation: hardcoded strings, no Coil image loading, no R.string,
 * no LocalContext, no UriHandler — just text + buttons that surface a URL
 * via a callback the platform fulfils. Lets the desktop build display
 * something that looks like SuvMusic without dragging the Android Compose
 * resource pipeline into commonMain.
 *
 * The Android-side AboutScreen (in :app) keeps its rich layout. This one
 * is the desktop face for now; convergence happens in Phase 5 when the
 * full UI moves to commonMain.
 */
@Composable
fun SuvMusicAboutScreen(
    appVersion: String,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SuvMusic",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Version $appVersion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "A YouTube Music client and local audio player.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )

            Text(
                text = "Now multiplatform — this very screen renders from " +
                    "shared Kotlin code on both Android and Windows.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.widthIn(max = 320.dp))
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Created by Suvojeet Sengupta",
                style = MaterialTheme.typography.bodyLarge,
            )

            TextButton(onClick = { onOpenUrl("https://github.com/suvojeet-sengupta/SuvMusic") }) {
                Text("View on GitHub")
            }

            TextButton(onClick = { onOpenUrl("https://t.me/suvojeet_sengupta") }) {
                Text("Telegram")
            }
        }
    }
}
