package com.aigrowth.os.core.database.workbench.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 剪报。status 为收藏/稍后读/已读等状态，source 为来源等补充信息；date 为 yyyy-MM-dd。
 */
@Entity(tableName = "clippings")
data class Clipping(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val status: String?,
    val source: String?,
    val tags: String,
    val layout: String,
    val date: String,
    val pinned: Boolean,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)