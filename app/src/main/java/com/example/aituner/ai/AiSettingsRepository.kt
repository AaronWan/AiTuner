package com.example.aituner.ai

import com.example.aituner.AiTunerApp
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiSettingsStore: DataStore<Preferences> by preferencesDataStore("ai_settings")

@Singleton
class AiSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_BASE_URL = stringPreferencesKey("ai_base_url")
        private val KEY_API_KEY = stringPreferencesKey("ai_api_key")
        private val KEY_MODEL = stringPreferencesKey("ai_model")

        /** No default — user must configure manually */
        val DEFAULT_CONFIG = AiConfig(
            providerId = AiProviderId.CUSTOM,
            baseUrl = "",
            apiKey = "",
            model = ""
        )
    }

    /**
     * Emits current config, falling back to DEFAULT_CONFIG if nothing saved.
     */
    val config: Flow<AiConfig> = context.aiSettingsStore.data.map { prefs ->
        val provider = prefs[KEY_PROVIDER]
        AiTunerApp.logDebug("Config", "loaded: provider=$provider, baseUrl=${prefs[KEY_BASE_URL]}, model=${prefs[KEY_MODEL]}")
        
        if (provider == null) {
            android.util.Log.d("AiTuner", "No saved config, using DEFAULT")
            return@map DEFAULT_CONFIG
        }
        
        AiConfig(
            providerId = try {
                AiProviderId.valueOf(provider)
            } catch (_: Exception) {
                DEFAULT_CONFIG.providerId
            },
            baseUrl = prefs[KEY_BASE_URL] ?: defaultBaseUrl(provider),
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: defaultModel(provider)
        )
    }

    /**
     * Save the complete configuration at once.
     */
    suspend fun save(config: AiConfig) {
        context.aiSettingsStore.edit { prefs ->
            prefs[KEY_PROVIDER] = config.providerId.name
            prefs[KEY_BASE_URL] = config.baseUrl
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.model
        }
    }

    private fun defaultBaseUrl(providerName: String): String = when (providerName) {
        "OPENAI" -> "https://api.openai.com"
        "CLAUDE" -> "https://api.anthropic.com"
        "OLLAMA" -> "http://localhost:11434"
        "CUSTOM" -> "https://your-api.com"
        else -> "https://api.openai.com"
    }

    private fun defaultModel(providerName: String): String = when (providerName) {
        "OPENAI" -> "gpt-4o-mini"
        "CLAUDE" -> "claude-3-haiku-20240307"
        "OLLAMA" -> "llama3.2"
        "CUSTOM" -> "gpt-3.5-turbo"
        else -> "gpt-4o-mini"
    }
}
