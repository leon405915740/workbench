package com.accounting.app.ui.components

private val expenseEmojiMap = mapOf(
    "餐饮美食" to "🍜",
    "交通出行" to "🚗",
    "日用家居" to "🏠",
    "娱乐休闲" to "🎮",
    "服饰美容" to "👕",
    "住房房租" to "🏘️",
    "通讯资费" to "📱",
    "医疗健康" to "💊",
    "教育学习" to "📚",
    "人情往来" to "🧧",
    "数码电器" to "💻",
    "爱车养车" to "🚙",
    "宠物生活" to "🐕",
    "旅行度假" to "✈️",
    "育儿长辈" to "👶",
    "其他支出" to "📦"
)

private val incomeEmojiMap = mapOf(
    "工资薪水" to "💰",
    "兼职副业" to "💼",
    "理财收益" to "📈",
    "人情礼金" to "🧧",
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