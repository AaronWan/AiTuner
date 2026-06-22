package com.example.aituner.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aituner.AiTunerApp
import com.example.aituner.ai.*
import com.example.aituner.audio.NoteFrequency
import com.example.aituner.audio.TunerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class AiChatMessage(
    val content: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false  // true while tokens are still arriving
)

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastDetectedNote: String? = null,
    val lastDetectedFreq: Float = 0f,
    val lastCentsOffset: Float = 0f
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiSettingsRepo: AiSettingsRepository,
    private val providerFactory: AiProviderFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val conversation = mutableListOf<AiMessage>(
        AiMessage(AiMessage.Role.SYSTEM, SYSTEM_PROMPT)
    )

    private var streamJob: Job? = null

    fun updateTunerContext(result: TunerResult) {
        if (result is TunerResult.NoteDetected) {
            _uiState.update {
                it.copy(
                    lastDetectedNote = result.note.noteName,
                    lastDetectedFreq = result.frequency,
                    lastCentsOffset = result.centsOffset
                )
            }
        }
    }

    fun sendMessage(text: String) {
        // Cancel any ongoing stream
        streamJob?.cancel()

        _uiState.update {
            it.copy(
                messages = it.messages + AiChatMessage(text, isUser = true),
                isLoading = true,
                error = null
            )
        }

        val contextNote = _uiState.value.let { state ->
            if (state.lastDetectedNote != null) {
                "\n[Tuning: ${state.lastDetectedNote} " +
                "offset ${state.lastCentsOffset.toInt()} cents]"
            } else ""
        }

        conversation.add(AiMessage(AiMessage.Role.USER, text + contextNote))

        // Start with an empty streaming message
        _uiState.update {
            it.copy(
                messages = it.messages + AiChatMessage("", isUser = false, isStreaming = true)
            )
        }

        val targetMsgIndex = _uiState.value.messages.size - 1

        streamJob = viewModelScope.launch {
            try {
                val config = aiSettingsRepo.config.first()
                AiTunerApp.logDebug("Chat", "config: ${config.providerId}, ${config.baseUrl}, ${config.model}")
                val provider = providerFactory.create(config)
                AiTunerApp.logDebug("Chat", "provider: ${provider.name}")

                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(30_000L) {
                        provider.chat(conversation.toList())
                    }
                }

                result.fold(
                    onSuccess = { reply ->
                        val updated = _uiState.value.messages.toMutableList()
                        if (targetMsgIndex in updated.indices) {
                            updated[targetMsgIndex] = updated[targetMsgIndex].copy(
                                content = reply,
                                isStreaming = false
                            )
                        }
                        _uiState.update { it.copy(messages = updated, isLoading = false) }
                        conversation.add(AiMessage(AiMessage.Role.ASSISTANT, reply))
                    },
                    onFailure = { e ->
                        _uiState.update { state ->
                            val updated = state.messages.toMutableList()
                            if (targetMsgIndex in updated.indices) {
                                updated.removeAt(targetMsgIndex)
                            }
                            state.copy(messages = updated, isLoading = false,
                                error = "Error: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                AiTunerApp.logDebug("Chat", "Error: ${e.message}")
                _uiState.update { state ->
                    val updated = state.messages.toMutableList()
                    if (targetMsgIndex in updated.indices) {
                        updated.removeAt(targetMsgIndex)
                    }
                    state.copy(
                        messages = updated,
                        isLoading = false,
                        error = "Error: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private val SYSTEM_PROMPT = """
You are a professional guitar tuning and music theory assistant. Help with:
1. Tuning guidance — standard, drop, open, DADGAD, etc.
2. Music theory — scales, chords, intervals
3. Gear — guitar care, strings, pickups
4. Technique — fretting, picking, strumming

Answer in the user's language, be concise and friendly.
        """.trimIndent()
    }
}
