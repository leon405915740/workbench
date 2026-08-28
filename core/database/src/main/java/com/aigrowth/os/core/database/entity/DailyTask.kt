package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 每日任务实体
 * AI生成的每日学习任务
 */
@Entity(
    tableName = "daily_tasks",
    foreignKeys = [
        ForeignKey(
            entity = LearningLevel::class,
            parentColumns = ["id"],
            childColumns = ["learningLevelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("learningLevelId")]
)
data class DailyTask(
    @PrimaryKey
    val id: String,
    val learningLevelId: String?,
    val taskType: TaskType,
    val title: String,
    val description: String,
    val estimatedTime: Int,          // 预计时间（分钟）
    val scheduledDate: Long,         // 计划日期
    val status: TaskStatus,
    val userResponse: String?,       // 用户回答
    val aiFeedback: String?,         // AI反馈
    val score: Int?,                 // 评分 0-100
    val completedAt: Long?,
    val createdAt: Long
)

enum class TaskType {
    LEARNING,    // 学习
    PRACTICE,    // 练习
    TEST,        // 测试
    FEYNMAN,     // 费曼学习
    REVIEW       // 复盘
}

enum class TaskStatus {
    PENDING,     // 待完成
    COMPLETED,   // 已完成
    SKIPPED      // 跳过
}