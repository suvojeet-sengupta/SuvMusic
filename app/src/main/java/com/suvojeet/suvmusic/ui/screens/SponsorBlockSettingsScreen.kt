package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.SponsorCategory
import com.suvojeet.suvmusic.ui.components.*
import com.suvojeet.suvmusic.core.ui.components.*
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SponsorBlockSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val enabledCategories by viewModel.sponsorBlockCategories.collectAsState(initial = emptySet())
    val isMasterEnabled by viewModel.sponsorBlockEnabled.collectAsState(initial = true)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3EPageHeader(
                title = "SponsorBlock",
                onBack = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            item {
                M3ESwitchItem(
                    icon = Icons.Default.FastForward,
                    title = "Enable SponsorBlock",
                    subtitle = "Skip non-music segments automatically",
                    checked = isMasterEnabled,
                    onCheckedChange = { viewModel.setSponsorBlockEnabled(it) }
                )
            }

            if (isMasterEnabled) {
                item { M3ESettingsGroupHeader("SEGMENTS") }
                
                SponsorCategory.entries.forEach { category ->
                    item {
                        val isChecked = enabledCategories.contains(category.key)
                        M3ESponsorBlockCategoryItem(
                            category = category,
                            isChecked = isChecked,
                            onCheckedChange = { checked ->
                                viewModel.toggleSponsorCategory(category.key, checked)
                            }
                        )
                    }
                }
                
                item { M3ESettingsGroupHeader("SKIP BEHAVIOR") }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Skip Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        M3EButtonGroup(
                            options = listOf("Auto", "Ask", "Manual"),
                            selected = "Auto", // Placeholder
                            onSelect = { /* viewModel.setSponsorBlockMode(it) */ },
                            label = { it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3ESponsorBlockCategoryItem(
    category: SponsorCategory,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    M3ESettingsItem(
        icon = Icons.Default.Label, // Generic icon
        iconTint = category.color,
        title = category.displayName,
        onClick = { onCheckedChange(!isChecked) },
        trailingContent = {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    )
}
