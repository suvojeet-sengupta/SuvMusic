package com.suvojeet.suvmusic.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Top-level Koin module aggregator. During the Hilt -> Koin migration (Phase 1
 * of the KMP migration) bindings are moved here one slice at a time. While both
 * frameworks coexist this list may be empty or partially populated; production
 * DI still flows through Hilt until chunk 1d removes it.
 */
val koinAppModules: List<Module> = listOf(
    module {
        // Phase 1a: intentionally empty. Bindings land here in 1b.
    },
)
