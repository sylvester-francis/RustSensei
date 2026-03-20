package com.sylvester.rustsensei.llm

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedMB: Long,
        val totalMB: Long,
        val speedMBps: Float = 0f,
        val estimatedSecondsLeft: Long = 0
    ) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val parameterSize: String,
    val filename: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long,
    val description: String,
    val ramRequired: String,
    val minDeviceMemoryGb: Float = 0f,
    val sha256: String = ""
)

class ModelManager(private val context: Context) {

    companion object {
        private const val MODEL_DIR = "models"

        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "litert-1b-gemma",
                displayName = "Rust Mentor 1B",
                parameterSize = "1B",
                filename = "rust-mentor-1b-mobile_q8_ekv2053.litertlm",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-1b-mobile-LiteRT/resolve/main/rust-mentor-1b-mobile_q8_ekv2053.litertlm",
                expectedSizeBytes = 1_200_000_000L,
                description = "Best balance of speed and quality. Recommended.",
                ramRequired = "~3 GB RAM",
                minDeviceMemoryGb = 3f
            )
            // NOTE: 0.6B and 1.7B models removed — they have ops that can't fully
            // delegate to GPU (1203/1255 nodes), causing LiteRT's LLM engine to fail
            // with INTERNAL error at llm_litert_compiled_model_executor.cc:2143.
            // Re-add once the models are re-converted with full GPU-compatible ops.
        )

        fun getModelById(id: String): ModelInfo? = AVAILABLE_MODELS.find { it.id == id }
    }

    private val modelsDir: File
        get() = File(context.filesDir, MODEL_DIR).also { it.mkdirs() }

    fun getModelFile(modelInfo: ModelInfo): File = File(modelsDir, modelInfo.filename)

    private fun getTempFile(modelInfo: ModelInfo): File = File(modelsDir, "${modelInfo.filename}.tmp")

    // Legacy accessors for backward compatibility
    val modelFile: File
        get() = File(modelsDir, AVAILABLE_MODELS[0].filename)

    fun isModelDownloaded(modelInfo: ModelInfo): Boolean {
        val file = getModelFile(modelInfo)
        return file.exists() && file.length() > 1_000_000
    }

    /** Bug 12: Check if a partial .tmp file exists from an interrupted download. */
    fun hasTempFile(modelInfo: ModelInfo): Boolean {
        val tempFile = getTempFile(modelInfo)
        return tempFile.exists() && tempFile.length() > 0
    }

    fun isModelDownloaded(): Boolean = isModelDownloaded(AVAILABLE_MODELS[0])

    fun getModelSizeMB(modelInfo: ModelInfo): Long {
        val file = getModelFile(modelInfo)
        return if (file.exists()) file.length() / (1024 * 1024) else 0
    }

    fun getModelSizeMB(): Long = getModelSizeMB(AVAILABLE_MODELS[0])

    fun getDownloadedModels(): List<ModelInfo> {
        return AVAILABLE_MODELS.filter { isModelDownloaded(it) }
    }

    fun deleteModel(modelInfo: ModelInfo): Boolean {
        getTempFile(modelInfo).delete()
        return getModelFile(modelInfo).delete()
    }

    fun deleteModel(): Boolean = deleteModel(AVAILABLE_MODELS[0])

    /** Clean up any orphaned .tmp files from failed downloads. */
    fun cleanupOrphanedTempFiles() {
        modelsDir.listFiles()?.filter { it.name.endsWith(".tmp") }?.forEach { tmpFile ->
            Log.i("ModelManager", "Cleaning up orphaned temp file: ${tmpFile.name} (${tmpFile.length() / (1024 * 1024)} MB)")
            tmpFile.delete()
        }
    }

    fun downloadModel(modelInfo: ModelInfo): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f, 0, 0))

        // Optimization #8: Acquire WiFi and Wake locks during large model download.
        // Without these, the WiFi radio may throttle or disconnect when the screen
        // turns off mid-download (~1.2 GB), wasting battery on retries.
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "RustSensei:ModelDownload")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RustSensei:ModelDownload")

        try {
            wifiLock?.acquire()
            wakeLock?.acquire(30 * 60 * 1000L) // 30 min timeout safety net

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val tempFile = getTempFile(modelInfo)
            val finalFile = getModelFile(modelInfo)

            var existingBytes = 0L
            if (tempFile.exists()) {
                existingBytes = tempFile.length()
            }

            val requestBuilder = Request.Builder().url(modelInfo.downloadUrl)
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                emit(DownloadState.Error("Download failed: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }

            val isResuming = response.code == 206 && existingBytes > 0
            if (!isResuming) {
                existingBytes = 0
                tempFile.delete()
            }

            val contentLength = body.contentLength().let {
                if (it > 0) it + existingBytes else modelInfo.expectedSizeBytes
            }
            val totalMB = contentLength / (1024 * 1024)

            val downloadStartTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(tempFile, isResuming).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead = existingBytes
                    val bytesAtStart = existingBytes
                    var lastEmitTime = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > 500) {
                            val progress = bytesRead.toFloat() / contentLength
                            val downloadedMB = bytesRead / (1024 * 1024)

                            // Calculate speed and ETA
                            val elapsedMs = now - downloadStartTime
                            val bytesDownloadedThisSession = bytesRead - bytesAtStart
                            val speedBps = if (elapsedMs > 0) (bytesDownloadedThisSession * 1000.0 / elapsedMs) else 0.0
                            val speedMBps = (speedBps / (1024 * 1024)).toFloat()
                            val remainingBytes = contentLength - bytesRead
                            val estimatedSecondsLeft = if (speedBps > 0) (remainingBytes / speedBps).toLong() else 0

                            emit(DownloadState.Downloading(progress, downloadedMB, totalMB, speedMBps, estimatedSecondsLeft))
                            lastEmitTime = now
                        }
                    }
                }
            }

            if (tempFile.length() < 1_000_000) {
                tempFile.delete()
                emit(DownloadState.Error("Download appears incomplete (${tempFile.length()} bytes)"))
                return@flow
            }

            // Verify SHA256 integrity if checksum is provided
            if (modelInfo.sha256.isNotEmpty()) {
                val actualHash = sha256(tempFile)
                if (!actualHash.equals(modelInfo.sha256, ignoreCase = true)) {
                    tempFile.delete()
                    emit(DownloadState.Error(
                        "Integrity check failed. Expected SHA256: ${modelInfo.sha256.take(12)}... " +
                        "Got: ${actualHash.take(12)}... The file may be corrupted."
                    ))
                    return@flow
                }
                Log.i("ModelManager", "SHA256 verified: ${actualHash.take(16)}...")
            }

            tempFile.renameTo(finalFile)
            emit(DownloadState.Completed)

        } catch (e: Exception) {
            // Clean up partial download on non-resumable errors
            val tempFile = getTempFile(modelInfo)
            if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                // Network errors: keep temp file for resume
                Log.w("ModelManager", "Network error, keeping temp file for resume: ${e.message}")
            } else {
                // Other errors: clean up to avoid orphaned files
                tempFile.delete()
            }
            emit(DownloadState.Error(e.message ?: "Unknown download error"))
        } finally {
            // Optimization #8: Always release locks when download finishes or fails
            try { if (wakeLock?.isHeld == true) wakeLock.release() } catch (_: Exception) {}
            try { if (wifiLock?.isHeld == true) wifiLock.release() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    // Legacy: download default model
    fun downloadModel(): Flow<DownloadState> = downloadModel(AVAILABLE_MODELS[0])

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
