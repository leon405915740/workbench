package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 番茄钟单例状态与累计统计（固定主键 "default"，仅一条记录）。
 * remainSeconds 为当前剩余秒数；startedAt 用于退后台后恢复计时；focusCount/totalFocusMinutes 为累计完成次数与专注分钟。
 */
@Entity(tableName = "pomodoro_state")
data class PomodoroState(
    @PrimaryKey val id: String,
    val running: Boolean,
    val remainSeconds: Int,
    val totalSeconds: Int,
    val startedAt: Long?,
    val focusCount: Int,
    val totalFocusMinutes: Int
)