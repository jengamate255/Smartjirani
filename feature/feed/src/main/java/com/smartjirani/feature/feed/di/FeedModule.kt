package com.smartjirani.feature.feed.di

import com.smartjirani.feature.feed.FeedExtrasRepository
import com.smartjirani.feature.feed.FeedExtrasRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedModule {
    @Binds
    @Singleton
    abstract fun bindFeedExtrasRepository(impl: FeedExtrasRepositoryImpl): FeedExtrasRepository
}
