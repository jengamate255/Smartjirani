package com.smartjirani.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AudioRecorder {
        private var recorder: MediaRecorder? = null
        private var outputFile: File? = null
        private var startTime: Long = 0L

        private val _isRecording = MutableStateFlow(false)
        override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _recordingDuration = MutableStateFlow(0L)
        override val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

        override suspend fun startRecording() {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file

            recorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(64000)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }

            startTime = System.currentTimeMillis()
            _isRecording.value = true
        }

        override suspend fun stopRecording(): AudioResult? =
            try {
                recorder?.apply {
                    stop()
                    release()
                }
                recorder = null
                _isRecording.value = false

                val duration = System.currentTimeMillis() - startTime
                _recordingDuration.value = 0L

                outputFile?.let { file ->
                    AudioResult(
                        filePath = file.absolutePath,
                        durationMs = duration,
                        fileSize = file.length(),
                    )
                }
            } catch (e: Exception) {
                recorder?.release()
                recorder = null
                _isRecording.value = false
                _recordingDuration.value = 0L
                null
            }

        override suspend fun cancelRecording() {
            try {
                recorder?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {
            }
            recorder = null
            _isRecording.value = false
            _recordingDuration.value = 0L
            outputFile?.delete()
            outputFile = null
        }
    }
