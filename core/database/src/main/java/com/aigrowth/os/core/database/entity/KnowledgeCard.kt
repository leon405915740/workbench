package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 知识卡片实体
 * AI生成的知识压缩卡片
 */
@Entity(
    tableName = "knowledge_cards",
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
data class KnowledgeCard(
    @PrimaryKey
    val id: String,
    val learningLevelId: String,
    val topic: String,              // 主题
    val coreDefinition: String,     // 核心定义
    val keyConcepts: String,        // 关键概念（JSON格式）
    val useCases: String,           // 应用案例（JSON格式）
    val commonMistakes: String,     // 常见错误（JSON格式）
    val checklist: String,          // 检查清单（JSON格式）
    val selfTestQuestions: String,  // 自测问题（JSON格式）
    val masteryScore: Int,          // 掌握度 0-100
    val createdAt: Long,
    val updatedAt: Long
)