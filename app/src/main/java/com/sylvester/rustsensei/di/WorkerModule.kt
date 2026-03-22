package com.sylvester.rustsensei.di

import android.content.Context
import com.sylvester.rustsensei.work.ReminderScheduler
import com.sylvester.rustsensei.work.ReminderSchedulerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context
    ): ReminderScheduler = ReminderSchedulerImpl(context)
}
