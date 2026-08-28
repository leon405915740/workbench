package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI记忆实体
 * AI从用户学习中提取的记忆，用于个性化服务
 */
@Entity(tableName = "ai_memories")
data class AIMemory(
    @PrimaryKey
    val id: String,
    val memoryType: MemoryType,
    val content: String,
    val importance: Int,            // 重要性 1-5
    val sourceType: String,         // 来源类型：conversation / task / evaluation
    val sourceId: String?,          // 来源ID
    val createdAt: Long,
    val lastAccessedAt: Long
)

enum class MemoryType {
    WEAKNESS,     // 薄弱点
    PREFERENCE,   // 偏好
    HABIT,        // 习惯
    ACHIEVEMENT   // 成就
}