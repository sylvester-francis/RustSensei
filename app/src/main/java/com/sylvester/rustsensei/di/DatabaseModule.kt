package com.sylvester.rustsensei.di

import android.content.Context
import com.sylvester.rustsensei.data.AppDatabase
import com.sylvester.rustsensei.data.ChatDao
import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.ProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideFlashCardDao(db: AppDatabase): FlashCardDao = db.flashCardDao()
}
