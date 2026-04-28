package com.suvojeet.suvmusic.composeapp.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.model.ArtworkSize

/**
 * Artwork size picker — ported from
 * `app/.../ui/screens/ArtworkSizeScreen.kt` to commonMain.
 *
 * Stateless: takes the current size + onSelect callback. Hosts provide
 * SessionManager/DataStore plumbing.
 */
@Composable
fun ArtworkSizeScreen(
    current: ArtworkSize,
    onSelect: (ArtworkSize) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Choose album artwork size on the player screen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        ArtworkSize.entries.forEach { size ->
            ArtworkSizeCard(
                size = size,
                isSelected = size == current,
                primaryColor = primaryColor,
                onClick = { onSelect(size) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Smaller artwork sizes leave more room for song controls and lyrics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun ArtworkSizeCard(
    size: ArtworkSize,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "border",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val boxCount = when (size) {
                    ArtworkSize.SMALL -> 1
                    ArtworkSize.MEDIUM -> 2
                    ArtworkSize.LARGE -> 3
                }
                repeat(3) { index ->
                    val boxSize = when {
                        index == 0 -> 16.dp
                        index == 1 -> 24.dp
                        else -> 32.dp
                    }
                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index < boxCount) {
                                    if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                },
                            ),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = size.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = when (size) {
                        ArtworkSize.SMALL -> "65% of screen width"
                        ArtworkSize.MEDIUM -> "75% of screen width"
                        ArtworkSize.LARGE -> "85% of screen width"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = primaryColor,
                )
            }
        }
    }
}
