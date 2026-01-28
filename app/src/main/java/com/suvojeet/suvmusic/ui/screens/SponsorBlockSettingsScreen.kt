package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.SponsorCategory
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlin.collections.emptySet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorBlockSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val enabledCategories by viewModel.sponsorBlockCategories.collectAsState(initial = emptySet())
    val isMasterEnabled by viewModel.sponsorBlockEnabled.collectAsState(initial = true)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("SponsorBlock Categories") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                // Master Switch
                ListItem(
                    headlineContent = { Text("Enable SponsorBlock", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Skip non-music segments automatically") },
                    trailingContent = {
                        Switch(
                            checked = isMasterEnabled,
                            onCheckedChange = { viewModel.setSponsorBlockEnabled(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Select categories to skip:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(SponsorCategory.entries) { category ->
                val isChecked = enabledCategories.contains(category.key)

                ListItem(
                    headlineContent = { Text(category.displayName) },
                    leadingContent = {
                        // Color Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(category.color, CircleShape)
                        )
                    },
                    trailingContent = {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                viewModel.toggleSponsorCategory(category.key, it)
                            },
                            enabled = isMasterEnabled
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}