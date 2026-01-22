package com.suvojeet.suvmusic.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerDataSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadDataSource
