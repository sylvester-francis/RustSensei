package com.sylvester.rustsensei.di

import android.content.Context
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.content.RagRetriever
import com.sylvester.rustsensei.data.ChatDao
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideChatRepository(chatDao: ChatDao): ChatRepository = ChatRepository(chatDao)

    @Provides @Singleton
    fun provideProgressRepository(progressDao: ProgressDao): ProgressRepository = ProgressRepository(progressDao)

    @Provides @Singleton
    fun provideContentRepository(@ApplicationContext context: Context): ContentRepository = ContentRepository(context)

    @Provides @Singleton
    fun provideRagRetriever(@ApplicationContext context: Context): RagRetriever = RagRetriever(context)

    @Provides @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager = PreferencesManager(context)
}
