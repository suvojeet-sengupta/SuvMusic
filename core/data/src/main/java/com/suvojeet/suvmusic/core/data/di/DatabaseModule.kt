package com.suvojeet.suvmusic.core.data.di

import android.content.Context
import androidx.room.Room
import com.suvojeet.suvmusic.core.data.local.AppDatabase
import com.suvojeet.suvmusic.core.data.local.dao.LibraryDao
import com.suvojeet.suvmusic.core.data.local.dao.ListeningHistoryDao
import com.suvojeet.suvmusic.core.data.local.dao.DislikedItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "suvmusic_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideLibraryDao(database: AppDatabase): LibraryDao {
        return database.libraryDao()
    }
    
    @Provides
    fun provideListeningHistoryDao(database: AppDatabase): ListeningHistoryDao {
        return database.listeningHistoryDao()
    }

    @Provides
    fun provideDislikedItemDao(database: AppDatabase): DislikedItemDao {
        return database.dislikedItemDao()
    }
}
