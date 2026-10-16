package com.smartjirani.feature.neighborhood.di

import com.smartjirani.feature.neighborhood.NeighborhoodRepository
import com.smartjirani.feature.neighborhood.NeighborhoodRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NeighborhoodModule {
    @Binds
    @Singleton
    abstract fun bindNeighborhoodRepository(impl: NeighborhoodRepositoryImpl): NeighborhoodRepository
}
