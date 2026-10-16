package com.smartjirani.core.realtime.di

import com.smartjirani.core.realtime.RealtimeService
import com.smartjirani.core.realtime.RealtimeServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {
    @Binds
    @Singleton
    abstract fun bindRealtimeService(impl: RealtimeServiceImpl): RealtimeService
}
