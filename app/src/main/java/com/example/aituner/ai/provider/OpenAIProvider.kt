package com.example.aituner.ai.provider

import com.example.aituner.AiTunerApp
import com.example.aituner.ai.AiConfig
import com.example.aituner.ai.AiMessage
import com.example.aituner.ai.AiProvider
import com.example.aituner.ai.AiProviderId
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible provider with SSE streaming support.
 */
class OpenAIProvider(
    private val config: AiConfig,
    private val gson: Gson = Gson()
) : AiProvider {

    override val name = "OpenAI"
    override val providerId = AiProviderId.OPENAI

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    @Suppress("UNCHECKED_CAST")
    override suspend fun chat(messages: List<AiMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                AiTunerApp.logDebug("OpenAI", "Non-stream chat to ${config.baseUrl}, model=${config.model}")
                val body = buildRequestBody(messages, stream = false)
                val request = buildRequest(body)
                val call = client.newCall(request)
                val job = coroutineContext[Job]
                if (job != null) {
                    job.invokeOnCompletion { call.cancel() }
                }
                val response = call.execute()
                val responseBody = response.body?.string() ?: ""
                val httpCode = response.code
                AiTunerApp.logDebug("OpenAI", "Response code: $httpCode, body: ${responseBody.take(500)}")

                val content = try {
                    val root = gson.fromJson<Map<String, Any?>>(
                        responseBody,
                        object : TypeToken<Map<String, Any?>>() {}.type
                    )
                    extractContent(root)
                } catch (e: JsonSyntaxException) {
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("Server error $httpCode"))
                    }
                    return@withContext Result.success(responseBody.take(256))
                }

                if (content == null && httpCode >= 400) {
                    val errorMsg = try {
                        val err = gson.fromJson<Map<String, Any?>>(responseBody,
                            object : TypeToken<Map<String, Any?>>() {}.type)
                        val error = err?.get("error") as? Map<*, *>
                        error?.get("message") as? String ?: "Server error $httpCode"
                    } catch (_: Exception) { "Server error $httpCode" }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                Result.success((content ?: responseBody.take(256)).trim())
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(IOException("Error: ${e.message}", e))
            }
        }

    /**
     * SSE streaming — emits tokens as they arrive.
     * Runs on IO dispatcher via flow { } wrapper, avoiding callbackFlow awaitClose complexity.
     */
    override fun chatStream(messages: List<AiMessage>): Flow<String> = flow {
        withContext(Dispatchers.IO) {
            try {
                val body = buildRequestBody(messages, stream = true)
                val request = buildRequest(body)
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "HTTP ${response.code}"
                    AiTunerApp.logDebug("OpenAI", "API error: $errBody")
                    throw IOException(errBody)
                }

                val contentType = response.header("Content-Type") ?: "unknown"
                AiTunerApp.logDebug("OpenAI", "Response content-type: $contentType")

                val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return@withContext))
                var line: String?
                var tokenCount = 0
                var lineCount = 0
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    val l = line ?: continue
                    if (l.isEmpty()) continue
                    if (!l.startsWith("data: ")) {
                        if (lineCount <= 5) AiTunerApp.logDebug("OpenAI", "Skip line: ${l.take(100)}")
                        continue
                    }

                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val root = gson.fromJson<Map<String, Any?>>(
                            data,
                            object : TypeToken<Map<String, Any?>>() {}.type
                        )
                        val choices = root?.get("choices") as? List<*>
                        if (choices != null && choices.isNotEmpty()) {
                            val first = choices[0] as? Map<*, *>
                            val delta = first?.get("delta") as? Map<*, *>
                            val content = delta?.get("content") as? String
                            if (!content.isNullOrEmpty()) {
                                tokenCount++
                                emit(content)
                            }
                        }
                    } catch (_: Exception) {
                        // Skip malformed SSE lines
                    }
                }
                AiTunerApp.logDebug("OpenAI", "Stream complete: $tokenCount tokens, lines: $lineCount")
            } catch (e: Exception) {
                AiTunerApp.logDebug("OpenAI", "Stream error: ${e.message}")
                throw e
            }
        }
    }.let { it as Flow<String> }

    private fun buildRequestBody(messages: List<AiMessage>, stream: Boolean): String {
        val map = mapOf(
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
            "temperature" to 0.7,
            "max_tokens" to 1024,
            "stream" to stream
        )
        return gson.toJson(map)
    }

    private fun buildRequest(body: String): Request {
        val builder = Request.Builder()
            .url("${config.baseUrl}/v1/chat/completions")
            .post(body.toRequestBody(JSON))
            .addHeader("Content-Type", "application/json")
        if (config.apiKey.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer ${config.apiKey}")
        }
        return builder.build()
    }

    private fun extractContent(root: Map<String, Any?>): String? {
        val choices = root["choices"] as? List<*>
        if (choices != null && choices.isNotEmpty()) {
            val first = choices[0] as? Map<*, *>
            val message = first?.get("message") as? Map<*, *>
            val text = message?.get("text") ?: message?.get("content")
            return text?.toString()
        }
        val direct = root["content"] as? String
        if (direct != null) return direct
        return null
    }
}