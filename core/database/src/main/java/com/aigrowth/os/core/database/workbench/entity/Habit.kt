package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯项目定义。打卡历史在 HabitLog 中按日期保存，不使用单一布尔覆盖。
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String,
    val title: String,
    val active: Boolean,
    val pinned: Boolean,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)