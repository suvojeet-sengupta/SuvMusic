package com.suvojeet.suvmusic.ai

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OpenAIClient(private val apiKey: String, private val model: String) : AIClient {
    private val client = OkHttpClient()
    private val gson = Gson()

    override suspend fun getAudioEffectState(prompt: String, currentStatus: AudioEffectState): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPrompt(currentStatus)
            val json = JSONObject().apply {
                put("model", model)
                put("messages", com.google.gson.JsonArray().apply {
                    add(JSONObject().apply { put("role", "system"); put("content", systemPrompt) }.toString())
                    add(JSONObject().apply { put("role", "user"); put("content", prompt) }.toString())
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
            }

            // OpenAI API is slightly different in message structure, I'll use a simpler way
            val requestBody = """
                {
                    "model": "$model",
                    "messages": [
                        {"role": "system", "content": ${JSONObject.quote(systemPrompt)}},
                        {"role": "user", "content": ${JSONObject.quote(prompt)}}
                    ],
                    "response_format": {"type": "json_object"}
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("OpenAI error: ${response.code} ${response.message}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val jsonResponse = JSONObject(body)
                val content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                Result.success(gson.fromJson(content, AudioEffectState::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GeminiClient(private val apiKey: String, private val model: String) : AIClient {
    private val client = OkHttpClient()
    private val gson = Gson()

    override suspend fun getAudioEffectState(prompt: String, currentStatus: AudioEffectState): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPrompt(currentStatus)
            val requestBody = """
                {
                  "contents": [{
                    "parts": [{"text": ${JSONObject.quote("$systemPrompt\n\nUser request: $prompt")}}]
                  }],
                  "generationConfig": {
                    "response_mime_type": "application/json"
                  }
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Gemini error: ${response.code} ${response.message}\n${response.body?.string()}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val jsonResponse = JSONObject(body)
                val content = jsonResponse.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                Result.success(gson.fromJson(content, AudioEffectState::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AnthropicClient(private val apiKey: String, private val model: String) : AIClient {
    private val client = OkHttpClient()
    private val gson = Gson()

    override suspend fun getAudioEffectState(prompt: String, currentStatus: AudioEffectState): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPrompt(currentStatus)
            val requestBody = """
                {
                    "model": "$model",
                    "max_tokens": 1024,
                    "system": ${JSONObject.quote(systemPrompt)},
                    "messages": [
                        {"role": "user", "content": ${JSONObject.quote(prompt)}}
                    ]
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Anthropic error: ${response.code} ${response.message}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val jsonResponse = JSONObject(body)
                val content = jsonResponse.getJSONArray("content").getJSONObject(0).getString("text")
                Result.success(gson.fromJson(content, AudioEffectState::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun getSystemPrompt(currentStatus: AudioEffectState): String {
    return """
        You are an expert audio engineer for the SuvMusic app. 
        Your task is to optimize the audio settings based on the user's natural language request.
        The app has the following parameters:
        - eqEnabled: boolean
        - eqBands: list of 10 floats (gains in dB for frequencies: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz). Range: -12.0 to 12.0.
        - bassBoost: float (0.0 to 1.0)
        - virtualizer: float (0.0 to 1.0)
        - spatialEnabled: boolean
        - crossfeedEnabled: boolean
        - limiterMakeupGain: float (dB, range 0.0 to 20.0)

        Current status: ${Gson().toJson(currentStatus)}

        Return ONLY a valid JSON object with the keys matching the parameter names above. Do not include any other text or explanation. 
        Ensure all 10 bands are present in eqBands.
    """.trimIndent()
}
