package com.example.aituner.audio

import kotlin.math.abs

/**
 * Pitch detector using autocorrelation with refinement.
 *
 * Algorithm:
 * 1. Normalize PCM samples
 * 2. Compute autocorrelation (sum of products at each lag)
 * 3. Find the first significant peak → fundamental period
 * 4. Refine peak with parabolic interpolation for sub-sample accuracy
 * 5. Convert period to frequency
 *
 * Frequency range: 60 Hz (E2) to 500 Hz (B4+) — covers all guitar strings.
 */
object PitchDetector {

    /** Samples per second */
    const val SAMPLE_RATE = 44100

    /**
     * Detect the fundamental frequency from a buffer of PCM audio samples.
     *
     * @param samples Float array of PCM samples (should be already normalized to [-1, 1])
     * @return Detected frequency in Hz, or 0f if no pitch detected
     */
    fun detectPitch(samples: FloatArray): Float {
        if (samples.size < 2048) return 0f

        // 1. Check signal energy — too quiet = no signal
        val energy = samples.sumOf { (it * it).toDouble() } / samples.size
        if (energy < 0.0001) return 0f

        // 2. Compute autocorrelation for lags corresponding to 60–500 Hz
        val minLag = SAMPLE_RATE / 500  // for 500 Hz
        val maxLag = SAMPLE_RATE / 60   // for 60 Hz
        val corr = autocorrelation(samples, minLag, maxLag)

        // 3. Find the first significant peak
        val peak = findFirstPeak(corr, minLag)
        if (peak < 0) return 0f

        // 4. Parabolic interpolation for sub-sample accuracy
        val refinedLag = parabolicInterpolation(corr, peak)

        // 5. Convert lag to frequency
        if (refinedLag <= 0f) return 0f
        return SAMPLE_RATE.toFloat() / refinedLag
    }

    /**
     * Confidence score for the detected pitch.
     */
    fun confidence(samples: FloatArray, detectedFreq: Float): Float {
        if (detectedFreq <= 0f) return 0f

        val energy = samples.sumOf { (it * it).toDouble() } / samples.size
        val energyScore = if (energy > 0.1) 1f else (energy.toFloat() / 0.1f).coerceIn(0f, 1f)

        // Check harmonic ratio — high harmonics suggest clean tone
        val minLag = SAMPLE_RATE / 500
        val maxLag = SAMPLE_RATE / 60
        val corr = autocorrelation(samples, minLag, maxLag)
        val maxCorr = corr.maxOrNull() ?: 0f
        val corrScore = maxCorr.coerceIn(0f, 1f)

        return (energyScore * 0.4f + corrScore * 0.6f)
    }

    // ─── Private ───

    private fun autocorrelation(samples: FloatArray, minLag: Int, maxLag: Int): FloatArray {
        val n = samples.size
        val actualMaxLag = minOf(maxLag, n - 1)
        val result = FloatArray(actualMaxLag + 1)

        for (lag in minLag..actualMaxLag) {
            var sum = 0.0
            for (i in 0 until n - lag) {
                sum += samples[i].toDouble() * samples[i + lag].toDouble()
            }
            result[lag] = (sum / (n - lag)).toFloat()
        }
        return result
    }

    /**
     * Find the first significant peak in autocorrelation.
     * Returns the index (lag) of the peak, or -1 if not found.
     */
    private fun findFirstPeak(corr: FloatArray, minLag: Int): Int {
        if (corr.size <= minLag + 2) return -1

        val startIdx = minLag
        val endIdx = corr.size - 2

        // Find global max first to establish threshold
        val globalMax = corr.slice(startIdx..endIdx).maxOrNull() ?: return -1
        if (globalMax <= 0f) return -1

        // Threshold at 30% of global max — pick up weaker signals
        val threshold = globalMax * 0.3f

        // Find first peak above threshold
        for (i in startIdx + 1 until endIdx) {
            if (corr[i] > threshold &&
                corr[i] >= corr[i - 1] &&
                corr[i] >= corr[i + 1]
            ) {
                return i
            }
        }
        return -1
    }

    /**
     * Parabolic interpolation to refine peak position to sub-sample accuracy.
     * Returns the interpolated lag position.
     */
    private fun parabolicInterpolation(corr: FloatArray, peak: Int): Float {
        if (peak <= 0 || peak >= corr.size - 1) return peak.toFloat()

        val y0 = abs(corr[peak - 1].toDouble())
        val y1 = abs(corr[peak].toDouble())
        val y2 = abs(corr[peak + 1].toDouble())

        val denom = (2 * (2 * y1 - y0 - y2))
        if (denom == 0.0) return peak.toFloat()

        return (peak + (y0 - y2) / denom).toFloat()
    }
}
