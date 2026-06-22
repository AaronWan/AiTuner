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
 * Anthropic Claude provider using the Messages API.
 */
class ClaudeProvider(
    private val config: AiConfig,
    private val gson: Gson = Gson()
) : AiProvider {

    override val name = "Claude"
    override val providerId = AiProviderId.CLAUDE

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(messages: List<AiMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Separate system message from conversation
                var system = ""
                val conversation = mutableListOf<Map<String, Any>>()

                for (msg in messages) {
                    when (msg.role) {
                        AiMessage.Role.SYSTEM -> system = msg.content
                        AiMessage.Role.USER -> conversation.add(
                            mapOf("role" to "user", "content" to msg.content)
                        )
                        AiMessage.Role.ASSISTANT -> conversation.add(
                            mapOf("role" to "assistant", "content" to msg.content)
                        )
                    }
                }

                val body = mutableMapOf<String, Any>(
                    "model" to config.model,
                    "max_tokens" to 1024,
                    "messages" to conversation
                )
                if (system.isNotEmpty()) {
                    body["system"] = system
                }

                val request = Request.Builder()
                    .url("${config.baseUrl}/v1/messages")
                    .post(gson.toJson(body).toRequestBody(JSON))
                    .addHeader("x-api-key", config.apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
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
                val content = json["content"] as? List<Map<String, Any>>
                val text = content?.firstOrNull()?.get("text") as? String ?: ""

                Result.success(text.trim())
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
