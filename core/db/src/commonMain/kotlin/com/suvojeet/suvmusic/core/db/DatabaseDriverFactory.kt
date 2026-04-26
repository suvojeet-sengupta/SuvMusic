package com.suvojeet.suvmusic.core.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-agnostic factory for the SQLDelight driver.
 *
 * Each actual provides its own constructor — Android takes a Context,
 * Desktop takes no arguments and uses a JDBC URL. Consumers in commonMain
 * receive a fully-constructed factory through DI and only call [createDriver].
 *
 * Wired into Koin and consumed by the migration coroutine in chunk 3b.2.5.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Convenience: build a fully-configured [SuvMusicDatabase] from a factory.
 * Lives in commonMain so both Android and Desktop call sites use the
 * same wiring.
 */
fun buildDatabase(factory: DatabaseDriverFactory): SuvMusicDatabase =
    SuvMusicDatabase(factory.createDriver())
