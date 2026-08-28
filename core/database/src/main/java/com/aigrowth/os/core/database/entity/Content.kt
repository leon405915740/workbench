package com.aigrowth.os.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 内容实体
 * 自媒体创作内容
 */
@Entity(tableName = "contents")
data class Content(
    @PrimaryKey
    val id: String,
    val title: String,
    val contentType: ContentType,
    val content: String,            // 内容正文
    val structure: String,          // 内容结构（JSON格式）
    val tags: String,               // 标签（JSON格式）
    val status: ContentStatus,
    val publishedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ContentType {
    IDEA,        // 创意
    SCRIPT,      // 脚本
    GROWTH_REPORT, // 成长报告
    ANALYSIS     // 分析
}

enum class ContentStatus {
    DRAFT,       // 草稿
    PUBLISHED    // 已发布
}