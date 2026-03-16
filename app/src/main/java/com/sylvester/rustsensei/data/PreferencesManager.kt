package com.sylvester.rustsensei.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sylvester.rustsensei.llm.InferenceConfig

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "rustsensei_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to plain SharedPreferences if encryption fails
        // (e.g., rooted device with broken KeyStore)
        Log.w("PreferencesManager", "Encrypted prefs failed, falling back to plain", e)
        context.getSharedPreferences("rustsensei_prefs", Context.MODE_PRIVATE)
    }

    fun saveInferenceConfig(config: InferenceConfig) {
        prefs.edit()
            .putFloat("temperature", config.temperature)
            .putFloat("top_p", config.topP)
            .putInt("max_tokens", config.maxTokens)
            .putInt("context_length", config.contextLength)
            .apply()
    }

    fun loadInferenceConfig(): InferenceConfig {
        val modelId = getSelectedModelId()
        val defaults = InferenceConfig.forModel(modelId)
        return InferenceConfig(
            temperature = prefs.getFloat("temperature", defaults.temperature),
            topP = prefs.getFloat("top_p", defaults.topP),
            maxTokens = prefs.getInt("max_tokens", defaults.maxTokens),
            contextLength = prefs.getInt("context_length", defaults.contextLength)
        )
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString("selected_model_id", modelId).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString("selected_model_id", "litert-1b-gemma") ?: "litert-1b-gemma"
    }

    // Content Versioning
    fun getContentVersion(): Int = prefs.getInt("content_version", 1)
    fun setContentVersion(version: Int) = prefs.edit().putInt("content_version", version).apply()

    // Recent Searches
    fun saveRecentSearches(searches: List<String>) {
        prefs.edit().putString("recent_searches", searches.take(10).joinToString(",")).apply()
    }

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString("recent_searches", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }
}
