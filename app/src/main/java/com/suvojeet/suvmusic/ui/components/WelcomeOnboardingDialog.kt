package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * First-run welcome screen. Deliberately minimal and brand-forward: the real
 * app logo, one confident headline, a short supporting line, and two clear
 * actions. No decorative gradient badges or generic feature-card grids — the
 * goal is a calm, professional first impression rather than a busy splash.
 */
@Composable
fun WelcomeOnboardingDialog(
    onLoginClick: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* First-run gate — not dismissible by tapping out */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
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
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.weight(0.9f))

                // Brand logo — the actual app mark, not a generic icon.
                AppLogo(size = 76.dp)

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Millions of tracks.\nAd-free, always.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 42.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Stream your YouTube Music library in high quality — " +
                        "no interruptions, no clutter. Everything stays on your device.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.weight(1f))

                // Primary action — full-width pill, brand color.
                Button(
                    onClick = onLoginClick,
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
                        "Log in with YouTube Music",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        "Continue as guest",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}
