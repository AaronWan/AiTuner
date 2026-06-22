package com.example.aituner.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine that captures microphone audio and emits pitch detection results.
 *
 * Sampling: 44100 Hz, 16-bit PCM mono
 * Buffer: 4096 samples (~93ms) — short enough for responsive tuning
 * Overlap: 50% — smoother updates
 */
@Singleton
class TunerEngine @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 4096  // ~93ms at 44100 Hz
    }

    private var audioRecord: AudioRecord? = null
    private var isRunning = false

    /**
     * Start listening and emit [TunerResult] values.
     * The flow emits when pitch is detected, or [TunerResult.Silence] when no signal.
     */
    fun start(): Flow<TunerResult> = callbackFlow {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(BUFFER_SIZE * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).also {
            if (it.state != AudioRecord.STATE_INITIALIZED) {
                close(null)
                return@callbackFlow
            }
            it.startRecording()
        }

        isRunning = true

        val readBuffer = ShortArray(BUFFER_SIZE)
        val floatBuffer = FloatArray(BUFFER_SIZE)

        while (isRunning) {
            val read = audioRecord?.read(readBuffer, 0, BUFFER_SIZE) ?: -1
            if (read <= 0) {
                trySend(TunerResult.Silence)
                continue
            }

            // Convert 16-bit PCM to normalized float [-1, 1]
            for (i in 0 until read) {
                floatBuffer[i] = readBuffer[i].toFloat() / 32768f
            }

            val freq = PitchDetector.detectPitch(floatBuffer)
            if (freq <= 0f) {
                trySend(TunerResult.Silence)
            } else {
                val result = NoteFrequency.findClosestNote(freq)
                val conf = PitchDetector.confidence(floatBuffer, freq)
                trySend(
                    TunerResult.NoteDetected(
                        frequency = freq,
                        note = result.closestNote,
                        centsOffset = result.centsOffset,
                        confidence = conf
                    )
                )
            }
        }

        awaitClose {
            stopInternal()
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        isRunning = false
    }

    private fun stopInternal() {
        isRunning = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        audioRecord?.release()
        audioRecord = null
    }
}

sealed class TunerResult {
    data object Silence : TunerResult()

    data class NoteDetected(
        val frequency: Float,
        val note: NoteFrequency.TuningNote,
        val centsOffset: Float,      // -50 to +50
        val confidence: Float = 0f   // 0 to 1
    ) : TunerResult() {

        /** True if within ±5 cents of perfect tuning */
        val isInTune: Boolean
            get() = kotlin.math.abs(centsOffset) <= 5f

        /** True if reasonably close (±15 cents) */
        val isClose: Boolean
            get() = kotlin.math.abs(centsOffset) <= 15f
    }
}
