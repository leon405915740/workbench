package com.accounting.app.util

import org.junit.Test

class AmountUtilsTest {

    @Test
    fun `extractAmounts should extract single amount`() {
        val segments = AmountUtils.extractAmounts("午饭30", "test-request")
        assert(segments.size == 1)
        assert(segments[0].amountFen == 3000L)
        assert(segments[0].textBefore == "午饭")
    }

    @Test
    fun `extractAmounts should extract decimal amount`() {
        val segments = AmountUtils.extractAmounts("咖啡35.5", "test-request")
        assert(segments.size == 1)
        assert(segments[0].amountFen == 3550L)
    }

    @Test
    fun `extractAmounts should extract multiple amounts`() {
        val segments = AmountUtils.extractAmounts("午饭20，晚饭40", "test-request")
        assert(segments.size == 2)
        assert(segments[0].amountFen == 2000L)
        assert(segments[1].amountFen == 4000L)
    }

    @Test
    fun `extractAmounts should handle Chinese numbers`() {
        val segments = AmountUtils.extractAmounts("花了一百", "test-request")
        assert(segments.size == 1)
        assert(segments[0].amountFen == 10000L)
    }

    @Test
    fun `extractAmounts should handle mixed formats`() {
        val segments = AmountUtils.extractAmounts("打车十五元，吃饭25", "test-request")
        assert(segments.size == 2)
        assert(segments[0].amountFen == 1500L)
        assert(segments[1].amountFen == 2500L)
    }

    @Test
    fun `extractAmounts should handle colloquial kuai fractions`() {
        assert(AmountUtils.extractAmount("奶茶25块5") == 2550L)
        assert(AmountUtils.extractAmount("奶茶25块50") == 2550L)
        assert(AmountUtils.extractAmount("奶茶25块钱5") == 2550L)
        assert(AmountUtils.extractAmount("奶茶25块500") == null)
        assert(AmountUtils.extractAmount("咖啡三块五") == null)
        assert(AmountUtils.extractAmount("午餐一百块二") == null)
        assert(AmountUtils.extractAmount("咖啡3块五") == null)
        assert(AmountUtils.extractAmount("咖啡三块5") == null)
    }

    @Test
    fun `extractAmounts should reject overflowing amounts`() {
        assert(AmountUtils.extractAmount("午饭999999999999999999999999元") == null)
        assert(AmountUtils.extractAmount("午饭九千九百九十九亿九千九百九十九亿") == null)
        assert(AmountUtils.extractAmount("十亿亿元") == null)
    }

    @Test
    fun `loose extraction should support custom merchant mappings without weakening normal parsing`() {
        assert(AmountUtils.extractAmount("Apple Store 5000") == null)
        assert(AmountUtils.extractLooseAmount("Apple Store 5000") == 500000L)
    }

    @Test
    fun `Chinese integer parsing should handle zero and colloquial trailing digits`() {
        assert(AmountUtils.extractAmount("二〇二六") == 202600L)
        assert(AmountUtils.extractAmount("一百零二元") == 10200L)
        assert(AmountUtils.extractAmount("一百二元") == 12000L)
        assert(AmountUtils.extractAmount("一万二元") == 1200000L)
    }

    @Test
    fun `extractAmounts should return empty for no amounts`() {
        val segments = AmountUtils.extractAmounts("你好", "test-request")
        assert(segments.isEmpty())
    }

    @Test
    fun `yuanToFen should convert correctly`() {
        assert(AmountUtils.yuanToFen("30") == 3000L)
        assert(AmountUtils.yuanToFen("35.5") == 3550L)
        assert(AmountUtils.yuanToFen("100") == 10000L)
        assert(AmountUtils.yuanToFen("0.29") == 29L)
        assert(AmountUtils.yuanToFen("12.345") == 1235L)
    }

    @Test
    fun `extractAmounts should keep direction word 收入 in textBefore`() {
        val segments = AmountUtils.extractAmounts("意外收入400", "test-request")
        assert(segments.size == 1)
        assert(segments[0].amountFen == 40000L)
        assert(segments[0].textBefore == "意外收入")
    }

    @Test
    fun `cleanSegment should remove noise`() {
        assert(AmountUtils.cleanSegment("午饭花了") == "午饭")
        assert(AmountUtils.cleanSegment("买衣服花了") == "买衣服")
        assert(AmountUtils.cleanSegment("昨天打车") == "打车")
        assert(AmountUtils.cleanSegment("花了") == "花了")
        assert(AmountUtils.cleanSegment("今天花了") == "花了")
    }
}
