package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A concise, professional "What's new" screen shown once after the app updates
 * to a build with notable user-facing changes. Version-gated by the host via
 * [com.suvojeet.suvmusic.data.SessionManager.getWhatsNewSeenVersion]; the host
 * records the current version code on dismiss so it appears at most once per
 * release. Intentionally editorial and monochrome — a short list of real
 * improvements rather than a decorative splash.
 */
@Composable
fun WhatsNewDialog(
    versionLabel: String,
    onDismiss: () -> Unit
) {
    val items = remember {
        listOf(
            WhatsNewEntry(
                icon = Icons.Rounded.Speed,
                title = "Smoother across the board",
                description = "Scrolling, navigation and screen transitions are faster and lighter on every device."
            ),
            WhatsNewEntry(
                icon = Icons.Rounded.GraphicEq,
                title = "A snappier now-playing",
                description = "The player responds instantly and uses less battery during playback."
            ),
            WhatsNewEntry(
                icon = Icons.Rounded.Tune,
                title = "Refined visuals",
                description = "Backgrounds and glass effects were rebuilt to render clean without dropping frames."
            ),
            WhatsNewEntry(
                icon = Icons.Rounded.Search,
                title = "Quicker search & artwork",
                description = "Results and album art load more responsively as you browse."
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = 40.dp, bottom = 28.dp)
            ) {
                Text(
                    text = "What's new",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(36.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    items.forEach { entry ->
                        WhatsNewRow(entry)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Got it",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private data class WhatsNewEntry(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
private fun WhatsNewRow(entry: WhatsNewEntry) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Neutral, monochrome mark — deliberately not a colored gradient circle.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
            )
        }
    }
}
