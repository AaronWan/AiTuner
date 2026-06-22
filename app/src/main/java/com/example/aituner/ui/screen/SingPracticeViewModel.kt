package com.example.aituner.ui.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aituner.ai.*
import com.example.aituner.audio.NoteFrequency
import com.example.aituner.audio.TunerEngine
import com.example.aituner.audio.TunerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class SingPracticeState(
    val phase: Phase = Phase.IDLE,
    val score: Score? = null,
    val currentIndex: Int = 0,
    val currentResult: ScoringResult? = null,
    val results: List<ScoringResult> = emptyList(),
    val currentFreq: Float = 0f,
    val currentNoteName: String = "",
    val countdown: Int = 0,
    val stats: PracticeStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val generationPrompt: String = "",
    val generatedScore: Score? = null
)

enum class Phase {
    IDLE, GENERATING, GENERATED, COUNTDOWN, SINGING, REVIEW
}

data class PracticeStats(
    val totalNotes: Int,
    val perfect: Int, val good: Int, val flat: Int, val sharp: Int, val missed: Int,
    val accuracy: Float
)

private const val TAG = "SingPractice"

@HiltViewModel
class SingPracticeViewModel @Inject constructor(
    private val tunerEngine: TunerEngine,
    private val aiSettingsRepo: AiSettingsRepository,
    private val providerFactory: AiProviderFactory,
    private val presetRepo: PresetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SingPracticeState())
    val state: StateFlow<SingPracticeState> = _state.asStateFlow()

    private var tunerJob: kotlinx.coroutines.Job? = null
    private var singJob: kotlinx.coroutines.Job? = null
    private var countdownJob: kotlinx.coroutines.Job? = null

    /** Available presets loaded from assets/presets/ */
    val presetList: List<PresetRepository.PresetInfo> get() = presetRepo.listPresets()

    /** Load a preset score by its id (filename without .md). */
    fun loadPreset(presetId: String): Boolean {
        val score = presetRepo.loadPreset(presetId) ?: return false
        _state.update {
            it.copy(
                phase = Phase.GENERATED,
                generatedScore = score,
                isLoading = false,
                generationPrompt = score.title
            )
        }
        return true
    }

    fun generateScore(prompt: String) {
        Log.d(TAG, "generateScore() called with prompt: $prompt")
        _state.update { it.copy(phase = Phase.GENERATING, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Reading AI config...")
                val config = aiSettingsRepo.config.first()
                Log.d(TAG, "Config: provider=${config.providerId}, model=${config.model} baseUrl=${config.baseUrl}")

                val provider = providerFactory.create(config)
                val fullPrompt = SCORE_GENERATION_PROMPT + "\n\nUser request: $prompt"
                val messages = listOf(AiMessage(AiMessage.Role.USER, fullPrompt))

                // Use non-streaming for reliability on emulator
                Log.d(TAG, "Calling chat() for score generation...")
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(30_000L) {
                        provider.chat(messages)
                    }
                }

                result.fold(
                    onSuccess = { raw ->
                        Log.d(TAG, "chat() returned ${raw.length} chars: ${raw.take(200)}")

                        val score = try {
                            ScoreParser.parse(raw)
                        } catch (e: Exception) {
                            Log.e(TAG, "Parse crash", e)
                            _state.update {
                                it.copy(phase = Phase.IDLE, isLoading = false,
                                    error = "Parse error: ${e.message}")
                            }
                            return@launch
                        }

                        if (score.notes.isEmpty()) {
                            _state.update {
                                it.copy(phase = Phase.IDLE, isLoading = false,
                                    error = "Could not parse score")
                            }
                            return@launch
                        }

                        _state.update {
                            it.copy(phase = Phase.GENERATED, generatedScore = score,
                                isLoading = false, generationPrompt = prompt)
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "API call failed", e)
                        _state.update {
                            it.copy(phase = Phase.IDLE, isLoading = false,
                                error = "API error: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _state.update {
                    it.copy(phase = Phase.IDLE, isLoading = false,
                        error = "Error: ${e.message}")
                }
            }
        }
    }

    fun startPractice() {
        val score = _state.value.generatedScore ?: return
        _state.update {
            it.copy(phase = Phase.COUNTDOWN, score = score, currentIndex = 0,
                results = List(score.notes.size) { ScoringResult(score.notes[it], verdict = Verdict.PENDING) },
                currentResult = null, stats = null, countdown = 3)
        }
        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _state.update { it.copy(countdown = i) }
                kotlinx.coroutines.delay(1000)
            }
            _state.update { it.copy(countdown = 0) }
            startSinging()
        }
    }

    private fun startSinging() {
        val score = _state.value.score ?: return
        _state.update { it.copy(phase = Phase.SINGING) }

        tunerJob = viewModelScope.launch {
            tunerEngine.start().collect { result ->
                val freq = if (result is TunerResult.NoteDetected) result.frequency else 0f
                val noteName = if (result is TunerResult.NoteDetected) result.note.noteName else ""
                _state.update { it.copy(currentFreq = freq, currentNoteName = noteName) }
            }
        }

        singJob = viewModelScope.launch {
            val notes = score.notes
            for (i in notes.indices) {
                _state.update { it.copy(currentIndex = i) }
                val target = notes[i]
                val elapsed = (target.durationSec * 1000).toLong()
                val steps = (elapsed / 100).coerceAtLeast(1)
                var bestCents = Float.MAX_VALUE
                var bestFreq = 0f
                var hasSound = false

                for (step in 0 until steps.toInt()) {
                    kotlinx.coroutines.delay(100)
                    val currentFreq = _state.value.currentFreq
                    if (currentFreq > 0f) {
                        hasSound = true
                        val offset = kotlin.math.abs(
                            NoteFrequency.centsFromTarget(currentFreq,
                                NoteFrequency.TuningNote(target.noteName, target.frequency, 0f))
                        )
                        if (offset < bestCents) {
                            bestCents = offset
                            bestFreq = currentFreq
                        }
                        _state.update {
                            val verdict = when {
                                offset <= 10f -> Verdict.PERFECT
                                offset <= 25f -> Verdict.GOOD
                                else -> Verdict.SHARP
                            }
                            it.copy(currentResult = ScoringResult(target, currentFreq, "", offset, verdict))
                        }
                    }
                }

                val sungFreq = if (hasSound) bestFreq else null
                val finalResult = evaluateNote(sungFreq, target)
                val updatedResults = _state.value.results.toMutableList()
                updatedResults[i] = finalResult
                _state.update { it.copy(results = updatedResults, currentResult = finalResult) }
            }
            endPractice()
        }
    }

    private fun endPractice() {
        tunerJob?.cancel()
        tunerEngine.stop()
        val results = _state.value.results
        val stats = PracticeStats(
            totalNotes = results.size,
            perfect = results.count { it.verdict == Verdict.PERFECT },
            good = results.count { it.verdict == Verdict.GOOD },
            flat = results.count { it.verdict == Verdict.FLAT },
            sharp = results.count { it.verdict == Verdict.SHARP },
            missed = results.count { it.verdict == Verdict.MISSED },
            accuracy = if (results.isNotEmpty())
                results.count { it.verdict == Verdict.PERFECT || it.verdict == Verdict.GOOD }
                    .toFloat() / results.size * 100f else 0f
        )
        _state.update { it.copy(phase = Phase.REVIEW, stats = stats, currentFreq = 0f, currentNoteName = "") }
    }

    fun stopPractice() {
        countdownJob?.cancel()
        singJob?.cancel()
        tunerJob?.cancel()
        tunerEngine.stop()
        _state.update { it.copy(phase = Phase.IDLE, currentFreq = 0f, currentNoteName = "", currentResult = null) }
    }

    fun reset() {
        stopPractice()
        _state.value = SingPracticeState()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        val SCORE_GENERATION_PROMPT = """
You are a music education assistant. Generate a solfege practice score based on the user's request.

Rules:
1. Output plain text, one note or rest per line
2. Format: NoteName:Duration(seconds), e.g. C4:2
3. Rests use "-:seconds"
4. Note range: A3 to C5 (vocal range)
5. Total 16-32 notes — create a full melody, not just a fragment
6. Duration 1-2 seconds each
7. Make it musical: include patterns, repetition, and a satisfying ending
8. No explanatory text, no code blocks, no markdown

Example for "simple C major melody":
C4:1
D4:1
E4:1
C4:1
E4:1
F4:1
G4:2
-:1
G4:1
F4:1
E4:1
D4:1
C4:1
D4:1
E4:1
C4:2
-:1
C4:1
E4:1
G4:1
C5:1
G4:1
E4:1
C4:2

If the user requests a specific song, generate its simplified full melody.
        """.trimIndent()
    }
}
