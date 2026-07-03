package com.accounting.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    /** 时间规则映射：时间关键词 → (一级分类, 二级分类)，仅用于餐饮类场景 */
    private val timeCategoryMap = mapOf(
        "早饭" to ("餐饮" to "早餐"), "早餐" to ("餐饮" to "早餐"), "早点" to ("餐饮" to "早餐"), "早上" to ("餐饮" to "早餐"),
        "午饭" to ("餐饮" to "午餐"), "午餐" to ("餐饮" to "午餐"), "中餐" to ("餐饮" to "午餐"),
        "中午吃饭" to ("餐饮" to "午餐"), "中午" to ("餐饮" to "午餐"),
        "下午茶" to ("餐饮" to "饮品"), "下午" to ("餐饮" to "饮品"),
        "晚饭" to ("餐饮" to "晚餐"), "晚餐" to ("餐饮" to "晚餐"),
        "晚上" to ("餐饮" to "晚餐"), "夜宵" to ("餐饮" to "晚餐"), "宵夜" to ("餐饮" to "晚餐"),
    )

    fun formatTime(timestamp: Long): String = formatter.format(Date(timestamp))

    fun formatTimeRelative(timestamp: Long): String {
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.CHINA)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR))
            return timeFormatter.format(target.time)
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR))
            return "昨天 ${timeFormatter.format(target.time)}"
        val dayDiff = (now.timeInMillis - timestamp) / 86_400_000L
        if (dayDiff in 0..6 && now.get(Calendar.WEEK_OF_YEAR) == target.get(Calendar.WEEK_OF_YEAR)) {
            val dayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            return "${dayNames[target.get(Calendar.DAY_OF_WEEK) - 1]} ${timeFormatter.format(target.time)}"
        }
        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
            val monthDayFmt = SimpleDateFormat("MM月dd日", Locale.CHINA)
            return "${monthDayFmt.format(target.time)} ${timeFormatter.format(target.time)}"
        }
        return formatter.format(Date(timestamp))
    }

    fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun parseTime(timeStr: String): Long = try { formatter.parse(timeStr)?.time ?: now() } catch (_: Exception) { now() }
    fun now(): Long = System.currentTimeMillis()

    fun simpleParseTime(rawInput: String): Long? {
        val cal = Calendar.getInstance(); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val timeRe = Regex("""(\d{1,2})[:：点](\d{1,2})?\s*(分)?""")
        val timeMatch = timeRe.find(rawInput)
        if (rawInput.contains("前天")) { cal.add(Calendar.DAY_OF_YEAR, -2); return applyTime(cal, timeMatch, rawInput) }
        if (rawInput.contains("昨天")) { cal.add(Calendar.DAY_OF_YEAR, -1); return applyTime(cal, timeMatch, rawInput) }
        if (rawInput.contains("今天")) return applyTime(cal, timeMatch, rawInput)
        val dayRe = Regex("""\b(\d{1,2})号\b"""); val dayMatch = dayRe.find(rawInput)
        if (dayMatch != null) { cal.set(Calendar.DAY_OF_MONTH, dayMatch.groupValues[1].toInt()); return applyTime(cal, timeMatch, rawInput) }
        if (rawInput.contains("今早") || rawInput.contains("早上")) { cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0); return cal.timeInMillis }
        if (rawInput.contains("中午")) { cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0); return cal.timeInMillis }
        if (rawInput.contains("下午")) { val hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 14; cal.set(Calendar.HOUR_OF_DAY, hour + 12); cal.set(Calendar.MINUTE, 0); return cal.timeInMillis }
        if (rawInput.contains("今晚") || rawInput.contains("晚上")) { val hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 20; cal.set(Calendar.HOUR_OF_DAY, hour.coerceAtMost(23)); cal.set(Calendar.MINUTE, 0); return cal.timeInMillis }
        if (timeMatch != null) return applyTime(cal, timeMatch, rawInput)
        return null
    }

    /**
     * 匹配时间规则映射分类。
     * 仅当文本包含明确时间关键词（早餐/午饭/晚饭等）且语义为吃饭/用餐相关时返回分类。
     * @return Pair(一级分类, 二级分类)，匹配失败返回 null
     */
    fun matchTimeCategory(rawInput: String): Pair<String, String>? {
        for ((key, value) in timeCategoryMap) {
            if (rawInput.contains(key)) return value
        }
        return null
    }

    private fun applyTime(cal: Calendar, timeMatch: MatchResult?, rawInput: String): Long {
        if (timeMatch != null) {
            val hour = timeMatch.groupValues[1].toInt()
            val minute = timeMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: 0
            val isPM = rawInput.contains("下午") || rawInput.contains("晚上") || rawInput.contains("今晚")
            cal.set(Calendar.HOUR_OF_DAY, if (isPM && hour < 12) hour + 12 else hour)
            cal.set(Calendar.MINUTE, minute)
        }
        return cal.timeInMillis
    }
}
