package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

                try {
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    engine = newEngine
                    Log.i(TAG, "Model loaded (GPU): $modelPath")
                } catch (gpuError: Exception) {
                    Log.w(TAG, "GPU failed: ${gpuError.message}, trying CPU...")
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    engine = newEngine
                    Log.i(TAG, "Model loaded (CPU): $modelPath")
                }

                isLoaded = true
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model: ${e.message}", e)
                isLoaded = false
                false
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

        // Create a fresh conversation for each generation
        val conv = try {
            currentEngine.createConversation(ConversationConfig())
        } catch (e: Exception) {
            Log.e(TAG, "createConversation failed: ${e.message}", e)
            return flow { throw RuntimeException("Failed to create conversation: ${e.message}") }
        }
        conversation = conv

        // sendMessageAsync(String) returns Flow<Message> — stream tokens
        return conv.sendMessageAsync(prompt)
            .map { message: Message ->
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
