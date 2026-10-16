package com.smartjirani.feature.ujamaa.di

import com.smartjirani.feature.ujamaa.MtaaMapRepository
import com.smartjirani.feature.ujamaa.MtaaMapRepositoryImpl
import com.smartjirani.feature.ujamaa.UjamaaRepository
import com.smartjirani.feature.ujamaa.UjamaaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UjamaaModule {
    @Binds
    @Singleton
    abstract fun bindUjamaaRepository(impl: UjamaaRepositoryImpl): UjamaaRepository

    @Binds
    @Singleton
    abstract fun bindMtaaMapRepository(impl: MtaaMapRepositoryImpl): MtaaMapRepository
}
