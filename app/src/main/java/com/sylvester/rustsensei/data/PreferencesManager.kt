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
        return prefs.getString("selected_model_id", "qwen3-1.7b") ?: "qwen3-1.7b"
    }
}
