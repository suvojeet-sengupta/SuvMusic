package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.suvojeet.suvmusic.core.model.LogoVariant
import kotlinx.coroutines.launch

/**
 * "App Logo" branding section for the Appearance settings screen. Renders
 * a single [SettingsCard]-styled NavRow showing the active variant's name
 * and a small preview, plus a bottom-sheet picker with full-size previews
 * of each variant.
 *
 * Host-side because the picker needs `painterResource(R.drawable.logo_*)`
 * for previews — those drawables only exist in :app, not commonMain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoPickerSection(
    selected: LogoVariant,
    onSelect: (LogoVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var pendingVariant by remember { mutableStateOf<LogoVariant?>(null) }
    val ctx = LocalContext.current

    Column(modifier = modifier) {
        Text(
            text = "App Logo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .clickable { showSheet = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = selected.drawableRes()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Logo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = selected.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Choose an app logo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
                Text(
                    text = "Picks the brand mark used across About, the home top bar, the launcher icon, and the splash screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 4.dp),
                )
                Text(
                    text = "Note: the icon shown in App info, system Settings → Apps, and permission dialogs is locked by Android and won't change at runtime — it only updates when the app itself is updated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
                )

                // Group variants by conceptKey so the picker reads
                // "Pulse → 5 styles, Resonance → 5 styles, …" instead of a
                // flat 16-row list. The sub-styles share one launcher icon
                // per concept (only the in-app brand and splash drawable
                // differ across styles within a concept).
                val grouped = LogoVariant.values().groupBy { it.conceptKey }
                grouped.forEach { (_, variants) ->
                    val concept = variants.first().conceptLabel
                    Text(
                        text = concept,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(top = 12.dp, bottom = 4.dp),
                    )
                    variants.forEach { variant ->
                        LogoOptionRow(
                            variant = variant,
                            isSelected = variant == selected,
                            onClick = {
                                if (variant == selected) {
                                    scope.launch {
                                        sheetState.hide()
                                        showSheet = false
                                    }
                                } else {
                                    pendingVariant = variant
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    pendingVariant?.let { variant ->
        AlertDialog(
            onDismissRequest = { pendingVariant = null },
            title = { Text("Change app logo to ${variant.displayName}?") },
            text = {
                Text(
                    "The launcher icon, splash screen, and in-app brand mark will switch to ${variant.displayName}. " +
                        "Android will close SuvMusic so the launcher can refresh — open the app again from your launcher after that.\n\n" +
                        "The icon shown in App info / Settings won't change (Android locks it until the app is updated).",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSelect(variant)
                    pendingVariant = null
                    // Brief toast so the user has a beat to read it before
                    // Android tears the process down. The actual kill happens
                    // inside SessionManager.applyLauncherAlias when it flips
                    // the active alias.
                    android.widget.Toast.makeText(
                        ctx,
                        "Switching to ${variant.displayName}…",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    scope.launch {
                        sheetState.hide()
                        showSheet = false
                    }
                }) {
                    Text("Apply & restart")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingVariant = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun LogoOptionRow(
    variant: LogoVariant,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = variant.drawableRes()),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                // styleLabel = "Hero" / "App Icon" / "Monochrome" / "On Light"
                // / "Single Tone" — the concept name lives in the section
                // header so we don't repeat it on every row.
                text = variant.styleLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = variant.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
    }
}
