package com.suvojeet.suvmusic.updater

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdaterModule {

    @Provides
    @Singleton
    fun provideUpdateChecker(okHttpClient: OkHttpClient): UpdateChecker {
        return UpdateChecker(okHttpClient)
    }
}
