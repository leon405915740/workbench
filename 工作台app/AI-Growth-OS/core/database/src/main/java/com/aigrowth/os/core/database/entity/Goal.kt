package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 目标实体
 * 用户的学习目标，如"我要学AI开发"
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val status: GoalStatus,
    val learningPathId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class GoalStatus {
    ACTIVE,      // 进行中
    COMPLETED,   // 已完成
    PAUSED       // 暂停
}