package com.accounting.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 支出记录实体。
 *
 * 设计要点：
 * - 金额以「分」为单位存储为 Long，避免浮点误差
 * - 对 time 字段建索引，加速按时间范围查询
 */
@Entity(tableName = "expense", indices = [Index("time")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,           // 金额，单位：分（避免浮点误差）
    val category: String,       // 一级分类
    val subcategory: String?,   // 二级分类
    val merchant: String?,      // 商家
    val time: Long,             // 消费时间戳（毫秒）
    val note: String?,          // 备注
    val confidence: Float,      // 置信度 0~1
    val rawInput: String,       // 用户原始输入
    val createdAt: Long         // 创建时间
)
