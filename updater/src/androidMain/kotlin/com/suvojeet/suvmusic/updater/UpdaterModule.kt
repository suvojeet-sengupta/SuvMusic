package com.suvojeet.suvmusic.updater

import com.suvojeet.suvmusic.providers.lyrics.createLyricsHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdaterModule {

    @Provides
    @Singleton
    fun provideUpdateChecker(): UpdateChecker = UpdateChecker(createLyricsHttpClient())
}
