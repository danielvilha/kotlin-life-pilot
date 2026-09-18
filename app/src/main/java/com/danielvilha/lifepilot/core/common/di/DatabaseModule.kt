package com.danielvilha.lifepilot.core.common.di

import android.content.Context
import androidx.room.Room
import com.danielvilha.lifepilot.core.database.AppDatabase
import com.danielvilha.lifepilot.core.database.TaskDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifepilot.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideTaskDao(
        database: AppDatabase
    ): TaskDao {
        return database.taskDao()
    }
}