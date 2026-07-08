package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * First-run welcome flow. Step 1 is the minimal brand-forward welcome; the
 * guest path continues to step 2, a quick "what do you listen to?" language
 * pick that seeds recommendations so the very first Home feed feels personal.
 * (The login path skips step 2 — the YT account already carries taste.)
 */
@Composable
fun WelcomeOnboardingDialog(
    onLoginClick: () -> Unit,
    onContinueAsGuest: (Set<String>) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val selectedLanguages = remember { mutableStateOf(setOf<String>()) }

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
            if (step == 1) {
                WelcomeStep(
                    onLoginClick = onLoginClick,
                    onContinueAsGuest = { step = 2 }
                )
            } else {
                LanguageTasteStep(
                    selected = selectedLanguages.value,
                    onToggle = { lang ->
                        selectedLanguages.value =
                            if (lang in selectedLanguages.value) selectedLanguages.value - lang
                            else selectedLanguages.value + lang
                    },
                    onDone = { onContinueAsGuest(selectedLanguages.value) },
                    onSkip = { onContinueAsGuest(emptySet()) }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onLoginClick: () -> Unit,
    onContinueAsGuest: () -> Unit
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

// Same list (and ordering rationale) as LanguageSelectionDialog: major South
// Asian languages first because that's the app's primary user base.
private val TASTE_LANGUAGES = listOf(
    "Hindi", "English", "Bengali", "Punjabi", "Marathi", "Gujarati",
    "Tamil", "Telugu", "Kannada", "Malayalam", "Urdu", "Bhojpuri",
    "Korean", "Spanish", "Japanese"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageTasteStep(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What do you\nlisten to?",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Pick the languages you enjoy — your Home feed starts personalized from day one. You can change this anytime in Playback settings.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FlowRow(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TASTE_LANGUAGES.forEach { lang ->
                FilterChip(
                    selected = lang in selected,
                    onClick = { onToggle(lang) },
                    label = { Text(lang) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDone,
            enabled = selected.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Text(
                if (selected.isEmpty()) "Pick at least one" else "Done",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                "Skip for now",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
            )
        }
    }
}
