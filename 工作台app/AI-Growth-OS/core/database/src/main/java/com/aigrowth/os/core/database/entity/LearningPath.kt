package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 学习路径实体
 * AI生成的学习路线，包含多个学习等级
 */
@Entity(
    tableName = "learning_paths",
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class LearningPath(
    @PrimaryKey
    val id: String,
    val goalId: String,
    val title: String,
    val description: String,
    val totalLevels: Int,
    val currentLevel: Int,
    val createdAt: Long,
    val updatedAt: Long
)