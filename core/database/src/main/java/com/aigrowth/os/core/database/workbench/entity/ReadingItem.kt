package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读条目（进度型）。unit 支持页/章或用户自定义单位。
 */
@Entity(tableName = "reading_items")
data class ReadingItem(
    @PrimaryKey val id: String,
    val title: String,
    val current: Float,
    val target: Float,
    val unit: String,
    val date: String,
    val note: String,
    val pinned: Boolean,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)