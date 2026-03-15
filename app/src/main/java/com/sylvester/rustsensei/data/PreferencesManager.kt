package com.sylvester.rustsensei.data

import android.content.Context
import android.content.SharedPreferences
import com.sylvester.rustsensei.llm.InferenceConfig

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rustsensei_prefs", Context.MODE_PRIVATE)

    fun saveInferenceConfig(config: InferenceConfig) {
        prefs.edit()
            .putFloat("temperature", config.temperature)
            .putFloat("top_p", config.topP)
            .putInt("max_tokens", config.maxTokens)
            .putInt("context_length", config.contextLength)
            .apply()
    }

    fun loadInferenceConfig(): InferenceConfig {
        return InferenceConfig(
            temperature = prefs.getFloat("temperature", 0.7f),
            topP = prefs.getFloat("top_p", 0.9f),
            maxTokens = prefs.getInt("max_tokens", 512),
            contextLength = prefs.getInt("context_length", 4096)
        )
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString("selected_model_id", modelId).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString("selected_model_id", "qwen3-4b") ?: "qwen3-4b"
    }
}
