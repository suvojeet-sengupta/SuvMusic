package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Appearance settings screen with theme mode and dynamic color options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeModeSheet by remember { mutableStateOf(false) }
    val themeModeSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Theme Section
            SectionTitle("Theme")
            
            ListItem(
                headlineContent = { Text("Theme Mode") },
                supportingContent = { Text(uiState.themeMode.label) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showThemeModeSheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("Dynamic Theme") },
                supportingContent = { Text("Use system colors (Android 12+)") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.dynamicColorEnabled,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    
    // Theme Mode Bottom Sheet
    if (showThemeModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeModeSheet = false },
            sheetState = themeModeSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode(mode)
                                scope.launch {
                                    themeModeSheetState.hide()
                                    showThemeModeSheet = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.themeMode == mode,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = mode.label)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
