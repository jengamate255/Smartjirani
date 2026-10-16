package com.smartjirani.feature.posts.di

import com.smartjirani.feature.posts.PostsRepository
import com.smartjirani.feature.posts.PostsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostsModule {
    @Binds
    @Singleton
    abstract fun bindPostsRepository(impl: PostsRepositoryImpl): PostsRepository
}
