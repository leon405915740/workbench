package com.accounting.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_mappings",
    indices = [Index(value = ["keyword", "type"], unique = true)]
)
data class CategoryMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val type: String,
    val categoryId: Long,
    val subcategoryId: Long?,
    val source: String,
    val enabled: Boolean = true,
    val hitCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val lastHitAt: Long?
)
