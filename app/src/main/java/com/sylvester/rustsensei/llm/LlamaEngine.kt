package com.sylvester.rustsensei.llm

import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

@Keep // P1 Fix #6: prevent R8 from renaming this class (JNI references it by name)
class LlamaEngine {

    companion object {
        init {
            System.loadLibrary("rustsensei-llm")
        }
    }

    // Callbacks from native code
    private var tokenCallback: ((String) -> Unit)? = null
    private var completeCallback: (() -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var statsCallback: ((Float, Float, Float) -> Unit)? = null

    // P1 Fix #6: @Keep prevents R8 from renaming JNI callback methods
    @Keep
    @Suppress("unused")
    fun onNativeToken(token: String) {
        tokenCallback?.invoke(token)
    }

    @Keep
    @Suppress("unused")
    fun onNativeComplete() {
        completeCallback?.invoke()
    }

    @Keep
    @Suppress("unused")
    fun onNativeError(error: String) {
        errorCallback?.invoke(error)
    }

    @Keep
    @Suppress("unused")
    fun onNativeStats(prefillTokPerSec: Float, decodeTokPerSec: Float, prefillMs: Float) {
        statsCallback?.invoke(prefillTokPerSec, decodeTokPerSec, prefillMs)
    }

    suspend fun loadModel(modelPath: String, contextSize: Int = 2048): Boolean {
        return withContext(Dispatchers.IO) {
            loadModelNative(modelPath, contextSize)
        }
    }

    fun generate(
        prompt: String,
        config: InferenceConfig = InferenceConfig(),
        onStats: ((prefillTokPerSec: Float, decodeTokPerSec: Float, prefillMs: Float) -> Unit)? = null
    ): Flow<String> = callbackFlow {
        tokenCallback = { token ->
            // P2 Fix #12: trySend can drop tokens if buffer is full.
            // Using a buffered channel ensures backpressure instead of drops.
            val result = trySend(token)
            if (result.isFailure) {
                // Channel closed or full — stop generation to avoid native thread spinning
                stopGenerationNative()
            }
        }
        completeCallback = {
            close()
        }
        errorCallback = { error ->
            close(RuntimeException(error))
        }
        statsCallback = onStats

        val thread = Thread {
            generateNative(prompt, config.maxTokens, config.temperature, config.topP)
        }
        thread.start()

        awaitClose {
            stopGenerationNative()
            tokenCallback = null
            completeCallback = null
            errorCallback = null
            statsCallback = null
        }
    }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.SUSPEND) // P2 Fix #12: buffer tokens

    fun stopGeneration() {
        stopGenerationNative()
    }

    fun clearCache() {
        clearCacheNative()
    }

    suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            unloadModelNative()
        }
    }

    fun isModelLoaded(): Boolean = isModelLoadedNative()

    // Native methods
    private external fun loadModelNative(modelPath: String, contextSize: Int): Boolean
    private external fun generateNative(prompt: String, maxTokens: Int, temperature: Float, topP: Float)
    private external fun stopGenerationNative()
    private external fun clearCacheNative()
    private external fun unloadModelNative()
    private external fun isModelLoadedNative(): Boolean
}
