package com.example.aituner.ai.provider

import com.example.aituner.ai.AiConfig
import com.example.aituner.ai.AiMessage
import com.example.aituner.ai.AiProvider
import com.example.aituner.ai.AiProviderId
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ollama local provider — runs completely offline.
 * Defaults to http://localhost:11434.
 */
class OllamaProvider(
    private val config: AiConfig,
    private val gson: Gson = Gson()
) : AiProvider {

    override val name = "Ollama (本地)"
    override val providerId = AiProviderId.OLLAMA

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // local models can be slow
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(messages: List<AiMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Ollama uses the OpenAI-compatible chat format
                val body = mapOf(
                    "model" to config.model,
                    "messages" to messages.map { msg ->
                        mapOf(
                            "role" to when (msg.role) {
                                AiMessage.Role.SYSTEM -> "system"
                                AiMessage.Role.USER -> "user"
                                AiMessage.Role.ASSISTANT -> "assistant"
                            },
                            "content" to msg.content
                        )
                    },
                    "stream" to false,
                    "options" to mapOf(
                        "temperature" to 0.7
                    )
                )

                val request = Request.Builder()
                    .url("${config.baseUrl}/v1/chat/completions")
                    .post(gson.toJson(body).toRequestBody(JSON))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("${response.code}: $responseBody")
                    )
                }

                val json = gson.fromJson(responseBody, Map::class.java)
                val choices = json["choices"] as? List<Map<String, Any>>
                val message = choices?.firstOrNull()?.get("message") as? Map<*, *>
                val content = message?.get("content") as? String ?: ""

                Result.success(content.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun chatStream(messages: List<AiMessage>): Flow<String> = flow {
        CoroutineScope(Dispatchers.IO).launch {
            chat(messages).fold(
                onSuccess = { emit(it) },
                onFailure = { throw it }
            )
        }
    }
}
