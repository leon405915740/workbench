package com.accounting.app.ui.model

/**
 * 聊天消息密封类。
 * 不同类型的消息在 UI 中渲染为不同样式。
 */
sealed class ChatMessage {
    abstract val timestamp: Long

    /** 用户发送的文本消息 */
    data class UserMessage(
        val text: String,
        override val timestamp: Long
    ) : ChatMessage()

    /** AI 回复的文本消息 */
    data class AiMessage(
        val text: String,
        override val timestamp: Long
    ) : ChatMessage()

    /** 记账卡片消息（包含支出或收入记录） */
    data class CardMessage(
        val recordId: Long,
        val type: String,           // expense / income
        val amount: Long,           // 金额（分）
        val category: String,
        val subcategory: String?,
        val merchant: String?,
        val recordTime: Long,       // 消费/收入时间
        val note: String?,          // 备注
        val confidence: Float,
        val matchedMemory: Boolean, // 是否命中记忆
        val rawInput: String,       // 原始输入（用于手动记账预填）
        val source: String = "",
        override val timestamp: Long  // 消息发送时间
    ) : ChatMessage()

    /** AI 纯文本对话消息（分析/查询回复） */
    data class AiTextMessage(
        val content: String,
        override val timestamp: Long
    ) : ChatMessage()

    /** 错误消息（解析失败，带手动记账入口） */
    data class ErrorMessage(
        val text: String,
        val rawInput: String,       // 保留原始输入，手动记账时预填
        override val timestamp: Long
    ) : ChatMessage()
}
