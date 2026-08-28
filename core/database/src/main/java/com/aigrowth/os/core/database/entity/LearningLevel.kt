package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 学习等级实体
 * 学习路径中的具体等级，如Level1 Python、Level2 API等
 */
@Entity(
    tableName = "learning_levels",
    foreignKeys = [
        ForeignKey(
            entity = LearningPath::class,
            parentColumns = ["id"],
            childColumns = ["learningPathId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("learningPathId")]
)
data class LearningLevel(
    @PrimaryKey
    val id: String,
    val learningPathId: String,
    val levelNumber: Int,           // 等级编号 1-5
    val title: String,              // 等级标题，如"Python基础"
    val objective: String,          // 学习目标
    val knowledgePoints: String,    // 知识点（JSON格式）
    val commonMistakes: String,     // 常见错误（JSON格式）
    val successCriteria: String,    // 达标标准
    val status: LevelStatus,
    val startedAt: Long?,
    val completedAt: Long?,
    val createdAt: Long
)

enum class LevelStatus {
    LOCKED,      // 未解锁
    UNLOCKED,    // 已解锁
    IN_PROGRESS, // 进行中
    COMPLETED    // 已完成
}