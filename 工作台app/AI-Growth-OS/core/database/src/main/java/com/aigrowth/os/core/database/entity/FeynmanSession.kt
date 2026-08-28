package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 费曼学习会话实体
 * 记录费曼学习模式的会话状态和结果
 */
@Entity(tableName = "feynman_sessions")
data class FeynmanSession(
    @PrimaryKey
    val id: String,
    val topic: String,              // 学习主题
    val knowledgeCardId: String?,   // 关联知识卡片ID
    val status: FeynmanStatus,      // 会话状态
    val currentScore: Int,          // 当前评分 0-100
    val targetScore: Int,           // 目标评分 默认90
    val finalFeedback: String?,     // 最终反馈总结
    val createdAt: Long,
    val completedAt: Long?
)

enum class FeynmanStatus {
    IN_PROGRESS,    // 进行中
    COMPLETED,      // 已完成（达到目标分数）
    ABANDONED       // 已放弃
}
