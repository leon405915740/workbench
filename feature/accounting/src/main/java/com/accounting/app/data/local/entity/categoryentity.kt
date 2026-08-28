package com.accounting.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type", "parentId"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val parentId: Long?,
    val sortOrder: Int,
    val isSystem: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)