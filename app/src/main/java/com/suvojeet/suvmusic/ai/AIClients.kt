package com.suvojeet.suvmusic.ai

import com.google.gson.Gson
import com.suvojeet.suvmusic.core.model.ChatProxyModels
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

    override suspend fun getAudioEffectState(
        prompt: String, 
        currentStatus: AudioEffectState,
        songContext: SongContext?
    ): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPrompt(currentStatus, songContext)
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

    override suspend fun getAudioEffectState(
        prompt: String, 
        currentStatus: AudioEffectState,
        songContext: SongContext?
    ): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPrompt(currentStatus, songContext)
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

    override suspend fun getAudioEffectState(
        prompt: String,
        currentStatus: AudioEffectState,
        songContext: SongContext?
    ): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPrompt(currentStatus, songContext)
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

class ChatProxyClient(
    private val model: String,
    private val fallbackModels: List<String>? = null
) : AIClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://chatbot.codexapi.workers.dev/"

    override suspend fun getAudioEffectState(
        prompt: String,
        currentStatus: AudioEffectState,
        songContext: SongContext?
    ): Result<AudioEffectState> = withContext(Dispatchers.IO) {
        val resolvedModel = ChatProxyModels.resolve(model)
        val candidates = listOf(resolvedModel) + (fallbackModels?.map { ChatProxyModels.resolve(it) } ?: emptyList())

        var lastError: Throwable? = null

        for (candidate in candidates) {
            try {
                val result = tryModel(prompt, currentStatus, songContext, candidate)
                if (result.isSuccess) return@withContext result
                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
            }
        }

        return@withContext Result.failure(
            lastError ?: Exception("All ${candidates.size} models failed")
        )
    }

    private fun tryModel(
        prompt: String,
        currentStatus: AudioEffectState,
        songContext: SongContext?,
        modelToUse: String
    ): Result<AudioEffectState> {
        try {
            val systemPrompt = getSystemPrompt(currentStatus, songContext)
            val fullPrompt = "$systemPrompt\n\nUser request: $prompt"
            val encodedPrompt = java.net.URLEncoder.encode(fullPrompt, "UTF-8")
            val encodedModel = java.net.URLEncoder.encode(modelToUse, "UTF-8")
            val url = "$baseUrl?prompt=$encodedPrompt&model=$encodedModel"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Model '$modelToUse' failed: ${response.code} ${response.message}"))
                }
                val body = response.body?.string() ?: return Result.failure(Exception("Model '$modelToUse': Empty response"))
                val jsonResponse = JSONObject(body)
                if (jsonResponse.has("error")) {
                    return Result.failure(Exception("Model '$modelToUse': ${jsonResponse.getString("error")}"))
                }
                val answer = jsonResponse.getString("answer")
                // Extract JSON from answer (might have markdown or extra text)
                val jsonStart = answer.indexOf("{")
                val jsonEnd = answer.lastIndexOf("}")
                if (jsonStart != -1 && jsonEnd != -1) {
                    val jsonContent = answer.substring(jsonStart, jsonEnd + 1)
                    return Result.success(gson.fromJson(jsonContent, AudioEffectState::class.java))
                } else {
                    return Result.failure(Exception("Model '$modelToUse': Invalid response format"))
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}

private fun getSystemPrompt(currentStatus: AudioEffectState, songContext: SongContext?): String {
    val songInfo = songContext?.let {
        "\nCurrently playing: ${it.title} by ${it.artist} (Source: ${it.source})"
    } ?: "\nNo song metadata available."

    return """
        You are an elite senior audio engineer for SuvMusic.
        User's device is playing: $songInfo
        
        Live Audio Analysis from Hardware:
        - Peak Level: ${"%.2f".format(currentStatus.limiterThresholdDb ?: 0f)} (0.0 to 1.0)
        - RMS Level: (Perceived loudness context)
        
        Your goal is to transform the audio based on the user's request AND the live signal data.
        Perform "Hardware-level Tuning":
        - If peak is high, lower limiterThresholdDb to prevent digital clipping.
        - Adjust limiterRatio and limiterMakeupGain for "mastering" quality.
        - DO NOT return all 0.0 values unless specifically asked for a flat/reset profile.
        - Actually change the bands to achieve the requested vibe (vibrant, echo, bassy, etc).
        
        The app has these low-level parameters:
        - eqEnabled: boolean
        - eqBands: list of 10 floats (-12 to 12 dB)
        - bassBoost: float (0.0 to 1.0)
        - virtualizer: float (0.0 to 1.0)
        - spatialEnabled: boolean
        - crossfeedEnabled: boolean
        - limiterThresholdDb: float (-24.0 to 0.0 dB)
        - limiterRatio: float (1.0 to 20.0)
        - limiterAttackMs: float (0.1 to 100.0)
        - limiterReleaseMs: float (10.0 to 1000.0)
        - limiterMakeupGain: float (0.0 to 12.0 dB)

        Current parameters: ${currentStatus.toJson()}

        Return ONLY a JSON object with these keys. No other text.
    """.trimIndent()
}
