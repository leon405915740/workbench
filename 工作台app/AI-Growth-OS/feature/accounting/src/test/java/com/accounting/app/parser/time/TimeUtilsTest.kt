package com.accounting.app.parser.time

import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun `extractTimeRange should parse this week`() {
        val (start, end) = TimeUtils.extractTimeRange("本周消费多少")
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expectedStart = cal.timeInMillis
        assert(start == expectedStart)
    }

    @Test
    fun `extractTimeRange should parse last week`() {
        val (start, end) = TimeUtils.extractTimeRange("上周花了多少")
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expectedStart = cal.timeInMillis
        assert(start == expectedStart)
    }

    @Test
    fun `extractTimeRange should parse today`() {
        val (start, end) = TimeUtils.extractTimeRange("今天消费")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expectedStart = cal.timeInMillis
        assert(start == expectedStart)
    }

    @Test
    fun `extractTimeRange should parse yesterday`() {
        val (start, end) = TimeUtils.extractTimeRange("昨天花了多少")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expectedStart = cal.timeInMillis
        assert(start == expectedStart)
    }

    @Test
    fun `extractTimeRange should return month default when no time keyword`() {
        val (start, end) = TimeUtils.extractTimeRange("这个月花了多少")
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expectedStart = cal.timeInMillis
        assert(start == expectedStart)
    }

    @Test
    fun `extractTimeRange should parse last month`() {
        val (start, end) = TimeUtils.extractTimeRange("上个月支出")
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expectedStart = cal.timeInMillis
        assert(start == expectedStart)
    }
}