package com.suvojeet.suvmusic.core.data.di

import com.suvojeet.suvmusic.core.data.repository.LibraryRepositoryImpl
import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindLibraryRepository(
        impl: LibraryRepositoryImpl
    ): LibraryRepository
}
