package com.sylvester.rustsensei.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpPlaygroundService @Inject constructor() : RustPlaygroundService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(request: PlaygroundRequest): PlaygroundResponse =
        withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("channel", request.channel)
                put("mode", request.mode)
                put("edition", request.edition)
                put("crateType", request.crateType)
                put("tests", request.tests)
                put("code", request.code)
                put("backtrace", request.backtrace)
            }

            val httpRequest = Request.Builder()
                .url(PLAYGROUND_URL)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                throw PlaygroundApiException("Playground API error: HTTP ${response.code}")
            }

            val body = response.body?.string()
                ?: throw PlaygroundApiException("Empty response from Playground API")

            val json = JSONObject(body)
            PlaygroundResponse(
                success = json.getBoolean("success"),
                stdout = json.optString("stdout", ""),
                stderr = json.optString("stderr", "")
            )
        }

    companion object {
        private const val PLAYGROUND_URL = "https://play.rust-lang.org/execute"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
