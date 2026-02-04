package com.suvojeet.suvmusic.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LikedSongsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val youTubeRepository: YouTubeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if we need to do a full sync or just a quick check
            // For rigorous "find any changes", we should probably do a deep scan
            // However, doing a full sync of 50k songs every time is expensive.
            // Ideally, we fetch page 1-2. If they match local head, we assume no changes (except unlikes).
            // But user asked to "find any changes".
            // Let's do a full sync for now to be safe and standard "Offline-First".
            // We can optimize later if needed.
            val success = youTubeRepository.syncLikedSongs(fetchAll = true)
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
