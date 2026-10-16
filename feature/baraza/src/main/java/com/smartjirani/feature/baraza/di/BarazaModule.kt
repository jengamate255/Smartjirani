package com.smartjirani.feature.baraza.di

import com.smartjirani.feature.baraza.BarazaRepository
import com.smartjirani.feature.baraza.BarazaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BarazaModule {
    @Binds
    @Singleton
    abstract fun bindBarazaRepository(impl: BarazaRepositoryImpl): BarazaRepository
}
