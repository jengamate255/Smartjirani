package com.smartjirani.feature.profile.di

import com.smartjirani.feature.profile.ProfileExtrasRepository
import com.smartjirani.feature.profile.ProfileExtrasRepositoryImpl
import com.smartjirani.feature.profile.ProfileRepository
import com.smartjirani.feature.profile.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindProfileExtrasRepository(impl: ProfileExtrasRepositoryImpl): ProfileExtrasRepository
}
