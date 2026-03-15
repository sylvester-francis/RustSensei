package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class LiteRtEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"
    }

    private var llmInference: LlmInference? = null
    private var isLoaded = false

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                llmInference?.close()
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(contextSize)
                    .setMaxTopK(64)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                isLoaded = true
                Log.i(TAG, "Model loaded: $modelPath (maxTokens=$contextSize)")
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
    ): Flow<String> = callbackFlow {
        val inference = llmInference ?: run {
            close(RuntimeException("Model not loaded"))
            return@callbackFlow
        }

        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0

        try {
            // Create a session with temperature/topK/topP
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(config.temperature)
                .setTopK(40)
                .setTopP(config.topP)
                .build()

            val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)

            // Add the prompt
            session.addQueryChunk(prompt)

            // Generate with streaming via ProgressListener
            session.generateResponseAsync { partialResult, done ->
                if (firstTokenTime == null && partialResult.isNotEmpty()) {
                    firstTokenTime = System.currentTimeMillis()
                    Log.i(TAG, "TTFT: ${firstTokenTime!! - startTime} ms")
                }

                if (partialResult.isNotEmpty()) {
                    tokenCount++
                    trySend(partialResult)
                }

                if (done) {
                    val ftTime = firstTokenTime ?: startTime
                    val genMs = System.currentTimeMillis() - ftTime
                    val tokPerSec = if (genMs > 0) tokenCount * 1000f / genMs else 0f
                    val prefillMs = (ftTime - startTime).toFloat()
                    Log.i(TAG, "Generation: $tokenCount tokens in ${genMs}ms (${"%.1f".format(tokPerSec)} tok/s)")
                    onStats?.invoke(0f, tokPerSec, prefillMs)
                    session.close()
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}", e)
            close(RuntimeException("Generation failed: ${e.message}"))
        }

        awaitClose { }
    }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)

    override fun stopGeneration() {
        // MediaPipe sessions close on completion; no mid-stream cancel
    }

    override fun clearCache() {
        // MediaPipe manages KV cache internally per session
    }

    override suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            try {
                llmInference?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing model: ${e.message}")
            }
            llmInference = null
            isLoaded = false
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded
}
