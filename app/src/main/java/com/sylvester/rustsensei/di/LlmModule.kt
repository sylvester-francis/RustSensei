package com.sylvester.rustsensei.di

import android.content.Context
import com.sylvester.rustsensei.llm.LiteRtEngine
import com.sylvester.rustsensei.llm.ModelManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {
    @Provides @Singleton
    fun provideLiteRtEngine(@ApplicationContext context: Context): LiteRtEngine = LiteRtEngine(context)

    @Provides @Singleton
    fun provideModelManager(@ApplicationContext context: Context): ModelManager = ModelManager(context)
}
