package com.example.aituner.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aituner.audio.NoteFrequency
import com.example.aituner.audio.TunerEngine
import com.example.aituner.audio.TunerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TunerUiState(
    val isListening: Boolean = false,
    val frequency: Float = 0f,
    val noteName: String = "",
    val centsOffset: Float = 0f,
    val confidence: Float = 0f,
    val isSilent: Boolean = true,
    // Auto-detect the target string based on detected note
    val targetStringIndex: Int = -1,  // 0-5 for guitar strings
    val targetStringName: String = "",
    val targetFrequency: Float = 0f,
    val tuningStatus: TuningStatus = TuningStatus.NoSignal
)

enum class TuningStatus {
    NoSignal,
    TooLow,
    Close,
    InTune,
    TooHigh
}

@HiltViewModel
class TunerViewModel @Inject constructor(
    private val tunerEngine: TunerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var engineJob: Job? = null

    fun startTuner() {
        if (engineJob?.isActive == true) return

        engineJob = viewModelScope.launch {
            tunerEngine.start().collect { result ->
                _uiState.update { state ->
                    when (result) {
                        is TunerResult.Silence -> {
                            state.copy(
                                isSilent = true,
                                isListening = true,
                                frequency = 0f,
                                noteName = "",
                                confidence = 0f,
                                tuningStatus = TuningStatus.NoSignal
                            )
                        }
                        is TunerResult.NoteDetected -> {
                            val targetIdx = findTargetString(result.note.midiNumber)
                            val status = if (result.isInTune) {
                                TuningStatus.InTune
                            } else if (result.centsOffset > 5) {
                                TuningStatus.TooHigh
                            } else if (result.centsOffset < -5) {
                                TuningStatus.TooLow
                            } else {
                                TuningStatus.Close
                            }

                            state.copy(
                                isSilent = false,
                                isListening = true,
                                frequency = result.frequency,
                                noteName = result.note.noteName,
                                centsOffset = result.centsOffset,
                                confidence = result.confidence,
                                targetStringIndex = targetIdx,
                                targetStringName = if (targetIdx >= 0)
                                    NoteFrequency.STRING_NAMES[targetIdx] else "",
                                targetFrequency = if (targetIdx >= 0)
                                    NoteFrequency.GUITAR_STANDARD[targetIdx].frequency else 0f,
                                tuningStatus = status
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopTuner() {
        engineJob?.cancel()
        tunerEngine.stop()
        _uiState.update { it.copy(isListening = false, isSilent = true, tuningStatus = TuningStatus.NoSignal) }
    }

    /**
     * Auto-match the detected MIDI note to the closest guitar string.
     */
    private fun findTargetString(detectedMidi: Float): Int {
        val targets = NoteFrequency.GUITAR_STANDARD
        var bestIdx = -1
        var bestDist = Float.MAX_VALUE

        for ((idx, note) in targets.withIndex()) {
            val dist = kotlin.math.abs(detectedMidi - note.midiNumber)
            if (dist < bestDist && dist <= 6f) {  // within half an octave
                bestDist = dist
                bestIdx = idx
            }
        }
        return bestIdx
    }
}
