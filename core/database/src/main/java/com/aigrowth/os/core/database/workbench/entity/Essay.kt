package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 随笔。type 区分普通记录(note)与引文表达(quote)；date 为 yyyy-MM-dd。
 */
@Entity(tableName = "essays")
data class Essay(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val mood: String?,
    val type: String,
    val tags: String,
    val layout: String,
    val date: String,
    val pinned: Boolean,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)