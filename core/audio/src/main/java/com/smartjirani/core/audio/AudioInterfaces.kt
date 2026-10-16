package com.smartjirani.core.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {
    val isRecording: StateFlow<Boolean>
    val recordingDuration: StateFlow<Long>

    suspend fun startRecording()

    suspend fun stopRecording(): AudioResult?

    suspend fun cancelRecording()
}

data class AudioResult(
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
)

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val playbackPosition: StateFlow<Long>
    val playbackDuration: StateFlow<Long>

    fun play(filePath: String)

    fun pause()

    fun resume()

    fun stop()

    fun seekTo(position: Long)
}
