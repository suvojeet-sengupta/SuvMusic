package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suvojeet.suvmusic.ui.components.DominantColors

import com.suvojeet.suvmusic.ui.theme.SquircleShape

@Composable
fun AudioQualityDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    dominantColors: DominantColors
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false, // Allow custom width
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Screen padding
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleShape),
                    color = dominantColors.primary, // Match player background
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audio Quality",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = dominantColors.onBackground
                            )
                            
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = dominantColors.onBackground.copy(alpha = 0.1f),
                                        shape = SquircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = dominantColors.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Badge Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = dominantColors.accent.copy(alpha = 0.15f),
                                    shape = SquircleShape
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = dominantColors.accent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Opus • HQ Audio",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = dominantColors.accent
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Description
                        Text(
                            text = "SuvMusic streams audio using the Opus codec, a superior modern format used by YouTube Music.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 26.sp
                            ),
                            color = dominantColors.onBackground.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Key Benefits
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            QualityBenefitItem(
                                title = "Better than MP3",
                                description = "160kbps Opus matches or beats 320kbps MP3 in quality.",
                                dominantColors = dominantColors
                            )
                            QualityBenefitItem(
                                title = "Superior to AAC",
                                description = "Wider frequency range for better bass and crisper highs.",
                                dominantColors = dominantColors
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Got it button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dominantColors.accent,
                                contentColor = Color.White
                            ),
                            shape = SquircleShape
                        ) {
                            Text(
                                text = "Got it",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityBenefitItem(
    title: String,
    description: String,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(dominantColors.accent, RoundedCornerShape(3.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = dominantColors.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp
                ),
                color = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
