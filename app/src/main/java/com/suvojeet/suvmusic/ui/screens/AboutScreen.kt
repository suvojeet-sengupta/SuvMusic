package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.ui.screens.about.AboutDescriptionSection
import com.suvojeet.suvmusic.ui.screens.about.AboutDeveloperSection
import com.suvojeet.suvmusic.ui.screens.about.AboutFeaturesSection
import com.suvojeet.suvmusic.ui.screens.about.AboutFooterSection
import com.suvojeet.suvmusic.ui.screens.about.AboutHeroSection
import com.suvojeet.suvmusic.ui.screens.about.AboutInformationSection
import com.suvojeet.suvmusic.ui.screens.about.AboutTechStackSection
import com.suvojeet.suvmusic.ui.utils.animateEnter
import com.suvojeet.suvmusic.ui.viewmodel.AboutViewModel
import com.suvojeet.suvmusic.util.dpadFocusable

/**
 * About Screen — Material 3 Expressive design.
 * Shell orchestrates sections; section composables live in `ui/screens/about/`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onHowItWorksClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") viewModel: AboutViewModel = hiltViewModel()
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("About SuvMusic", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(onClick = onBack, shape = CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Box(modifier = Modifier.animateEnter(0)) { AboutHeroSection() } }
            item { Box(modifier = Modifier.animateEnter(1)) { AboutDescriptionSection() } }
            item { Box(modifier = Modifier.animateEnter(2)) { AboutFeaturesSection() } }
            item {
                Box(modifier = Modifier.animateEnter(3)) {
                    AboutDeveloperSection(onOpenUri = { uriHandler.openUri(it) })
                }
            }
            item { Box(modifier = Modifier.animateEnter(4)) { AboutTechStackSection() } }
            item {
                Box(modifier = Modifier.animateEnter(5)) {
                    AboutInformationSection(
                        onOpenUri = { uriHandler.openUri(it) },
                        onHowItWorksClick = onHowItWorksClick
                    )
                }
            }
            item { Box(modifier = Modifier.animateEnter(6)) { AboutFooterSection() } }
        }
    }
}
