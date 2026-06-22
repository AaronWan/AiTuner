package com.example.aituner.ai

/**
 * Generic AI provider interface — plug in any LLM backend.
 */
interface AiProvider {

    /** Human-readable name (shown in settings) */
    val name: String

    /** API provider identifier */
    val providerId: AiProviderId

    /**
     * Send a chat completion request.
     *
     * @param messages Conversation history — first message should set the role/context
     * @return The AI's response text, or failure
     */
    suspend fun chat(messages: List<AiMessage>): Result<String>

    /**
     * Send a chat completion request with SSE streaming.
     * Emits content tokens as they arrive from the server.
     */
    fun chatStream(messages: List<AiMessage>): kotlinx.coroutines.flow.Flow<String>
}

enum class AiProviderId {
    OPENAI,     // OpenAI API (ChatGPT, GPT-4)
    CLAUDE,     // Anthropic Claude
    OLLAMA,     // Local Ollama (offline capable)
    CUSTOM      // Custom endpoint
}

data class AiMessage(
    val role: Role,
    val content: String
) {
    enum class Role { SYSTEM, USER, ASSISTANT }
}

data class AiConfig(
    val providerId: AiProviderId,
    val baseUrl: String,       // API endpoint
    val apiKey: String,        // API key (or empty for Ollama)
    val model: String          // e.g. "gpt-4o", "claude-3-opus", "llama3"
)
