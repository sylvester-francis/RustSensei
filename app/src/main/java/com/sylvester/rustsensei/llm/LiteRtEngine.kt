package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LiteRtEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"
    }

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isLoaded = false

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                engine?.close()
                conversation?.close()

                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine
                isLoaded = true
                Log.i(TAG, "Model loaded: $modelPath (GPU)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "GPU failed, trying CPU: ${e.message}")
                // Fallback to CPU if GPU fails
                try {
                    val cpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(cpuConfig)
                    newEngine.initialize()
                    engine = newEngine
                    isLoaded = true
                    Log.i(TAG, "Model loaded: $modelPath (CPU fallback)")
                    true
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to load model: ${e2.message}", e2)
                    isLoaded = false
                    false
                }
            }
        }
    }

    override fun generate(
        prompt: String,
        config: InferenceConfig,
        onStats: ((Float, Float, Float) -> Unit)?
    ): Flow<String> {
        val currentEngine = engine ?: return flow {
            throw RuntimeException("Model not loaded")
        }

        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0

        val samplerConfig = SamplerConfig(
            topK = 40,
            topP = config.topP.toDouble(),
            temperature = config.temperature.toDouble(),
            seed = 0
        )
        val convConfig = ConversationConfig(
            samplerConfig = samplerConfig
        )
        val conv = currentEngine.createConversation(convConfig)
        conversation = conv

        return conv.sendMessageAsync(prompt)
            .map { message ->
                if (firstTokenTime == null) {
                    firstTokenTime = System.currentTimeMillis()
                    Log.i(TAG, "TTFT: ${firstTokenTime!! - startTime} ms")
                }
                tokenCount++
                message.contents.toString()
            }
            .buffer(capacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)
    }

    override fun stopGeneration() {
        try {
            conversation?.cancelProcess()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling: ${e.message}")
        }
    }

    override fun clearCache() {
        try {
            conversation?.close()
            conversation = null
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing: ${e.message}")
        }
    }

    override suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            try {
                conversation?.close()
                engine?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error unloading: ${e.message}")
            }
            conversation = null
            engine = null
            isLoaded = false
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded
}
