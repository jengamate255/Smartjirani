package com.smartjirani.core.audio.di

import com.smartjirani.core.audio.AudioPlayer
import com.smartjirani.core.audio.AudioPlayerImpl
import com.smartjirani.core.audio.AudioRecorder
import com.smartjirani.core.audio.AudioRecorderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindAudioRecorder(impl: AudioRecorderImpl): AudioRecorder

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: AudioPlayerImpl): AudioPlayer
}
