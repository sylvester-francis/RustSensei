package com.sylvester.rustsensei.di

import com.sylvester.rustsensei.network.OkHttpPlaygroundService
import com.sylvester.rustsensei.network.RustPlaygroundService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindPlaygroundService(impl: OkHttpPlaygroundService): RustPlaygroundService
}
