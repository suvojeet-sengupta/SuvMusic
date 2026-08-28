package com.suvojeet.suvmusic.composeapp

import androidx.compose.runtime.Composable
import com.suvojeet.suvmusic.core.db.LibraryStore
import com.suvojeet.suvmusic.core.db.ListeningHistoryStore
import com.suvojeet.suvmusic.core.domain.history.ListeningHistorySource
import com.suvojeet.suvmusic.core.domain.library.LibraryFeatureController
import com.suvojeet.suvmusic.core.domain.repository.DownloadManager
import com.suvojeet.suvmusic.core.domain.repository.LocalMediaSource
import com.suvojeet.suvmusic.core.domain.repository.RecommendationSource
import com.suvojeet.suvmusic.core.domain.settings.AppSettingsStore

/**
 * Android entry point for the shared composeApp UI. The existing :app module
 * does not yet host this — it will be wired in during Phase 5 (UI to commonMain).
 * For Phase 0 this exists only so the androidMain source set has at least one
 * Kotlin file and compiles.
 */
@Composable
fun AndroidAppEntry(
    appVersion: String = "0.0.0-dev",
    onOpenUrl: (String) -> Unit = {},
    onPickAudioFile: () -> String? = { null },
    onPickMusicFolder: (() -> String?)? = null,
    onSearchYouTube: (suspend (String) -> List<com.suvojeet.suvmusic.composeapp.ui.RemoteSearchResult>)? = null,
    onResolveStreamSong: (suspend (com.suvojeet.suvmusic.composeapp.ui.RemoteSearchResult) -> com.suvojeet.suvmusic.core.model.Song?)? = null,
    libraryStore: LibraryStore? = null,
    libraryController: LibraryFeatureController? = null,
    historyStore: ListeningHistoryStore? = null,
    historySource: ListeningHistorySource? = null,
    settingsStore: AppSettingsStore? = null,
    localMediaSource: LocalMediaSource? = null,
    recommendationSource: RecommendationSource? = null,
    downloadManager: DownloadManager? = null,
) {
    App(
        appVersion = appVersion,
        onOpenUrl = onOpenUrl,
        onPickAudioFile = onPickAudioFile,
        onPickMusicFolder = onPickMusicFolder,
        onSearchYouTube = onSearchYouTube,
        onResolveStreamSong = onResolveStreamSong,
        libraryStore = libraryStore,
        libraryController = libraryController,
        historyStore = historyStore,
        historySource = historySource,
        settingsStore = settingsStore,
        localMediaSource = localMediaSource,
        recommendationSource = recommendationSource,
        downloadManager = downloadManager,
    )
}
