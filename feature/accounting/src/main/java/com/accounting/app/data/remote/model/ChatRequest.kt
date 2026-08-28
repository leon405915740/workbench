package com.accounting.app.data.remote.model

/**
 * DeepSeek Chat Completion 请求体。
 *
 * DeepSeek 使用 OpenAI 兼容的 Chat Completion API，
 * 字段命名与 OpenAI 一致，便于直接复用现有规范。
 *
 * 设计要点：
 * - temperature 默认 0.1，低温度保证输出稳定，避免记账解析结果抖动
 * - stream 默认关闭，记账场景一次返回完整 JSON 即可
 */
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    val stream: Boolean = false
)

/**
 * 单条对话消息。
 *
 * @param role    角色：system / user / assistant
 * @param content 文本内容
 */
data class ChatMessage(
    val role: String,
    val content: String
)
