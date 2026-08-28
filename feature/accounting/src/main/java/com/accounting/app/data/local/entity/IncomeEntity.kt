package com.accounting.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 收入记录实体。
 *
 * 字段与 ExpenseEntity 完全一致，独立成表便于按收支类型分别统计。
 * 金额以「分」为单位存储为 Long，避免浮点误差。
 */
@Entity(tableName = "income", indices = [Index("time")])
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,           // 金额，单位：分（避免浮点误差）
    val category: String,       // 一级分类
    val subcategory: String?,   // 二级分类
    val merchant: String?,      // 商家
    val time: Long,             // 收入时间戳（毫秒）
    val note: String?,          // 备注
    val confidence: Float,      // 置信度 0~1
    val rawInput: String,       // 用户原始输入
    val createdAt: Long,        // 创建时间
    val attachmentPath: String? = null  // 附加凭证图片（App 私有路径），空=无附件
)
