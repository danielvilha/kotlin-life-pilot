package com.danielvilha.lifepilot.core.common.di

import com.danielvilha.lifepilot.domain.repository.AiTaskParser
import com.danielvilha.lifepilot.feature.ai.data.FakeAiTaskParser
import com.danielvilha.lifepilot.feature.ai.data.GeminiTaskParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiTaskParser(
        implementation: FakeAiTaskParser
    ): AiTaskParser

//    @Binds
//    @Singleton
//    abstract fun bindAiTaskParser(
//        implementation: GeminiTaskParser
//    ): AiTaskParser
}