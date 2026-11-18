package com.nexus.gamelauncher.di

import android.content.Context
import androidx.room.Room
import com.nexus.gamelauncher.feature.library.LibraryDatabase
import com.nexus.gamelauncher.feature.library.MIGRATION_1_2
import com.nexus.gamelauncher.feature.library.MIGRATION_2_3
import com.nexus.gamelauncher.feature.library.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): LibraryDatabase =
        Room.databaseBuilder(context, LibraryDatabase::class.java, "nexus.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun libraryDao(database: LibraryDatabase) = database.libraryDao()
}
