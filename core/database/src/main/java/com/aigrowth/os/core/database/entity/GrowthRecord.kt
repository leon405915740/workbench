package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 成长记录实体
 * 每日的成长数据记录
 */
@Entity(tableName = "growth_records")
data class GrowthRecord(
    @PrimaryKey
    val id: String,
    val date: Long,                 // 记录日期
    val learningMinutes: Int,       // 学习时长（分钟）
    val tasksCompleted: Int,        // 完成任务数
    val knowledgeCardsCreated: Int, // 创建知识卡片数
    val masteryScore: Int,          // 掌握度评分
    val aiSummary: String?,         // AI成长总结
    val createdAt: Long
)