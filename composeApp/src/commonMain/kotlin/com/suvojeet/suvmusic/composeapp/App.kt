package com.suvojeet.suvmusic.composeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.model.SortOrder
import com.suvojeet.suvmusic.core.model.SortType

@Composable
fun App() {
    // Smoke test that :core:model commonMain is resolvable from desktop —
    // these enums exist in the shared sourceSet now (chunk 2.1).
    val defaultSort = SortType.DATE_ADDED
    val defaultOrder = SortOrder.DESCENDING

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "SuvMusic Desktop",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Phase 2.1 — sharing :core:model with desktop",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = "Default sort: $defaultSort $defaultOrder",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
