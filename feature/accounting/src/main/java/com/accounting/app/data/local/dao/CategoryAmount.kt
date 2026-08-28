package com.accounting.app.data.local.dao

/**
 * 分类统计查询结果载体。
 *
 * 用于 getCategoryStats 查询：按一级分类聚合后的金额合计。
 * 字段名需与 SQL 中的别名 totalAmount 一致，以便 Room 自动映射。
 */
data class CategoryAmount(
    val category: String,
    val totalAmount: Long
)
