package com.suvojeet.suvmusic.composeapp

import androidx.compose.ui.unit.DpSize
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.suvojeet.suvmusic.composeapp.newpipe.YouTubeSearch
import com.suvojeet.suvmusic.composeapp.ui.RemoteSearchResult
import com.suvojeet.suvmusic.core.db.DatabaseDriverFactory
import com.suvojeet.suvmusic.core.db.LibraryStore
import com.suvojeet.suvmusic.core.db.ListeningHistoryStore
import com.suvojeet.suvmusic.core.db.SqlDelightListeningHistorySource
import com.suvojeet.suvmusic.core.db.SqlDelightLibraryRepository
import com.suvojeet.suvmusic.core.domain.library.LibraryFeatureController
import com.suvojeet.suvmusic.core.domain.notification.AppNotification
import com.suvojeet.suvmusic.core.domain.notification.DesktopNotificationSink
import com.suvojeet.suvmusic.core.domain.repository.DesktopDownloadManager
import com.suvojeet.suvmusic.core.domain.repository.DesktopLocalMediaSource
import com.suvojeet.suvmusic.core.domain.repository.DesktopRecommendationSource
import com.suvojeet.suvmusic.core.domain.settings.DesktopAppSettingsStore
import com.suvojeet.suvmusic.core.domain.sync.DesktopLibrarySyncScheduler
import com.suvojeet.suvmusic.updater.UpdateChecker
import com.suvojeet.suvmusic.core.db.buildDatabase
import com.suvojeet.suvmusic.core.model.Song
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import java.net.URI

private const val APP_VERSION = "2.6.6.0"
private const val LIBRARY_SYNC_NOTIFICATION_ID = 42001

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SuvMusic",
        state = rememberWindowState(size = DpSize(1024.dp, 720.dp)),
    ) {
        val database = remember { buildDatabase(DatabaseDriverFactory()) }
        val libraryStore = remember(database) { LibraryStore(database) }
        val libraryController = remember(libraryStore) {
            LibraryFeatureController(SqlDelightLibraryRepository(libraryStore))
        }
        val historyStore = remember(database) { ListeningHistoryStore(database) }
        val historySource = remember(historyStore) { SqlDelightListeningHistorySource(historyStore) }
        val settingsStore = remember { DesktopAppSettingsStore() }
        val updateChecker = remember { UpdateChecker() }
        val notificationSink = remember { DesktopNotificationSink() }
        val downloadManager = remember(notificationSink) {
            DesktopDownloadManager(notificationSink = notificationSink)
        }
        val localMediaSource = remember { DesktopLocalMediaSource() }
        val recommendationSource = remember(localMediaSource) {
            DesktopRecommendationSource(localMediaSource)
        }
        val librarySyncScheduler = remember(localMediaSource, notificationSink) {
            DesktopLibrarySyncScheduler {
                val count = localMediaSource.getAllLocalSongs().size
                notificationSink.show(
                    AppNotification(
                        id = LIBRARY_SYNC_NOTIFICATION_ID,
                        title = "Library scan complete",
                        body = "$count local track(s) available",
                    ),
                )
            }
        }
        androidx.compose.runtime.DisposableEffect(librarySyncScheduler) {
            librarySyncScheduler.schedulePeriodic(intervalHours = 24)
            onDispose { librarySyncScheduler.close() }
        }
        App(
            appVersion = APP_VERSION,
            onOpenUrl = ::openInBrowser,
            onPickAudioFile = ::pickAudioFile,
            onPickMusicFolder = ::pickMusicFolder,
            onSearchYouTube = ::searchYouTube,
            onResolveStreamSong = ::resolveStreamSong,
            libraryStore = libraryStore,
            libraryController = libraryController,
            historyStore = historyStore,
            historySource = historySource,
            settingsStore = settingsStore,
            localMediaSource = localMediaSource,
            recommendationSource = recommendationSource,
            downloadManager = downloadManager,
            updateChecker = updateChecker,
        )
    }
}

/** Adapter from NewPipe's SearchResult to the commonMain RemoteSearchResult. */
private suspend fun searchYouTube(query: String): List<RemoteSearchResult> {
    return YouTubeSearch.search(query).map { result ->
        RemoteSearchResult(
            title = result.title,
            uploader = result.uploader,
            durationSeconds = result.durationSeconds,
            url = result.url,
            thumbnailUrl = result.thumbnailUrl,
        )
    }
}

private suspend fun resolveStreamSong(result: RemoteSearchResult): Song? {
    val nativeResult = com.suvojeet.suvmusic.composeapp.newpipe.SearchResult(
        title = result.title,
        uploader = result.uploader,
        durationSeconds = result.durationSeconds,
        url = result.url,
        thumbnailUrl = result.thumbnailUrl,
    )
    return YouTubeSearch.resolveStreamSong(nativeResult)
}

/**
 * Open a URL in the user's default browser. Uses the AWT Desktop API,
 * which works on Windows / macOS / most Linux DEs. Silently no-ops if the
 * platform doesn't support it (headless, unusual desktop env) — the
 * About screen UI doesn't depend on the result.
 */
private fun openInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (t: Throwable) {
        // Best-effort only.
    }
}

/**
 * Open a native file picker for an audio file. Returns the absolute path
 * of the selected file, or null if the user cancelled.
 *
 * Uses [java.awt.FileDialog] which delegates to the OS-native chooser on
 * Windows / macOS / Linux (XDG portals). Compose Desktop doesn't ship a
 * built-in file picker, so this is the cleanest option without pulling
 * in a third-party library like FileKit.
 */
private fun pickMusicFolder(): String? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select music folder"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
    }
}

private fun pickAudioFile(): String? {
    val dialog = FileDialog(null as Frame?, "Select audio file", FileDialog.LOAD).apply {
        // OS-dependent filter hint. Windows ignores it; macOS NSOpenPanel
        // and most Linux file choosers honour it.
        file = "*.mp3;*.flac;*.m4a;*.ogg;*.opus;*.wav"
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return File(dir, name).absolutePath
}
