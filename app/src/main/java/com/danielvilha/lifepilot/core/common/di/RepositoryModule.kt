package com.danielvilha.lifepilot.core.common.di

import com.danielvilha.lifepilot.core.database.TaskRepositoryImpl
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        implementation: TaskRepositoryImpl
    ): TaskRepository
}