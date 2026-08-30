package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读日志（进度型）。amount 记录单次阅读量，date 使用 yyyy-MM-dd。
 */
@Entity(tableName = "reading_logs")
data class ReadingLog(
    @PrimaryKey val id: String,
    val readingItemId: String,
    val date: String,
    val amount: Float,
    val createdAt: Long,
    val note: String? = null
)
