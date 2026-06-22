package com.example.aituner.ai

import com.example.aituner.ai.provider.ClaudeProvider
import com.example.aituner.ai.provider.OllamaProvider
import com.example.aituner.ai.provider.OpenAIProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory that creates the appropriate [AiProvider] from config.
 */
@Singleton
class AiProviderFactory @Inject constructor() {

    fun create(config: AiConfig): AiProvider = when (config.providerId) {
        AiProviderId.OPENAI -> OpenAIProvider(config)
        AiProviderId.CLAUDE -> ClaudeProvider(config)
        AiProviderId.OLLAMA -> OllamaProvider(config)
        AiProviderId.CUSTOM -> OpenAIProvider(config) // CUSTOM assumes OpenAI-compatible API
    }
}
