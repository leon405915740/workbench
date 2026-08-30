package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 习惯打卡历史。复合主键 habitId + date（date 为 yyyy-MM-dd），保证同一习惯同一天仅一条，
 * 支持历史补打与取消补打，不覆盖历史。
 */
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)
data class HabitLog(
    val habitId: String,
    val date: String,
    val checkedAt: Long,
    val durationMinutes: Int? = null,
    val note: String? = null,
    val category: ExerciseCategoryEnum? = null
)