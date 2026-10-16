package com.smartjirani.core.audio

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AudioPlayer {
        private var player: MediaPlayer? = null
        private var currentFilePath: String? = null

        private val _isPlaying = MutableStateFlow(false)
        override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _playbackPosition = MutableStateFlow(0L)
        override val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

        private val _playbackDuration = MutableStateFlow(0L)
        override val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

        override fun play(filePath: String) {
            stop()
            currentFilePath = filePath

            player =
                MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    start()

                    _playbackDuration.value = duration.toLong()
                    _isPlaying.value = true

                    setOnCompletionListener {
                        _isPlaying.value = false
                        _playbackPosition.value = 0L
                    }
                }
        }

        override fun pause() {
            player?.pause()
            _isPlaying.value = false
        }

        override fun resume() {
            player?.start()
            _isPlaying.value = true
        }

        override fun stop() {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
            player = null
            currentFilePath = null
            _isPlaying.value = false
            _playbackPosition.value = 0L
            _playbackDuration.value = 0L
        }

        override fun seekTo(position: Long) {
            player?.seekTo(position.toInt())
            _playbackPosition.value = position
        }
    }
