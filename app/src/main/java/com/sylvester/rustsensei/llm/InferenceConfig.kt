package com.sylvester.rustsensei.llm

import android.content.Context
import android.os.BatteryManager

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 384,
    val contextLength: Int = 2048
) {
    companion object {
        // Optimization #9: Battery threshold below which we reduce inference workload
        private const val LOW_BATTERY_THRESHOLD = 20

        fun forModel(modelId: String): InferenceConfig {
            return when (modelId) {
                "litert-0.6b" -> InferenceConfig(
                    maxTokens = 256,
                    contextLength = 2048
                )
                "litert-1b-gemma" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 2048
                )
                "litert-1.7b" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 2048
                )
                else -> InferenceConfig()
            }
        }

        /**
         * Optimization #9: Returns battery percentage (0-100), or -1 if unavailable.
         */
        fun getBatteryLevel(context: Context): Int {
            return try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            } catch (e: Exception) {
                -1
            }
        }

        /**
         * Optimization #9: Reduce context window and max tokens when battery is low
         * to halve inference compute and extend battery life.
         */
        fun adjustForBattery(config: InferenceConfig, context: Context): InferenceConfig {
            val batteryPct = getBatteryLevel(context)
            return if (batteryPct in 0 until LOW_BATTERY_THRESHOLD) {
                config.copy(
                    contextLength = (config.contextLength / 2).coerceAtLeast(512),
                    maxTokens = (config.maxTokens / 2).coerceAtLeast(128)
                )
            } else {
                config
            }
        }
    }
}
