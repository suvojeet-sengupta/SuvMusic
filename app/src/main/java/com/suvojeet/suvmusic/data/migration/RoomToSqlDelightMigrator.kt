package com.suvojeet.suvmusic.data.migration

import android.content.Context
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.SongGenreDao
import com.suvojeet.suvmusic.core.db.DatabaseDriverFactory
import com.suvojeet.suvmusic.core.db.SuvMusicDatabase
import com.suvojeet.suvmusic.core.db.buildDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot migrator that copies user data from the legacy Room database
 * (`suvmusic_database`) into the new SQLDelight database (`suvmusic.sqldelight.db`)
 * managed by [DatabaseDriverFactory].
 *
 * Status (chunk 3b.2.5): scaffolding only. The class compiles, has all the
 * input DAOs + the SuvMusicDatabase wired in, and exposes a [migrate]
 * function — but the function is a NO-OP. The real per-table copy logic
 * lands once:
 *  1. INSERT queries are added to each .sq file in :core:db (chunk 3b.2.5b).
 *  2. The user provides a snapshot of /data/data/com.suvojeet.suvmusic/
 *     databases/suvmusic_database from a real device for testing.
 *
 * Until then, this class is dead code that proves the :app -> :core:db
 * dependency resolves cleanly (the variant-attribute concern that broke
 * 3b.1).
 *
 * The migrator is intentionally NOT registered in Koin yet and not invoked
 * from [com.suvojeet.suvmusic.SuvMusicApplication.onCreate]. Wiring it in
 * is the cutover step (chunk 3b.3) and gated on a smoke test against a
 * real-data snapshot.
 */
@Singleton
class RoomToSqlDelightMigrator @Inject constructor(
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val listeningHistoryDao: ListeningHistoryDao,
    private val libraryDao: LibraryDao,
    private val dislikedItemDao: DislikedItemDao,
    private val songGenreDao: SongGenreDao,
) {

    /** Lazily build the SQLDelight database via the multiplatform driver factory. */
    private val sqlDelightDatabase: SuvMusicDatabase by lazy {
        buildDatabase(DatabaseDriverFactory(context))
    }

    /**
     * Copy every Room table into SQLDelight. Idempotent — safe to call
     * multiple times (TODO: add an actual no-op short-circuit once a
     * SharedPreferences flag is wired in chunk 3b.3).
     *
     * Currently a NO-OP — bodies arrive in 3b.2.5b alongside the INSERT
     * queries.
     */
    suspend fun migrate(): MigrationResult {
        // Chunk 3b.2.5b: replace with actual copy logic.
        // Sketch:
        //   val histories = listeningHistoryDao.getAllHistory()
        //   sqlDelightDatabase.transaction {
        //       histories.forEach { sqlDelightDatabase.listeningHistoryQueries.insert(...) }
        //   }
        //   ...repeat for libraryDao, dislikedItemDao, songGenreDao
        return MigrationResult.NotYetImplemented
    }

    sealed class MigrationResult {
        data object Success : MigrationResult()
        data object NotYetImplemented : MigrationResult()
        data class Failure(val cause: Throwable) : MigrationResult()
    }
}
