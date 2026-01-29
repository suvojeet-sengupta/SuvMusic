package com.suvojeet.suvmusic.lastfm.di

import com.suvojeet.suvmusic.lastfm.LastFmClient
import com.suvojeet.suvmusic.lastfm.LastFmConfig
import com.suvojeet.suvmusic.lastfm.LastFmConfigImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LastFmModule {

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
