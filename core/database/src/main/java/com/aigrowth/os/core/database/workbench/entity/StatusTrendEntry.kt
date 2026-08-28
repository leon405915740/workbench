package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 七天状态趋势中的单日状态记录。date 为 yyyy-MM-dd，score 为当日状态分（0-100）。
 */
@Entity(tableName = "status_trend_entries")
data class StatusTrendEntry(
    @PrimaryKey val id: String,
    val date: String,
    val score: Int,
    val note: String?,
    val createdAt: Long
)