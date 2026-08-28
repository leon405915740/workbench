package com.aigrowth.os.core.aiengine

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI API客户端
 * 统一管理AI API调用（DeepSeek / OpenCode Zen），处理请求/响应、错误处理、超时设置。
 * 两者均为OpenAI兼容格式，仅 base url 与默认模型不同。
 */
@Singleton
class AIClient @Inject constructor() {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions"
        private const val ZEN_URL = "https://opencode.ai/zen/go/v1/chat/completions"
        private const val DEEPSEEK_MODEL = "deepseek-chat"
        private const val ZEN_MODEL = "deepseek-v4-flash"
    }

    /**
     * 根据provider路由到对应API
     * "zen" -> OpenCode Zen；其他 -> DeepSeek
     */
    suspend fun call(
        apiKey: String,
        provider: String,
        request: AIRequest
    ): Result<AIResponse> {
        return when (provider) {
            "zen" -> request(apiKey, ZEN_URL, ZEN_MODEL, request)
            else -> request(apiKey, DEEPSEEK_URL, DEEPSEEK_MODEL, request)
        }
    }

    private suspend fun request(
        apiKey: String,
        url: String,
        modelName: String,
        request: AIRequest
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildRequest(request, modelName)

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                val aiResponse = parseResponse(responseBody)
                Result.success(aiResponse)
            } else {
                Result.failure(Exception("API call failed: ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("API call error: ${e.message}", e))
        }
    }

    private fun buildRequest(request: AIRequest, modelName: String): RequestBody {
        val body = mapOf(
            "model" to modelName,
            "max_tokens" to request.maxTokens,
            "temperature" to request.temperature,
            "messages" to listOf(
                mapOf("role" to "system", "content" to request.systemPrompt),
                mapOf("role" to "user", "content" to request.userMessage)
            )
        )
        return gson.toJson(body).toRequestBody(
            "application/json".toMediaType()
        )
    }

    private fun parseResponse(json: String): AIResponse {
        val response = gson.fromJson(json, OpenAIResponseJson::class.java)
        return AIResponse(
            id = response.id,
            content = response.choices.firstOrNull()?.message?.content ?: "",
            model = response.model,
            usage = Usage(
                inputTokens = response.usage.prompt_tokens,
                outputTokens = response.usage.completion_tokens
            )
        )
    }

    // JSON响应数据类
    private data class OpenAIResponseJson(
        val id: String,
        val model: String,
        val choices: List<Choice>,
        val usage: OpenAIUsageJson
    )

    private data class Choice(
        val message: Message,
        val finish_reason: String
    )

    private data class Message(
        val role: String,
        val content: String
    )

    private data class OpenAIUsageJson(
        val prompt_tokens: Int,
        val completion_tokens: Int
    )
}
