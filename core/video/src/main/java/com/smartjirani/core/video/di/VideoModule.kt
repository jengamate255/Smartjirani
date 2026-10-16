package com.smartjirani.core.video.di

import com.smartjirani.core.video.VideoConferenceService
import com.smartjirani.core.video.VideoConferenceServiceImpl
import com.smartjirani.core.video.VoiceCallService
import com.smartjirani.core.video.VoiceCallServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VideoModule {
    @Binds
    @Singleton
    abstract fun bindVoiceCallService(impl: VoiceCallServiceImpl): VoiceCallService

    @Binds
    @Singleton
    abstract fun bindVideoConferenceService(impl: VideoConferenceServiceImpl): VideoConferenceService
}
