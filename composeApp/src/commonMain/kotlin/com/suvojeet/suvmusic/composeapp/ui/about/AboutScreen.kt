package com.suvojeet.suvmusic.composeapp.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.updater.UpdateChecker
import com.suvojeet.suvmusic.updater.UpdateInfo

/**
 * SuvMusic About screen — ported from app/.../ui/screens/AboutScreen.kt.
 *
 * Drops compared to the Android original:
 *   - Scaffold + TopAppBar — the desktop window already has its own
 *     navigation; the Android version's scaffold makes sense in a
 *     phone navigation graph but here it would double-bar.
 *   - dpadFocusable (TV remote helper, Android-only)
 *   - animateEnter (per-row staggered animation utility — drop for
 *     parity, re-add when the animation utility is in commonMain)
 *   - AboutViewModel — the original VM is currently a no-op placeholder
 *     (param marked @Suppress("UNUSED_PARAMETER")) so we just drop it.
 *   - R.drawable.logo — replaced with a music-note icon in
 *     AboutHeroSection until Compose Multiplatform resources carry the
 *     real asset over.
 *
 * Same composables otherwise: 7 sections in identical order with the
 * exact same text content as the Android version.
 */
@Composable
fun AboutScreen(
    appVersion: String,
    onOpenUri: (String) -> Unit,
    onHowItWorksClick: () -> Unit = {},
    updateChecker: UpdateChecker? = null,
) {
    var latestUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf(if (updateChecker == null) "Updater unavailable" else "Not checked") }
    var refreshToken by remember { mutableStateOf(0) }
    LaunchedEffect(updateChecker, refreshToken) {
        if (updateChecker != null) {
            updateStatus = "Checking…"
            latestUpdate = updateChecker.checkForUpdate()
            updateStatus = if (latestUpdate == null) "You’re up to date or update metadata is unavailable" else "Update available"
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { AboutHeroSection(appVersion = appVersion) }
        if (updateChecker != null) {
            item {
                UpdateStatusCard(
                    appVersion = appVersion,
                    update = latestUpdate,
                    status = updateStatus,
                    onRefresh = {
                        latestUpdate = null
                        updateStatus = "Checking…"
                        refreshToken++
                    },
                )
            }
        }
        item { AboutDescriptionSection() }
        item { AboutStorySection() }
        item { AboutFeaturesSection() }
        item { AboutDeveloperSection(onOpenUri = onOpenUri) }
        item { AboutTechStackSection() }
        item {
            AboutInformationSection(
                onOpenUri = onOpenUri,
                onHowItWorksClick = onHowItWorksClick,
            )
        }
        item { AboutFooterSection() }
    }
}

@Composable
private fun UpdateStatusCard(
    appVersion: String,
    update: UpdateInfo?,
    status: String,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Updates", style = MaterialTheme.typography.titleMedium)
            Text("Current version: $appVersion", style = MaterialTheme.typography.bodySmall)
            Text(
                update?.let { "Version ${it.versionName} is available" } ?: status,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onRefresh) { Text("Check again") }
            }
        }
    }
}
