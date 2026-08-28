package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * AI对话记录实体
 * 用户与AI的对话记录，用于AI Memory和学习
 */
@Entity(
    tableName = "ai_conversations",
    foreignKeys = [
        ForeignKey(
            entity = DailyTask::class,
            parentColumns = ["id"],
            childColumns = ["relatedTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("relatedTaskId")]
)
data class AIConversation(
    @PrimaryKey
    val id: String,
    val sessionId: String,          // 会话ID，关联一组对话
    val agentType: AgentType,        // Agent类型
    val role: ConversationRole,      // 角色：用户或AI
    val content: String,             // 对话内容
    val relatedTaskId: String?,      // 关联的任务ID
    val createdAt: Long
)

enum class AgentType {
    LEARNING_AGENT,   // 学习导师
    ENGLISH_AGENT,    // 英语教练
    FITNESS_AGENT,    // 健身教练
    CREATOR_AGENT     // 创作助手
}

enum class ConversationRole {
    USER,  // 用户
    AI     // AI
}