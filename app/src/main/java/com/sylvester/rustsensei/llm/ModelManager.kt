package com.sylvester.rustsensei.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedMB: Long, val totalMB: Long) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelManager(private val context: Context) {

    companion object {
        private const val MODEL_DIR = "models"
        private const val MODEL_FILENAME = "qwen3-4b.Q4_K_M.gguf"
        private const val MODEL_URL = "https://huggingface.co/sylvester-francis/rust-mentor-4b-GGUF/resolve/main/qwen3-4b.Q4_K_M.gguf"
        private const val EXPECTED_SIZE_BYTES = 2_497_280_832L // ~2.5GB
    }

    private val modelsDir: File
        get() = File(context.filesDir, MODEL_DIR).also { it.mkdirs() }

    val modelFile: File
        get() = File(modelsDir, MODEL_FILENAME)

    fun isModelDownloaded(): Boolean {
        return modelFile.exists() && modelFile.length() > 1_000_000 // basic sanity check
    }

    fun getModelSizeMB(): Long {
        return if (modelFile.exists()) modelFile.length() / (1024 * 1024) else 0
    }

    fun deleteModel(): Boolean {
        return modelFile.delete()
    }

    fun downloadModel(): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f, 0, 0))

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val request = Request.Builder()
                .url(MODEL_URL)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download failed: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }

            val contentLength = body.contentLength().let {
                if (it > 0) it else EXPECTED_SIZE_BYTES
            }
            val totalMB = contentLength / (1024 * 1024)

            // Write to a temp file first, then rename
            val tempFile = File(modelsDir, "$MODEL_FILENAME.tmp")

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Long = 0
                    var lastEmitTime = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read

                        // Emit progress at most every 500ms to avoid flooding
                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > 500) {
                            val progress = bytesRead.toFloat() / contentLength
                            val downloadedMB = bytesRead / (1024 * 1024)
                            emit(DownloadState.Downloading(progress, downloadedMB, totalMB))
                            lastEmitTime = now
                        }
                    }
                }
            }

            // Rename temp to final
            tempFile.renameTo(modelFile)
            emit(DownloadState.Completed)

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown download error"))
        }
    }.flowOn(Dispatchers.IO)
}
