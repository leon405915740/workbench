package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 今日计划条目。
 * priority 使用 P0/P1/P2 或用户自定义字符串；planDate 使用 yyyy-MM-dd。
 */
@Entity(tableName = "plan_items")
data class PlanItem(
    @PrimaryKey val id: String,
    val title: String,
    val priority: String,
    val note: String,
    val done: Boolean,
    val pinned: Boolean,
    val planDate: String,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
)