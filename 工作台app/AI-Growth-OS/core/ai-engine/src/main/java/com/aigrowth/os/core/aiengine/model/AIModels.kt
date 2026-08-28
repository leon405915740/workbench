package com.aigrowth.os.core.aiengine

/**
 * AI请求
 */
data class AIRequest(
    val model: String = "deepseek-v4-flash",
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val systemPrompt: String,
    val userMessage: String
)

/**
 * AI响应
 */
data class AIResponse(
    val id: String,
    val content: String,
    val model: String,
    val usage: Usage
)

/**
 * Token使用量
 */
data class Usage(
    val inputTokens: Int,
    val outputTokens: Int
)

/**
 * AI模型类型
 */
enum class AIModel {
    DEEPSEEK_CHAT,
    DEEPSEEK_REASONER
}