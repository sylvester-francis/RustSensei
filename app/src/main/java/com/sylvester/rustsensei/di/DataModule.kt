package com.sylvester.rustsensei.di

import android.content.Context
import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.content.RagRetriever
import com.sylvester.rustsensei.data.AppDatabase
import com.sylvester.rustsensei.data.ChatDao
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.InferenceConfigProvider
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindContentProvider(impl: ContentRepository): ContentProvider

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            AppDatabase.getDatabase(context)

        @Provides
        fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

        @Provides
        fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()

        @Provides
        fun provideFlashCardDao(db: AppDatabase): FlashCardDao = db.flashCardDao()

        @Provides
        @Singleton
        fun provideChatRepository(chatDao: ChatDao): ChatRepository =
            ChatRepository(chatDao)

        @Provides
        @Singleton
        fun provideProgressRepository(progressDao: ProgressDao): ProgressRepository =
            ProgressRepository(progressDao)

        @Provides
        @Singleton
        fun provideContentRepository(@ApplicationContext context: Context): ContentRepository =
            ContentRepository(context)

        @Provides
        @Singleton
        fun provideRagRetriever(@ApplicationContext context: Context): RagRetriever =
            RagRetriever(context)

        @Provides
        @Singleton
        fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager =
            PreferencesManager(context)

        @Provides
        @Singleton
        fun provideInferenceConfigProvider(preferencesManager: PreferencesManager): InferenceConfigProvider =
            preferencesManager
    }
}
