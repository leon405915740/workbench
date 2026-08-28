package com.accounting.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类记忆实体。
 *
 * 用于记忆「触发词 -> 分类」的映射，提升后续自动分类的准确率。
 * 触发词与类型组合建立唯一索引，保证同一触发词在同一收支类型下唯一。
 */
@Entity(
    tableName = "category_memory",
    indices = [Index(value = ["triggerWord", "type"], unique = true)]
)
data class CategoryMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerWord: String,    // 触发词（商家名/核心关键词）
    val type: String,           // 收支类型：expense / income
    val category: String,       // 一级分类
    val subcategory: String?,   // 二级分类（通用分类，时段由运行时判定）
    val hitCount: Int = 1,      // 命中次数
    val source: String = "user", // 来源：seed（系统预置）/ auto（自动学习）/ user（手动添加）
    val confidence: Int = 100,  // 置信度 0-100，种子词默认 80，用户记忆默认 100
    val createdAt: Long,        // 创建时间
    val updatedAt: Long         // 最后更新时间
)
