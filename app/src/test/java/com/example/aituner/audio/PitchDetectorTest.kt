package com.example.aituner.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PitchDetectorTest {

    /**
     * Generate a sine wave at [frequency] Hz, sampled at [sampleRate] Hz,
     * with [duration] seconds. Normalized to [-1, 1].
     */
    private fun generateSineWave(frequency: Float, sampleRate: Int = 44100, duration: Float = 0.5f): FloatArray {
        val numSamples = (sampleRate * duration).toInt()
        return FloatArray(numSamples) { i ->
            sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }
    }

    @Test
    fun detectPitch_440Hz_sineWave_returnsNear440() {
        val samples = generateSineWave(440f)
        val result = PitchDetector.detectPitch(samples)
        assertTrue("Expected near 440 Hz, got $result", result in 430f..450f)
    }

    @Test
    fun detectPitch_100Hz_sineWave_returnsNear100() {
        val samples = generateSineWave(100f)
        val result = PitchDetector.detectPitch(samples)
        assertTrue("Expected near 100 Hz, got $result", result in 95f..105f)
    }

    @Test
    fun detectPitch_330Hz_sineWave_returnsNear330() {
        val samples = generateSineWave(330f)
        val result = PitchDetector.detectPitch(samples)
        assertTrue("Expected near 330 Hz, got $result", result in 320f..340f)
    }

    @Test
    fun detectPitch_lowEnergy_returnsZero() {
        // Almost silent signal
        val samples = FloatArray(4096) { 0.0001f }
        val result = PitchDetector.detectPitch(samples)
        assertEquals(0f, result)
    }

    @Test
    fun detectPitch_insufficientSamples_returnsZero() {
        val samples = FloatArray(1024) // less than 2048 minimum
        val result = PitchDetector.detectPitch(samples)
        assertEquals(0f, result)
    }

    @Test
    fun detectPitch_silence_returnsZero() {
        val samples = FloatArray(4096) // zeros, all silent
        val result = PitchDetector.detectPitch(samples)
        assertEquals(0f, result)
    }

    @Test
    fun confidence_440Hz_highConfidence() {
        val samples = generateSineWave(440f)
        val freq = PitchDetector.detectPitch(samples)
        val conf = PitchDetector.confidence(samples, freq)
        assertTrue("Expected high confidence, got $conf", conf > 0.5f)
    }

    @Test
    fun confidence_silence_lowConfidence() {
        val samples = FloatArray(4096)
        val conf = PitchDetector.confidence(samples, 0f)
        assertEquals(0f, conf)
    }

    @Test
    fun detectPitch_detectableRange_bassGuitarE2() {
        // E2 ≈ 82.4 Hz
        val samples = generateSineWave(82.41f, duration = 0.5f)
        val result = PitchDetector.detectPitch(samples)
        assertTrue("Expected near 82.4 Hz, got $result", result in 75f..90f)
    }

    @Test
    fun detectPitch_detectableRange_highB4() {
        // B4 ≈ 493.9 Hz
        val samples = generateSineWave(493.88f, duration = 0.3f)
        val result = PitchDetector.detectPitch(samples)
        assertTrue("Expected near 494 Hz, got $result", result in 480f..510f)
    }
}
