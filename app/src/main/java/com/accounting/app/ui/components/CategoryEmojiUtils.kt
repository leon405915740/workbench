package com.accounting.app.ui.components

private val expenseEmojiMap = mapOf(
    "餐饮" to "🍜",
    "交通" to "🚗",
    "购物" to "🛍️",
    "居家" to "🏠",
    "娱乐" to "🎮",
    "通讯" to "📱",
    "医疗" to "💊",
    "教育" to "📚",
    "其他" to "📦"
)

private val incomeEmojiMap = mapOf(
    "工资" to "💰",
    "奖金" to "🎁",
    "红包" to "🧧",
    "报销" to "🧾",
    "退款" to "↩️",
    "投资收益" to "📈",
    "兼职收入" to "💼",
    "其他收入" to "📦"
)

fun getCategoryEmoji(name: String, type: String): String {
    val map = if (type == "income") incomeEmojiMap else expenseEmojiMap
    return map[name] ?: "📦"
}

fun getSubcategoryEmoji(name: String, type: String, parentName: String?): String {
    val map = if (type == "income") incomeEmojiMap else expenseEmojiMap
    return map[name] ?: parentName?.let { map[it] } ?: "📦"
}