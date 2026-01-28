package com.suvojeet.suvmusic.providers.di

import com.suvojeet.suvmusic.providers.lastfm.LastFmClient
import com.suvojeet.suvmusic.providers.lastfm.LastFmConfig
import com.suvojeet.suvmusic.providers.lastfm.LastFmConfigImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProvidersModule {

    @Binds
    @Singleton
    abstract fun bindLastFmConfig(impl: LastFmConfigImpl): LastFmConfig

    companion object {
        @Provides
        @Singleton
        fun provideLastFmClient(config: LastFmConfig): LastFmClient {
            return LastFmClient(config)
        }
    }
}
