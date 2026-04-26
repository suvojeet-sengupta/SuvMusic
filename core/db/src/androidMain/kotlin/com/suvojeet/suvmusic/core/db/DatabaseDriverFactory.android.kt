package com.suvojeet.suvmusic.core.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android driver: AndroidSqliteDriver, opening (or creating) the named
 * SQLite file under the app's standard databases dir.
 *
 * Database file name is intentionally distinct from the legacy Room file
 * (`suvmusic_database`, used by [com.suvojeet.suvmusic.core.data.local.AppDatabase])
 * so the migration in chunk 3b.2.5 can read Room and write SQLDelight in
 * parallel without locking conflicts.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = SuvMusicDatabase.Schema,
        context = context.applicationContext,
        name = DATABASE_FILE_NAME,
    )

    companion object {
        const val DATABASE_FILE_NAME = "suvmusic.sqldelight.db"
    }
}
