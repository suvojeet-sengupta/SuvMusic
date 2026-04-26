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
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SortOrder
import com.suvojeet.suvmusic.core.model.SortType

@Composable
fun App() {
    // Smoke test that :core:model commonMain is resolvable from desktop —
    // these classes all exist in the shared sourceSet after chunk 2.3.
    val defaultSort = SortType.DATE_ADDED
    val defaultOrder = SortOrder.DESCENDING
    val sampleSong = Song.fromYouTube(
        videoId = "dQw4w9WgXcQ",
        title = "Sample",
        artist = "Sample Artist",
        album = "Sample Album",
        duration = 0L,
        thumbnailUrl = null,
    )

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
                        text = "Phase 2.3 — full :core:model in commonMain",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = "Default sort: $defaultSort $defaultOrder",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Built sample: ${sampleSong?.title} by ${sampleSong?.artist}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
