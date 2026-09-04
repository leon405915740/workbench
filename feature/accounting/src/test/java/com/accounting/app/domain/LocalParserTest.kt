package com.accounting.app.domain

import com.accounting.app.ai.model.AiItem
import com.accounting.app.ai.model.AiOutput
import com.accounting.app.plan.parser.LocalParser
import org.junit.Test

class LocalParserTest {

    @Test
    fun `parse should parse numeric amount`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "午餐", amount = "30", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.size == 1)
        assert(result[0].amountFen == 3000L)
        assert(result[0].description == "午餐")
    }

    @Test
    fun `parse should parse decimal amount`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "咖啡", amount = "35.5", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result[0].amountFen == 3550L)
    }

    @Test
    fun `parse should parse Chinese integer amount`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "午餐", amount = "三十", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result[0].amountFen == 3000L)
    }

    @Test
    fun `parse should reject unsupported Chinese fractional shorthand instead of misreading it`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "咖啡", amount = "三块五", time_hint = "今天"),
            AiItem(description = "午餐", amount = "一百块二", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.isEmpty())
    }

    @Test
    fun `parse should reject overflow and fail the whole mixed batch`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "午餐", amount = "30", time_hint = "今天"),
            AiItem(description = "异常金额", amount = "一百亿亿", time_hint = "今天")
        ))
        assert(LocalParser.parse(aiOutput, "test-request").isEmpty())
    }

    @Test
    fun `parse should filter missing amount`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "未知", amount = null, time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.isEmpty())
    }

    @Test
    fun `parse should handle multiple items`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "午餐", amount = "30", time_hint = "今天"),
            AiItem(description = "打车", amount = "25", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.size == 2)
        assert(result.sumOf { it.amountFen } == 5500L)
    }

    @Test
    fun `parse should filter invalid items`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = null, amount = "invalid", time_hint = "invalid")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.isEmpty())
    }
}
