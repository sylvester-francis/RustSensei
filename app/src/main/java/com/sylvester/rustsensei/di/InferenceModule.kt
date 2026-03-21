package com.sylvester.rustsensei.di

import android.content.Context
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.LiteRtEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelLifecycleManager
import com.sylvester.rustsensei.llm.ModelManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    abstract fun bindInferenceEngine(impl: LiteRtEngine): InferenceEngine

    @Binds
    abstract fun bindModelLifecycle(impl: ModelLifecycleManager): ModelLifecycle

    companion object {

        @Provides
        @Singleton
        fun provideLiteRtEngine(@ApplicationContext context: Context): LiteRtEngine =
            LiteRtEngine(context)

        @Provides
        @Singleton
        fun provideModelManager(@ApplicationContext context: Context): ModelManager =
            ModelManager(context)
    }
}
