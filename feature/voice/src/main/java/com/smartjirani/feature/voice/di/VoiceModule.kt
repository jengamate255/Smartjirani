package com.smartjirani.feature.voice.di

import com.smartjirani.feature.voice.VoiceMessageRepository
import com.smartjirani.feature.voice.VoiceMessageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {
    @Binds
    @Singleton
    abstract fun bindVoiceMessageRepository(impl: VoiceMessageRepositoryImpl): VoiceMessageRepository
}
