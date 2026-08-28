package com.accounting.app.parser.time

import com.accounting.app.ui.model.TimeRangeType
import com.accounting.app.log.AppLogger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

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

    fun getWeekStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun describeTimeRange(type: TimeRangeType, customStart: Long?, customEnd: Long?): String {
        return when (type) {
            TimeRangeType.TODAY -> "今日"
            TimeRangeType.WEEK -> "本周"
            TimeRangeType.MONTH -> "本月"
            TimeRangeType.CUSTOM -> {
                if (customStart == null || customEnd == null) return "自定义"
                val startCal = Calendar.getInstance().apply { timeInMillis = customStart }
                val endCal = Calendar.getInstance().apply { timeInMillis = customEnd }
                val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
                if (sameYear) {
                    val fmt = SimpleDateFormat("MM月dd日", Locale.CHINA)
                    "${fmt.format(startCal.time)} - ${fmt.format(endCal.time)}"
                } else {
                    val fmt = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
                    "${fmt.format(startCal.time)} - ${fmt.format(endCal.time)}"
                }
            }
        }
    }

    fun getDaysBetween(start: Long, end: Long): Int {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var days = 1
        while (startCal.timeInMillis < endCal.timeInMillis) {
            startCal.add(Calendar.DAY_OF_YEAR, 1)
            days++
        }
        return days
    }

    fun parseTime(timeStr: String): Long = try { formatter.parse(timeStr)?.time ?: now() } catch (_: Exception) { now() }
    fun now(): Long = System.currentTimeMillis()

    fun simpleParseTime(rawInput: String): Long? {
        val cal = Calendar.getInstance(); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val timeRe = Regex("""(\d{1,2})[:：点](\d{1,2})?\s*(分)?""")
        val timeMatch = timeRe.find(rawInput)
        if (rawInput.contains("前天")) { cal.add(Calendar.DAY_OF_YEAR, -2); return applyDateAndTime(cal, timeMatch, rawInput) }
        if (rawInput.contains("昨天")) { cal.add(Calendar.DAY_OF_YEAR, -1); return applyDateAndTime(cal, timeMatch, rawInput) }
        if (rawInput.contains("今天")) return applyDateAndTime(cal, timeMatch, rawInput)
        val lastWeekMatch = Regex("""上周([一二三四五六日天])""").find(rawInput)
        if (lastWeekMatch != null) { applyWeekdayOffset(cal, lastWeekMatch.groupValues[1], isLastWeek = true); return applyDateAndTime(cal, timeMatch, rawInput) }
        val weekdayMatch = Regex("""(?<!下)周([一二三四五六日天])""").find(rawInput)
        if (weekdayMatch != null) { applyWeekdayOffset(cal, weekdayMatch.groupValues[1], isLastWeek = false); return applyDateAndTime(cal, timeMatch, rawInput) }
        val lastMonthMatch = Regex("""上个月(\d{1,2})号""").find(rawInput)
        if (lastMonthMatch != null) {
            val day = lastMonthMatch.groupValues[1].toInt()
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            return applyDateAndTime(cal, timeMatch, rawInput)
        }
        val dayRe = Regex("""(\d{1,2})号""")
        val dayMatch = dayRe.find(rawInput)
        if (dayMatch != null) {
            val day = dayMatch.groupValues[1].toInt()
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            if (day >= currentDay) cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            return applyDateAndTime(cal, timeMatch, rawInput)
        }
        if (rawInput.contains("今早") || rawInput.contains("早上") || rawInput.contains("昨晚") || rawInput.contains("今晚") || rawInput.contains("晚上") || rawInput.contains("中午") || rawInput.contains("下午") || rawInput.contains("半夜") || rawInput.contains("凌晨")) {
            return applyDateAndTime(cal, timeMatch, rawInput)
        }
        if (timeMatch != null) return applyTime(cal, timeMatch, rawInput)
        return null
    }

    fun parseOrDefault(input: String?, requestId: String, billIndex: Int? = null): Long {
        if (!input.isNullOrBlank()) {
            val localParsed = simpleParseTime(input)
            if (localParsed != null) {
                if (billIndex != null) {
                    AppLogger.d(requestId, "时间解析", "输入提示：$input，解析结果：${formatTime(localParsed)}", billIndex)
                } else {
                    AppLogger.d(requestId, "时间解析", "输入提示：$input，解析结果：${formatTime(localParsed)}")
                }
                return localParsed
            }
            val hintParsed = parseTimeHint(input)
            if (hintParsed != null) {
                if (billIndex != null) {
                    AppLogger.d(requestId, "时间解析", "输入提示：$input，解析结果：${formatTime(hintParsed)}", billIndex)
                } else {
                    AppLogger.d(requestId, "时间解析", "输入提示：$input，解析结果：${formatTime(hintParsed)}")
                }
                return hintParsed
            }
        }
        val fallback = nowZeroSeconds()
        if (billIndex != null) {
            AppLogger.w(requestId, "时间解析", "输入提示：$input，解析失败，兜底当前时间：${formatTime(fallback)}", billIndex)
        } else {
            AppLogger.w(requestId, "时间解析", "输入提示：$input，解析失败，兜底当前时间：${formatTime(fallback)}")
        }
        return fallback
    }

    fun parseTimeHint(timeHint: String?): Long? {
        if (timeHint.isNullOrBlank()) return null
        val forbiddenPatterns = listOf(
            Regex("""\d{4}-\d{2}-\d{2}"""),
            Regex("""\d{13}"""),
            Regex("""\d{4}/\d{2}/\d{2}"""),
            Regex("""\d{2}/\d{2}/\d{4}"""),
            Regex("""\d{4}年\d{2}月\d{2}日""")
        )
        if (forbiddenPatterns.any { it.containsMatchIn(timeHint) }) return null
        return simpleParseTime(timeHint)
    }

    fun matchTimeCategory(rawInput: String): Pair<String, String>? {
        for ((key, value) in timeCategoryMap) {
            if (rawInput.contains(key)) return value
        }
        return null
    }

    data class TimeRange(val start: Long, val end: Long)

    fun extractTimeRange(input: String): TimeRange {
        val endCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = Calendar.getInstance()
        when {
            input.contains("本周") || input.contains("这周") || input.contains("这个星期") -> {
                startCal.firstDayOfWeek = Calendar.MONDAY
                startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            input.contains("上周") || input.contains("上星期") -> {
                startCal.firstDayOfWeek = Calendar.MONDAY
                startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                startCal.add(Calendar.WEEK_OF_YEAR, -1)
            }
            input.contains("昨天") -> {
                startCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            input.contains("今天") || input.contains("今日") -> {
            }
            input.contains("上个月") || input.contains("上月") -> {
                startCal.add(Calendar.MONTH, -1)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
            }
            input.contains("今年") || input.contains("本年") -> {
                startCal.set(Calendar.MONTH, Calendar.JANUARY)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
            }
            input.contains("去年") -> {
                startCal.add(Calendar.YEAR, -1)
                startCal.set(Calendar.MONTH, Calendar.JANUARY)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
            }
            input.contains("本月") || input.contains("这个月") -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
            }
            else -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        if (input.contains("昨天")) {
            val yesterdayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return TimeRange(startCal.timeInMillis, yesterdayEnd.timeInMillis)
        }

        return TimeRange(startCal.timeInMillis, endCal.timeInMillis)
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

    private fun applyDateAndTime(cal: Calendar, timeMatch: MatchResult?, rawInput: String): Long {
        if (timeMatch != null) return applyTime(cal, timeMatch, rawInput)
        when {
            rawInput.contains("今早") || rawInput.contains("早上") -> { cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0) }
            rawInput.contains("昨晚") || rawInput.contains("今晚") || rawInput.contains("晚上") -> { cal.set(Calendar.HOUR_OF_DAY, 20); cal.set(Calendar.MINUTE, 0) }
            rawInput.contains("中午") -> { cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0) }
            rawInput.contains("下午") -> { cal.set(Calendar.HOUR_OF_DAY, 14); cal.set(Calendar.MINUTE, 0) }
            rawInput.contains("半夜") -> { cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0) }
            rawInput.contains("凌晨") -> { cal.set(Calendar.HOUR_OF_DAY, 4); cal.set(Calendar.MINUTE, 0) }
        }
        return cal.timeInMillis
    }

    private fun applyWeekdayOffset(cal: Calendar, weekday: String, isLastWeek: Boolean) {
        val targetDow = when (weekday) {
            "一" -> Calendar.MONDAY
            "二" -> Calendar.TUESDAY
            "三" -> Calendar.WEDNESDAY
            "四" -> Calendar.THURSDAY
            "五" -> Calendar.FRIDAY
            "六" -> Calendar.SATURDAY
            "日", "天" -> Calendar.SUNDAY
            else -> return
        }
        val todayDow = cal.get(Calendar.DAY_OF_WEEK)
        val diff = targetDow - todayDow
        val daysBack = if (isLastWeek) {
            diff - 7
        } else {
            when {
                diff > 0 -> diff - 7
                diff == 0 -> -7
                else -> diff
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, daysBack)
    }

    private fun nowZeroSeconds(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}